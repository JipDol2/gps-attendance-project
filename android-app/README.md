# Android App (Kotlin + Compose)

GPS Attendance 안드로이드 앱입니다.

## 기술 스택
- Kotlin
- Jetpack Compose
- Hilt
- Retrofit + OkHttp
- DataStore
- Google Location Services
- Kakao Maps SDK

## 사전 준비
- Android Studio
- Android SDK (minSdk 26)
- 실행 중인 백엔드 (`http://localhost:8080`)

## 실행 방법
1. Android Studio에서 `android-app` 폴더를 엽니다.
2. Gradle Sync를 완료합니다.
3. 에뮬레이터 또는 실기기를 실행합니다.
4. 앱을 빌드/실행합니다.

## API 서버 주소
- 설정 위치: `gradle.properties` (`BASE_URL`)
- 키: `BASE_URL`
- 현재 개발값: `http://192.168.0.2:8080/`
- 에뮬레이터 기본값 예시: `http://10.0.2.2:8080/`

실기기 테스트 시:
- `10.0.2.2` 대신 PC의 LAN IP로 변경해야 합니다.

## 카카오맵 설정
1. Kakao Developers에서 Native App Key 발급
2. `gradle.properties` 또는 `~/.gradle/gradle.properties`에 아래 추가
```properties
KAKAO_NATIVE_APP_KEY=YOUR_KAKAO_NATIVE_APP_KEY
```
3. 앱 실행

## 구현된 주요 API 연동
- `POST /api/v1/users/login`
- `POST /api/v1/users/refresh`
- `POST /api/v1/attendance/me/location`
- `GET /api/v1/attendance/me/sessions`
- `GET /api/v1/attendance/me/history`
- `GET /api/v1/attendance/visible-sessions`
- `GET /api/v1/dashboard/summary`
- `GET /api/v1/teams/{teamId}/attendance/today`
- `GET /api/v1/work-policies`
- `PUT /api/v1/work-policies/{policyId}`
- `GET /api/v1/permissions`
- `GET /api/v1/permissions/{permissionId}/members`
- `POST /api/v1/permissions`
- `POST /api/v1/permissions/{permissionId}/members`
- `DELETE /api/v1/permissions/{permissionId}/members/{userId}`

## 최근 UI/기능 업데이트 (2026-03-28)
- 로그인 후 기본 화면은 지도 직행이 아니라 메뉴 기반 홈 화면으로 변경
- `위치 공유` 탭에서만 카카오지도 노출
- 지도 오버레이 패널 제거, `현재 위치` FAB만 유지
- `대시보드/출퇴근 기록/팀 관리/근무 정책/권한 관리` 탭을 모바일 카드 UI로 정리
- 탭 내부 긴 콘텐츠에 대해 세로 스크롤 지원
- `권한 관리` 탭에서 권한 생성/구성원 조회/멤버 할당·해제 연동
- `근무 정책` 탭에서 정책 조회/수정 연동
