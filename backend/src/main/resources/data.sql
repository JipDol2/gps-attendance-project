INSERT INTO branches (name, latitude, longitude)
SELECT '을지로 경기빌딩', 37.5661, 126.9920
WHERE NOT EXISTS (
    SELECT 1 FROM branches WHERE name = '을지로 경기빌딩'
);

INSERT INTO branches (name, latitude, longitude)
SELECT '강남 테헤란센터', 37.4981, 127.0276
WHERE NOT EXISTS (
    SELECT 1 FROM branches WHERE name = '강남 테헤란센터'
);

INSERT INTO teams (name, parent_team_id, branch_id)
SELECT 'RodeInnovation', NULL, b.id
FROM branches b
WHERE b.name = '을지로 경기빌딩'
  AND NOT EXISTS (
      SELECT 1 FROM teams t WHERE t.name = 'RodeInnovation' AND t.parent_team_id IS NULL
  );

INSERT INTO teams (name, parent_team_id, branch_id)
SELECT 'BusinessDivision', parent.id, b.id
FROM teams parent
JOIN branches b ON b.name = '을지로 경기빌딩'
WHERE parent.name = 'RodeInnovation'
  AND parent.parent_team_id IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM teams t WHERE t.name = 'BusinessDivision' AND t.parent_team_id = parent.id
  );

INSERT INTO teams (name, parent_team_id, branch_id)
SELECT child_name, parent.id, b.id
FROM teams parent
CROSS JOIN (VALUES ('HR'), ('Sales'), ('FieldOps')) AS children(child_name)
JOIN branches b ON b.name = CASE WHEN children.child_name = 'FieldOps' THEN '강남 테헤란센터' ELSE '을지로 경기빌딩' END
WHERE parent.name = 'BusinessDivision'
  AND NOT EXISTS (
      SELECT 1 FROM teams t WHERE t.name = children.child_name AND t.parent_team_id = parent.id
  );

INSERT INTO work_policies (name, checkin_radius_m, checkout_radius_m, checkout_grace_minutes, team_id)
SELECT
    'Default Policy ' || t.id,
    500,
    700,
    10,
    t.id
FROM teams t
WHERE NOT EXISTS (
    SELECT 1 FROM work_policies wp WHERE wp.team_id = t.id
);

INSERT INTO users (login_id, password_hash, email, name, role_level, team_id, policy_id, active, hr_authority, created_at, updated_at)
SELECT
    'admin',
    '$2a$10$AxTw/7.TZ1oWYOReGjx6qONpqeCKqj7xVc4b1EhtBv3znaiyAtmF2',
    'admin@rodeinnovation.com',
    'Admin',
    'DEPARTMENT_HEAD',
    t.id,
    wp.id,
    true,
    true,
    NOW(),
    NOW()
FROM teams t
JOIN work_policies wp ON wp.team_id = t.id
WHERE t.name = 'BusinessDivision'
  AND NOT EXISTS (
      SELECT 1 FROM users WHERE login_id = 'admin'
  );
