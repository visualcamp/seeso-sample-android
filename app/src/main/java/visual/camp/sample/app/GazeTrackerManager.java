package visual.camp.sample.app;

import android.content.Context;
import android.media.Image;
import android.util.Log;
import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.CalibrationCallback;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.UserStatusCallback;
import camp.visual.gazetracker.callback.GazeTrackerCallback;
import camp.visual.gazetracker.callback.InitializationCallback;
import camp.visual.gazetracker.constant.AccuracyCriteria;
import camp.visual.gazetracker.constant.CalibrationModeType;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.constant.UserStatusOption;
import camp.visual.gazetracker.device.CameraPosition;
import camp.visual.gazetracker.gaze.GazeInfo;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;

import visual.camp.sample.app.calibration.CalibrationDataStorage;

public class GazeTrackerManager {
  private final List<InitializationCallback> initializationCallbacks = new ArrayList<>();
  private final List<GazeCallback> gazeCallbacks = new ArrayList<>();
  private final List<CalibrationCallback> calibrationCallbacks = new ArrayList<>();
  private final List<UserStatusCallback> userStatusCallbacks = new ArrayList<>();

  static private GazeTrackerManager mInstance = null;

  private final WeakReference<Context> mContext;

  GazeTracker gazeTracker = null;
  // TODO: change licence key
  String SEESO_LICENSE_KEY = "your license key";

  static public GazeTrackerManager makeNewInstance(Context context) {
    if (mInstance != null) {
      mInstance.deinitGazeTracker();
    }
    mInstance = new GazeTrackerManager(context);
    return mInstance;
  }

  static public GazeTrackerManager getInstance() {
    return mInstance;
  }

  private GazeTrackerManager(Context context) {
    this.mContext = new WeakReference<>(context);
  }

  public boolean hasGazeTracker() {
    return gazeTracker != null;
  }

  public void initGazeTracker(InitializationCallback callback, UserStatusOption option) {
    initializationCallbacks.add(callback);
    GazeTracker.initGazeTracker(mContext.get(), SEESO_LICENSE_KEY, initializationCallback, option);
  }

  public void setCameraPosition(CameraPosition cp){
    if(gazeTracker != null) {
      gazeTracker.addCameraPosition(cp);
    }
  }

  public void deinitGazeTracker() {
    if (hasGazeTracker()) {
      GazeTracker.deinitGazeTracker(gazeTracker);
      gazeTracker = null;
    }
  }

  public void addImage(Image image) {
    if(gazeTracker != null) {
      gazeTracker.addImage(image);
    }
  }

  public void setGazeTrackerCallbacks(GazeTrackerCallback... callbacks) {
    for(GazeTrackerCallback callback : callbacks) {
      if (callback instanceof GazeCallback) {
        if(!gazeCallbacks.contains(callback)) {
          gazeCallbacks.add((GazeCallback)callback);
        }

      } else if (callback instanceof CalibrationCallback) {
        if(!calibrationCallbacks.contains(callback)) {
          calibrationCallbacks.add((CalibrationCallback) callback);
        }

      } else if (callback instanceof UserStatusCallback) {
        if(!userStatusCallbacks.contains(callback)) {
          userStatusCallbacks.add((UserStatusCallback) callback);
        }
      }
    }
  }

  public void removeCallbacks(GazeTrackerCallback... callbacks) {
    for (GazeTrackerCallback callback : callbacks) {
      if (callback instanceof GazeCallback){
        gazeCallbacks.remove(callback);
      }

      if (callback instanceof CalibrationCallback) {
        calibrationCallbacks.remove(callback);
      }

      if (callback instanceof UserStatusCallback) {
        userStatusCallbacks.remove(callback);
      }
    }
  }

  public boolean startCalibration(CalibrationModeType modeType, AccuracyCriteria criteria) {
    if (hasGazeTracker()) {
      return gazeTracker.startCalibration(modeType, criteria);
    }
    return false;
  }

  public void stopCalibration() {
    if (isCalibrating()) {
      gazeTracker.stopCalibration();
    }
  }

  public void startCollectingCalibrationSamples() {
    if (isCalibrating()) {
      gazeTracker.startCollectSamples();
    }
  }

