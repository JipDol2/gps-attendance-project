# Background Location & Geofencing Strategy

앱이 백그라운드에 있거나 완전히 종료된 상태에서도 자동 출퇴근을 구현하기 위한 상세 기술 전략입니다.

## 1. 핵심 메커니즘: Geofencing (지오펜싱)
앱이 직접 GPS를 계속 확인하는 대신, OS(Android/iOS)에 특정 지역 감지를 위임합니다.

- **장점**: 배터리 소모 최소화, 앱 종료 상태에서도 작동 가능.
- **작동 방식**: OS가 해당 반경 진입/이탈 감지 시 앱을 깨워(Wake-up) 특정 로직(API 호출)을 실행하게 함.

---

## 2. 플랫폼별 구현 전략

### 2.1. Android (Kotlin)
- **API**: `GeofencingClient` (Google Play Services) 사용.
- **필수 권한**: 
  - `ACCESS_FINE_LOCATION`
  - `ACCESS_BACKGROUND_LOCATION` (Android 10 이상 필수)
- **핵심 컴포넌트**: 
  - `BroadcastReceiver`: 지오펜싱 이벤트(진입/이탈) 수신.
  - `Foreground Service`: 위치 업데이트가 빈번할 경우 사용자가 인지할 수 있도록 알림 표시.

### 2.2. iOS (Swift)
- **API**: `CoreLocation`의 `CLCircularRegion` 및 `startMonitoring(for:)` 사용.
- **필수 권한**: 
  - `Location Always and When In Use Usage Description` (Info.plist)
  - 'Always Allow' 설정 유도 필수.
- **핵심 컴포넌트**: 
  - `CLLocationManagerDelegate`: `didEnterRegion`, `didExitRegion` 메서드 구현.

---

## 3. 해결해야 할 과제 (Challenges)

### 3.1. 권한 획득 (Permissions)
- 최근 OS는 '항상 허용' 권한을 얻는 것을 매우 엄격하게 제한함.
- **전략**: 처음부터 '항상 허용'을 묻지 말고, '앱 사용 중에만'을 먼저 받은 뒤, 기능 설명(온보딩)을 통해 왜 '항상 허용'이 필요한지 설득하는 UX가 필요함.

### 3.2. 배터리 최적화 정책
- Android의 경우 '도즈 모드(Doze Mode)'나 제조사별 배터리 관리 정책으로 인해 지오펜싱이 지연될 수 있음.
- **전략**: 설정 화면에서 '배터리 최적화 제외' 설정을 유도하는 가이드 제공.

### 3.3. 네트워크 불안정 대응
- 지하철이나 건물 깊숙한 곳에서 반경을 벗어날 경우 API 호출이 실패할 수 있음.
- **전략**: 로컬 DB에 이벤트를 캐싱하고 네트워크 복구 시 재전송(Retry) 로직 구현.
