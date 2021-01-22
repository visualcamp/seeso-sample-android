package visual.camp.sample.app;

import android.app.Application;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.view.TextureView;
import android.widget.TextView.SavedState;
import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.CalibrationCallback;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.GazeTrackerCallback;
import camp.visual.gazetracker.callback.ImageCallback;
import camp.visual.gazetracker.callback.InitializationCallback;
import camp.visual.gazetracker.callback.StatusCallback;
import camp.visual.gazetracker.constant.CalibrationModeType;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.constant.StatusErrorType;
import camp.visual.gazetracker.device.GazeDevice;
import camp.visual.gazetracker.gaze.GazeInfo;
import java.util.ArrayList;
import java.util.List;
import visual.camp.sample.app.calibration.CalibrationDataStorage;

public class BaseApplication extends Application {
  private List<InitializationCallback> initializationCallbacks = new ArrayList<>();
  private List<GazeCallback> gazeCallbacks = new ArrayList<>();
  private List<CalibrationCallback> calibrationCallbacks = new ArrayList<>();
  private List<StatusCallback> statusCallbacks = new ArrayList<>();
  private List<ImageCallback> imageCallbacks = new ArrayList<>();

  private TextureView cameraPreview = null;

  GazeTracker gazeTracker = null;
  // TODO: change licence key
  String SEESO_LICENSE_KEY = "your license key";

  public boolean hasGazeTracker() {
    return gazeTracker != null;
  }

  public void initGazeTracker(InitializationCallback callback) {
    GazeDevice gazeDevice = new GazeDevice();
    initializationCallbacks.add(callback);
    GazeTracker.initGazeTracker(getApplicationContext(), gazeDevice, SEESO_LICENSE_KEY, initializationCallback);
  }

  public void deinitGazeTracker() {
    if (hasGazeTracker()) {
      GazeTracker.deinitGazeTracker(gazeTracker);
      gazeTracker = null;
    }
  }

  public void setGazeTrackerCallbacks(GazeTrackerCallback... callbacks) {
    for(GazeTrackerCallback callback : callbacks) {
      if (callback instanceof GazeCallback) {
        gazeCallbacks.add((GazeCallback)callback);

      } else if (callback instanceof CalibrationCallback) {
        calibrationCallbacks.add((CalibrationCallback) callback);

      } else if (callback instanceof ImageCallback) {
        imageCallbacks.add((ImageCallback)callback);

      } else if (callback instanceof StatusCallback) {
        statusCallbacks.add((StatusCallback) callback);
      }
    }
  }

  public void removeCallbacks(GazeTrackerCallback... callbacks) {
    for (GazeTrackerCallback callback : callbacks) {
      gazeCallbacks.remove(callback);
      calibrationCallbacks.remove(callback);
      imageCallbacks.remove(callback);
      statusCallbacks.remove(callback);
    }
  }

  public boolean startGazeTracking() {
    if (hasGazeTracker()) {
      gazeTracker.startTracking();
      return true;
    }
    return false;
  }

  public boolean stopGazeTracking() {
    if (isTracking()) {
      gazeTracker.stopTracking();
      return true;
    }
    return false;
  }

  public boolean startCalibration(CalibrationModeType modeType) {
    if (hasGazeTracker()) {
      return gazeTracker.startCalibration(modeType);
    }
    return false;
  }

  public boolean stopCalibration() {
    if (isCalibrating()) {
      gazeTracker.stopCalibration();
      return true;
    }
    return false;
  }

  public boolean startCollectingCalibrationSamples() {
    if (isCalibrating()) {
      return gazeTracker.startCollectSamples();
    }
    return false;
  }

  public boolean isTracking() {
    if (hasGazeTracker()) {
      return gazeTracker.isTracking();
    }
    return false;
  }

  public boolean isCalibrating() {
    if (hasGazeTracker()) {
      return gazeTracker.isCalibrating();
    }
    return false;
  }

  public enum LoadCalibrationResult {
    SUCCESS,
    FAIL_DOING_CALIBRATION,
    FAIL_NO_CALIBRATION_DATA,
    FAIL_HAS_NO_TRACKER
  }
  public LoadCalibrationResult loadCalibrationData() {
    if (hasGazeTracker()) {
      return LoadCalibrationResult.FAIL_HAS_NO_TRACKER;
    }

    double[] calibrationData = CalibrationDataStorage.loadCalibrationData(getApplicationContext());
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

  public void setCameraPreview(TextureView preview) {
    this.cameraPreview = preview;
    if (hasGazeTracker()) {
      gazeTracker.setCameraPreview(preview);
    }
  }

  public void removeCameraPreview(TextureView preview) {
    if (this.cameraPreview == preview) {
      this.cameraPreview = null;
      if (hasGazeTracker()) {
        gazeTracker.removeCameraPreview();
      }
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
        gazeTracker.setCallbacks(gazeCallback, calibrationCallback, imageCallback, statusCallback);
        if (cameraPreview != null) {
          gazeTracker.setCameraPreview(cameraPreview);
        }
      }
    }
  };
  private final GazeCallback gazeCallback = new GazeCallback() {
    @Override
    public void onGaze(GazeInfo gazeInfo) {
      for (GazeCallback gazeCallback : gazeCallbacks) {
        gazeCallback.onGaze(gazeInfo);
      }
    }
  };

  private CalibrationCallback calibrationCallback = new CalibrationCallback() {
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
      CalibrationDataStorage.saveCalibrationData(getApplicationContext(), doubles);
      for (CalibrationCallback calibrationCallback : calibrationCallbacks) {
        calibrationCallback.onCalibrationFinished(doubles);
      }
    }
  };

  private ImageCallback imageCallback = new ImageCallback() {
    @Override
    public void onImage(long l, byte[] bytes) {
      for (ImageCallback imageCallback : imageCallbacks) {
        imageCallback.onImage(l, bytes);
      }
    }
  };

  private  StatusCallback statusCallback = new StatusCallback() {
    @Override
    public void onStarted() {
      for (StatusCallback statusCallback : statusCallbacks) {
        statusCallback.onStarted();
      }
    }

    @Override
    public void onStopped(StatusErrorType statusErrorType) {
      for (StatusCallback statusCallback : statusCallbacks) {
        statusCallback.onStopped(statusErrorType);
      }
    }
  };

  private void setGazeTracker(GazeTracker gazeTracker) {
    this.gazeTracker = gazeTracker;
  }

  @Override
  public void onTerminate() {
    super.onTerminate();
    if (hasGazeTracker()) {
      GazeTracker.deinitGazeTracker(gazeTracker);
    }
  }
}