# SDKSample

## sample
- app: 기본 샘플앱
- view: 샘플 앱에서 사용하는 뷰
## sdk
- libgaze-release: TrueGaze에서 사용하는 GazeTracker(Gaze) 모듈
- gazetracker-release: GazeTracker를 관리하고 가져온 시선 정보를 가공해 전달하는 모듈

### 난독화하려면 proguard-rules.pro에 추가해야함
```
-keep interface camp.visual.libgaze.callbacks.jni.LibGazeJNICallback {
  *;
}
```
