# GPS Attendance API Draft (Mobile v1)

## Scope
- Date: 2026-03-28
- Client: Android app (`android-app`)
- Goal: Support mobile menus
  - `대시보드`
  - `출퇴근 기록`
  - `위치 공유`
  - `팀 관리`
  - `근무 정책`
  - `권한 관리`

## Auth
- Existing
  - `POST /api/v1/users/register`
  - `POST /api/v1/users/login`
  - `POST /api/v1/users/refresh`
- Header
  - `Authorization: Bearer <accessToken>`

## New APIs (Draft)

### 1) Dashboard Summary
- `GET /api/v1/dashboard/summary`
- Purpose: Main dashboard counters and my status
- Response `200`
```json
{
  "teamId": 1,
  "teamName": "프론트엔드팀",
  "workDate": "2026-03-28",
  "totalMembers": 12,
  "checkedInMembers": 8,
  "notCheckedInMembers": 4,
  "myStatus": "CHECKED_IN"
}
```

### 2) My Attendance History
- `GET /api/v1/attendance/me/history?month=2026-03&page=0&size=20`
- Purpose: 출퇴근 기록 화면 리스트
- Response `200`
```json
{
  "content": [
    {
      "sessionId": 101,
      "workDate": "2026-03-28",
      "status": "CHECKED_IN",
      "checkInAt": "2026-03-28T09:02:00",
      "checkOutAt": null,
      "late": false
    }
  ]
}
```

### 3) Team Attendance (Today)
- `GET /api/v1/teams/{teamId}/attendance/today`
- Purpose: 팀 관리 화면 팀원 상태
- Response `200`
```json
{
  "teamId": 1,
  "teamName": "프론트엔드팀",
  "workDate": "2026-03-28",
  "members": [
    {
      "userId": 7,
      "userName": "김관리",
      "position": "팀장",
      "status": "CHECKED_IN",
      "checkInAt": "2026-03-28T09:01:10",
      "checkOutAt": null,
      "locationKnown": true
    }
  ]
}
```

### 4) Work Policies
- `GET /api/v1/work-policies`
- Purpose: 근무 정책 목록 조회
- Response `200`
```json
[
  {
    "policyId": 11,
    "teamId": 1,
    "teamName": "프론트엔드팀",
    "workAddress": "서울시 강남구 테헤란로 123",
    "allowedRadiusM": 200,
    "graceMinutes": 5,
    "coreTimeStart": "10:00",
    "coreTimeEnd": "16:00",
    "enabled": true
  }
]
```

- `PUT /api/v1/work-policies/{policyId}`
- Request
```json
{
  "teamId": 1,
  "workAddress": "서울시 강남구 테헤란로 123",
  "allowedRadiusM": 200,
  "graceMinutes": 5,
  "coreTimeStart": "10:00",
  "coreTimeEnd": "16:00",
  "enabled": true
}
```

### 5) Permissions
- `GET /api/v1/permissions`
- Purpose: 권한 목록
- Response `200`
```json
[
  {
    "permissionId": 1,
    "name": "시스템 관리자",
    "description": "모든 권한",
    "memberCount": 1,
    "createdAt": "2024-01-01"
  }
]
```

- `GET /api/v1/permissions/{permissionId}/members`
- Response `200`
```json
[
  {
    "userId": 7,
    "userName": "김관리",
    "teamName": "프론트엔드팀"
  }
]
```

- `POST /api/v1/permissions`
- `PUT /api/v1/permissions/{permissionId}`
- `DELETE /api/v1/permissions/{permissionId}`

## Existing APIs kept as-is
- `POST /api/v1/attendance/me/location`
- `GET /api/v1/attendance/me/sessions`
- `GET /api/v1/attendance/visible-sessions`
- `GET /api/v1/teams`

## Error Contract (Recommended)
- Error body (all endpoints)
```json
{
  "code": "VALIDATION_ERROR",
  "message": "allowedRadiusM must be >= 50",
  "details": []
}
```
- Status
  - `400` validation
  - `401` token invalid/expired
  - `403` no authority
  - `404` resource not found
  - `500` internal error