  public boolean isCalibrating() {
    if (hasGazeTracker()) {
      return gazeTracker.isCalibrating();
    }
    return false;
  }

  public CameraPosition getCameraPosition() {
    if (hasGazeTracker()) {
      return  gazeTracker.getCameraPosition();
    }
    return null;
  }

  public enum LoadCalibrationResult {
    SUCCESS,
    FAIL_DOING_CALIBRATION,
    FAIL_NO_CALIBRATION_DATA,
    FAIL_HAS_NO_TRACKER
  }
  public LoadCalibrationResult loadCalibrationData() {
    if (!hasGazeTracker()) {
      return LoadCalibrationResult.FAIL_HAS_NO_TRACKER;
    }
    double[] calibrationData = CalibrationDataStorage.loadCalibrationData(mContext.get());
    if (calibrationData != null) {
      if (!gazeTracker.setCalibrationData(calibrationData)) {
        return LoadCalibrationResult.FAIL_DOING_CALIBRATION;
      } else {
        return LoadCalibrationResult.SUCCESS;
      }
    } else {
      return LoadCalibrationResult.FAIL_NO_CALIBRATION_DATA;
    }
  }

  // GazeTracker Callbacks
  private final InitializationCallback initializationCallback = new InitializationCallback() {
    @Override
    public void onInitialized(GazeTracker gazeTracker, InitializationErrorType initializationErrorType) {
      setGazeTracker(gazeTracker);
      for (InitializationCallback initializationCallback : initializationCallbacks) {
        initializationCallback.onInitialized(gazeTracker, initializationErrorType);
      }
      initializationCallbacks.clear();
      if (gazeTracker != null) {
        gazeTracker.setGazeCallback(gazeCallback);
        gazeTracker.setCalibrationCallback(calibrationCallback);
        gazeTracker.setUserStatusCallback(userStatusCallback);
      }
    }
  };

  private final GazeCallback gazeCallback = new GazeCallback() {
    @Override
    public void onGaze(GazeInfo gazeInfo) {
      Log.d("GazeTrackerManager", "onGaze " + gazeInfo.trackingState.name());
      for (GazeCallback gazeCallback : gazeCallbacks) {
        gazeCallback.onGaze(gazeInfo);
      }
    }
  };

  private final UserStatusCallback userStatusCallback = new UserStatusCallback() {
    @Override
    public void onAttention(long timestampBegin, long timestampEnd, float attentionScore) {
      for (UserStatusCallback userStatusCallback : userStatusCallbacks) {
        userStatusCallback.onAttention(timestampBegin, timestampEnd, attentionScore);
      }
    }

    @Override
    public void onBlink(long timestamp, boolean isBlinkLeft, boolean isBlinkRight, boolean isBlink, float leftOpenness, float rightOpenness) {
      for (UserStatusCallback userStatusCallback : userStatusCallbacks) {
        userStatusCallback.onBlink(timestamp, isBlinkLeft, isBlinkRight, isBlink, leftOpenness, rightOpenness);
      }
    }

    @Override
    public void onDrowsiness(long timestamp, boolean isDrowsiness, float intensity) {
      for (UserStatusCallback userStatusCallback : userStatusCallbacks) {
        userStatusCallback.onDrowsiness(timestamp, isDrowsiness, intensity);
      }
    }
  };

  private final CalibrationCallback calibrationCallback = new CalibrationCallback() {
    @Override
    public void onCalibrationProgress(float v) {
      for (CalibrationCallback calibrationCallback : calibrationCallbacks) {
        calibrationCallback.onCalibrationProgress(v);
      }
    }

    @Override
    public void onCalibrationNextPoint(float v, float v1) {
      for (CalibrationCallback calibrationCallback : calibrationCallbacks) {
        calibrationCallback.onCalibrationNextPoint(v, v1);
      }
    }

    @Override
    public void onCalibrationFinished(double[] doubles) {
      CalibrationDataStorage.saveCalibrationData(mContext.get(), doubles);
      for (CalibrationCallback calibrationCallback : calibrationCallbacks) {
        calibrationCallback.onCalibrationFinished(doubles);
      }
    }
  };

  private void setGazeTracker(GazeTracker gazeTracker) {
    this.gazeTracker = gazeTracker;
  }

}
