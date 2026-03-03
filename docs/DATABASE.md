# Database Schema Design (Draft)

GPS 기반 자동 출퇴근 및 위치 공유 시스템을 위한 데이터베이스 설계 초안입니다.

## 1. ERD 개요 (Logical)

- **User** (1) <--- (N) **WorkSession**: 사용자는 여러 번의 출퇴근 기록을 가짐
- **Team** (1) <--- (N) **User**: 사용자는 하나의 팀에 소속됨
- **Team** (1) <--- (1) **WorkPolicy**: 팀별로 하나의 근무 정책을 가짐 (또는 사용자별 설정 가능)
- **User** (1) <--- (N) **LocationSharingPermission**: 위치 공유 권한 관계

---

## 2. 테이블 상세 설계

### 2.1. `users` (사용자)
사용자의 기본 정보와 현재 소속된 팀, 권한을 관리합니다.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | 고유 식별자 |
| `login_id` | VARCHAR(50) | 로그인 ID |
| `email` | VARCHAR(100) | 이메일 |
| `password_hash` | VARCHAR(255) | 암호화된 비밀번호 |
| `name` | VARCHAR(50) | 이름 |
| `role_level` | VARCHAR(20) | 권한 레벨 (USER, LEADER, ADMIN) |
| `team_id` | BIGINT (FK) | 소속 팀 ID |
| `policy_id` | BIGINT (FK) | 적용된 근무 정책 ID |
| `hr_authority` | BOOLEAN | 인사 관리 권한 여부 (리더와 별개 가능) |
| `created_at` | DATETIME | 생성 일시 |

### 2.2. `work_policies` (근무 정책)
근무지의 위치와 자동 출퇴근을 위한 설정값을 담습니다.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | 고유 식별자 |
| `name` | VARCHAR(100) | 정책 이름 (예: 강남 본사 근무) |
| `latitude` | DOUBLE | 근무지 중심 위도 |
| `longitude` | DOUBLE | 근무지 중심 경도 |
| `checkin_radius_m` | INT | 자동 출근 인정 반경 (미터) |
| `checkout_radius_m` | INT | 자동 퇴근 인정 반경 (미터) |
| `checkout_grace_minutes` | INT | **퇴근 유예 시간 (분)** - 반경 이탈 후 이 시간 경과 시 퇴근 처리 |
| `team_id` | BIGINT (FK) | 이 정책을 사용하는 팀 |

### 2.3. `work_sessions` (출퇴근 기록)
사용자의 실제 출퇴근 세션을 관리합니다.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | 고유 식별자 |
| `user_id` | BIGINT (FK) | 사용자 ID |
| `status` | VARCHAR(20) | 현재 상태 (CHECKED_IN, CHECKED_OUT) |
| `check_in_at` | DATETIME | 출근 처리 시각 |
| `check_out_at` | DATETIME | 퇴근 처리 시각 |
| `outside_since` | DATETIME | **반경 밖으로 나간 시각** (자동 퇴근 계산용) |
| `last_latitude` | DOUBLE | 마지막으로 확인된 위도 |
| `last_longitude` | DOUBLE | 마지막으로 확인된 경도 |

### 2.4. `user_locations` (최신 위치 캐시)
리더나 동료가 지도에서 실시간으로 위치를 확인할 때 성능을 위해 사용하는 테이블입니다.

| Column | Type | Description |
| :--- | :--- | :--- |
| `user_id` | BIGINT (PK, FK) | 사용자 ID |
| `latitude` | DOUBLE | 현재 위도 |
| `longitude` | DOUBLE | 현재 경도 |
| `updated_at` | DATETIME | 마지막 업데이트 시각 |

### 2.5. `location_sharing_permissions` (위치 공유 권한)
누가 누구의 위치를 볼 수 있는지 정의합니다.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | 고유 식별자 |
| `granter_id` | BIGINT (FK) | 위치 정보를 제공하는 사람 (주체) |
| `grantee_id` | BIGINT (FK) | 위치 정보를 확인하는 사람 (객체) |
| `permission_type` | VARCHAR(20) | 유형 (LEADER: 리더 권한, PEER: 상호 동의) |
| `is_active` | BOOLEAN | 현재 공유 활성화 여부 |

---

## 3. 주요 로직 처리 가이드

### 3.1. 자동 퇴근 (Grace Period) 로직
1. 앱에서 주기적으로 위치를 서버로 전송 (`/api/v1/attendance/me/location`)
2. 서버는 전송된 위치가 `WorkPolicy`의 `checkout_radius_m` 밖인지 확인.
3. **밖이라면**: `work_sessions.outside_since`가 `null`이면 현재 시간으로 기록.
4. **안이라면**: `work_sessions.outside_since`를 다시 `null`로 초기화 (잠깐 나갔다 들어온 경우).
5. **백엔드 스케줄러**: 1분마다 `outside_since`가 기록된 세션 중 `now() - outside_since > checkout_grace_minutes` 인 세션을 찾아 자동 퇴근 처리.

### 3.2. 위치 공유 로직
1. 리더가 팀 지도를 열 때: `user_locations`와 `location_sharing_permissions`를 조인하여 권한이 있는 팀원의 최신 위치만 조회.
2. 동료끼리 볼 때: 상호 `PEER` 권한이 `is_active=true`인 경우만 조회 가능.
