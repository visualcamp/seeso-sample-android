package visual.camp.sample.app.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.CalibrationCallback;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.UserStatusCallback;
import camp.visual.gazetracker.callback.InitializationCallback;
import camp.visual.gazetracker.constant.AccuracyCriteria;
import camp.visual.gazetracker.constant.CalibrationModeType;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.constant.UserStatusOption;
import camp.visual.gazetracker.device.CameraPosition;
import camp.visual.gazetracker.filter.OneEuroFilterManager;
import camp.visual.gazetracker.gaze.GazeInfo;
import camp.visual.gazetracker.state.ScreenState;
import camp.visual.gazetracker.state.TrackingState;
import camp.visual.gazetracker.util.ViewLayoutChecker;
import com.jiangdg.usb.USBMonitor;
import com.jiangdg.usb.USBMonitor.UsbControlBlock;
import com.jiangdg.uvc.IFrameCallback;
import com.jiangdg.uvc.UVCCamera;
import java.nio.ByteBuffer;
import java.util.Objects;
import visual.camp.sample.app.GazeTrackerManager;
import visual.camp.sample.app.GazeTrackerManager.LoadCalibrationResult;
import visual.camp.sample.app.R;
import visual.camp.sample.view.CalibrationViewer;
import visual.camp.sample.view.PointView;
import visual.camp.sample.view.EyeBlinkView;
import visual.camp.sample.view.AttentionView;
import visual.camp.sample.view.DrowsinessView;

public class MainActivity extends AppCompatActivity implements USBMonitor.OnDeviceConnectListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA
    };
    private static final int REQ_PERMISSION = 1000;
    private GazeTrackerManager gazeTrackerManager;
    private final ViewLayoutChecker viewLayoutChecker = new ViewLayoutChecker();
    private final HandlerThread backgroundThread = new HandlerThread("background");
    private Handler backgroundHandler;

    private final float USB_CAMERA_ORIGIN_X = 0;
    private final float USB_CAMERA_ORIGIN_Y = -8;

    private final float IMAGE_WIDTH = 640;
    private final float IMAGE_HEIGHT = 480;

    private boolean isConnected = false;

    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;

    private boolean isStartPreview = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gazeTrackerManager = GazeTrackerManager.makeNewInstance(this);
        Log.i(TAG, "gazeTracker version: " + GazeTracker.getVersionName());
        mUSBMonitor = new USBMonitor(this, this);
        mUSBMonitor.register();
        initView();
        checkPermission();
        initHandler();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        setOffsetOfView();
        if(gazeTrackerManager.hasGazeTracker()) {
            mUVCCamera.startPreview();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(gazeTrackerManager.hasGazeTracker()) {
            mUVCCamera.stopPreview();
        }
        Log.i(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        gazeTrackerManager.removeCallbacks(gazeCallback, calibrationCallback, userStatusCallback);
        Log.i(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mUVCCamera != null){
            mUVCCamera.stopPreview();
            mUVCCamera = null;
        }

        releaseHandler();
        mUSBMonitor.unregister();
        mUSBMonitor.destroy();
        viewLayoutChecker.releaseChecker();
    }

    // handler

    private void initHandler() {
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void releaseHandler() {
        backgroundThread.quitSafely();
    }

    // handler end

    // permission
    private void checkPermission() {
        // Check permission status
        if (!hasPermissions()) {
            requestPermissions(PERMISSIONS, REQ_PERMISSION);
        } else {
            checkPermission(true);
        }
    }
    private boolean hasPermissions() {
        int result;
        // Check permission status in string array
        for (String perms : MainActivity.PERMISSIONS) {
            if (perms.equals(Manifest.permission.SYSTEM_ALERT_WINDOW)) {
                if (!Settings.canDrawOverlays(this)) {
                    return false;
                }
            }
            result = ContextCompat.checkSelfPermission(this, perms);
            if (result == PackageManager.PERMISSION_DENIED) {
                // When if unauthorized permission found
                return false;
            }
        }
        // When if all permission allowed
        return true;
    }

    private void checkPermission(boolean isGranted) {
        if (isGranted) {
            permissionGranted();
        } else {
            showToast("not granted permissions", true);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSION) {
            if (grantResults.length > 0) {
                boolean cameraPermissionAccepted =
                    grantResults[0] == PackageManager.PERMISSION_GRANTED;
                checkPermission(cameraPermissionAccepted);
            }
        }
    }

    private void permissionGranted() {
        setViewAtGazeTrackerState();
    }
    // permission end

    // view
    private TextureView preview;
    private View layoutProgress;
    private View viewWarningTracking;
    private PointView viewPoint;
    private Button btnInitGaze, btnReleaseGaze;
    private Button btnStartTracking, btnStopTracking;
    private Button btnStartCalibration, btnStopCalibration, btnSetCalibration;
    private CalibrationViewer viewCalibration;
    private EyeBlinkView viewEyeBlink;
    private AttentionView viewAttention;
    private DrowsinessView viewDrowsiness;

    // gaze filter
    private SwitchCompat swUseGazeFilter;
    private SwitchCompat swStatusBlink, swStatusAttention, swStatusDrowsiness;
    private boolean isUseGazeFilter = true;
    private boolean isStatusBlink = false;
    private boolean isStatusAttention = false;
    private boolean isStatusDrowsiness = false;

    // calibration type
    private RadioGroup rgCalibration;
    private RadioGroup rgAccuracy;
    private CalibrationModeType calibrationType = CalibrationModeType.DEFAULT;
    private AccuracyCriteria criteria = AccuracyCriteria.DEFAULT;

    private void initView() {
        AppCompatTextView txtGazeVersion = findViewById(R.id.txt_gaze_version);
        txtGazeVersion.setText(getString(R.string.version_name, GazeTracker.getVersionName()));

        layoutProgress = findViewById(R.id.layout_progress);
        layoutProgress.setOnClickListener(null);

        viewWarningTracking = findViewById(R.id.view_warning_tracking);

        preview = findViewById(R.id.preview);
        preview.setSurfaceTextureListener(surfaceTextureListener);

        btnInitGaze = findViewById(R.id.btn_init_gaze);
        btnReleaseGaze = findViewById(R.id.btn_release_gaze);
        btnInitGaze.setOnClickListener(onClickListener);
        btnReleaseGaze.setOnClickListener(onClickListener);

        btnStartTracking = findViewById(R.id.btn_start_tracking);
        btnStopTracking = findViewById(R.id.btn_stop_tracking);
        btnStartTracking.setOnClickListener(onClickListener);
        btnStopTracking.setOnClickListener(onClickListener);

        btnStartCalibration = findViewById(R.id.btn_start_calibration);
        btnStopCalibration = findViewById(R.id.btn_stop_calibration);
        btnStartCalibration.setOnClickListener(onClickListener);
        btnStopCalibration.setOnClickListener(onClickListener);

        btnSetCalibration = findViewById(R.id.btn_set_calibration);
        btnSetCalibration.setOnClickListener(onClickListener);

        viewPoint = findViewById(R.id.view_point);
        viewCalibration = findViewById(R.id.view_calibration);

        swUseGazeFilter = findViewById(R.id.sw_use_gaze_filter);
        rgCalibration = findViewById(R.id.rg_calibration);
        rgAccuracy = findViewById(R.id.rg_accuracy);

        viewEyeBlink = findViewById(R.id.view_eye_blink);
        viewAttention = findViewById(R.id.view_attention);
        viewDrowsiness = findViewById(R.id.view_drowsiness);

        swStatusBlink = findViewById(R.id.sw_status_blink);
        swStatusAttention = findViewById(R.id.sw_status_attention);
        swStatusDrowsiness = findViewById(R.id.sw_status_drowsiness);

        swUseGazeFilter.setChecked(isUseGazeFilter);
        swStatusBlink.setChecked(isStatusBlink);
        swStatusAttention.setChecked(isStatusAttention);
        swStatusDrowsiness.setChecked(isStatusDrowsiness);

        RadioButton rbCalibrationOne = findViewById(R.id.rb_calibration_one);
        RadioButton rbCalibrationFive = findViewById(R.id.rb_calibration_five);
        RadioButton rbCalibrationSix = findViewById(R.id.rb_calibration_six);

        switch (calibrationType) {
            case ONE_POINT:
                rbCalibrationOne.setChecked(true);
                break;
            case SIX_POINT:
                rbCalibrationSix.setChecked(true);
                break;
            default:
                // default = five point
                rbCalibrationFive.setChecked(true);
                break;
        }

        swUseGazeFilter.setOnCheckedChangeListener(onCheckedChangeSwitch);
        swStatusBlink.setOnCheckedChangeListener(onCheckedChangeSwitch);
        swStatusAttention.setOnCheckedChangeListener(onCheckedChangeSwitch);
        swStatusDrowsiness.setOnCheckedChangeListener(onCheckedChangeSwitch);
        rgCalibration.setOnCheckedChangeListener(onCheckedChangeRadioButton);
        rgAccuracy.setOnCheckedChangeListener(onCheckedChangeRadioButton);

        viewEyeBlink.setVisibility(View.GONE);
        viewAttention.setVisibility(View.GONE);
        viewDrowsiness.setVisibility(View.GONE);

        hideProgress();
        setOffsetOfView();
        setViewAtGazeTrackerState();
    }

    private final RadioGroup.OnCheckedChangeListener onCheckedChangeRadioButton = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (group == rgCalibration) {
                if (checkedId == R.id.rb_calibration_one) {
                    calibrationType = CalibrationModeType.ONE_POINT;
                } else if (checkedId == R.id.rb_calibration_five) {
                    calibrationType = CalibrationModeType.FIVE_POINT;
                } else if (checkedId == R.id.rb_calibration_six) {
                    calibrationType = CalibrationModeType.SIX_POINT;
                }
            } else if (group == rgAccuracy) {
                if (checkedId == R.id.rb_accuracy_default) {
                    criteria = AccuracyCriteria.DEFAULT;
                } else if (checkedId == R.id.rb_accuracy_low) {
                    criteria = AccuracyCriteria.LOW;
                } else if (checkedId == R.id.rb_accuracy_high) {
                    criteria = AccuracyCriteria.HIGH;
                }
            }
        }
    };

    private final SwitchCompat.OnCheckedChangeListener onCheckedChangeSwitch = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (buttonView == swUseGazeFilter) {
                isUseGazeFilter = isChecked;
            } else if (buttonView == swStatusBlink) {
                isStatusBlink = isChecked;
                if (isStatusBlink) {
                    viewEyeBlink.setVisibility(View.VISIBLE);
                } else {
                    viewEyeBlink.setVisibility(View.GONE);
                }
            } else if (buttonView == swStatusAttention) {
                isStatusAttention = isChecked;
                if (isStatusAttention) {
                    viewAttention.setVisibility(View.VISIBLE);
                } else {
                    viewAttention.setVisibility(View.GONE);
                }
            } else if (buttonView == swStatusDrowsiness) {
                isStatusDrowsiness = isChecked;
                if (isStatusDrowsiness) {
                    viewDrowsiness.setVisibility(View.VISIBLE);
                } else {
                    viewDrowsiness.setVisibility(View.GONE);
                }
            }
        }
    };

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // When if textureView available
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private final ImageReader.OnImageAvailableListener imageAvailableListener = new OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            if(gazeTrackerManager.hasGazeTracker()) {
                gazeTrackerManager.addFrame(image);
            }
            image.close();
        }


    };

    // The gaze or calibration coordinates are delivered only to the absolute coordinates of the entire screen.
    // The coordinate system of the Android view is a relative coordinate system,
    // so the offset of the view to show the coordinates must be obtained and corrected to properly show the information on the screen.
    private void setOffsetOfView() {
        viewLayoutChecker.setOverlayView(viewPoint, new ViewLayoutChecker.ViewLayoutListener() {
            @Override
            public void getOffset(int x, int y) {
                viewPoint.setOffset(x, y);
                viewCalibration.setOffset(x, y);
            }
        });
    }

    private void showProgress() {
        if (layoutProgress != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    layoutProgress.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void hideProgress() {
        if (layoutProgress != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    layoutProgress.setVisibility(View.INVISIBLE);
                }
            });
        }
    }

    private void showTrackingWarning() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewWarningTracking.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideTrackingWarning() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewWarningTracking.setVisibility(View.INVISIBLE);
            }
        });
    }

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
             if (v == btnInitGaze) {
                initGaze();
            } else if (v == btnReleaseGaze) {
                releaseGaze();
            } else if (v == btnStartTracking) {
                startTracking();
            } else if (v == btnStopTracking) {
                stopTracking();
            } else if (v == btnStartCalibration) {
                startCalibration();
            } else if (v == btnStopCalibration) {
                stopCalibration();
            } else if (v == btnSetCalibration) {
                setCalibration();
            }
        }
    };

    private void showToast(final String msg, final boolean isShort) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, isShort ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showGazePoint(final float x, final float y, final ScreenState type) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewPoint.setType(type == ScreenState.INSIDE_OF_SCREEN ? PointView.TYPE_DEFAULT : PointView.TYPE_OUT_OF_SCREEN);
                viewPoint.setPosition(x, y);
            }
        });
    }

    private void setCalibrationPoint(final float x, final float y) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewCalibration.setVisibility(View.VISIBLE);
                viewCalibration.changeDraw(true, null);
                viewCalibration.setPointPosition(x, y);
                viewCalibration.setPointAnimationPower(0);
            }
        });
    }

    private void setCalibrationProgress(final float progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewCalibration.setPointAnimationPower(progress);
            }
        });
    }

    private void hideCalibrationView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewCalibration.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void setViewAtGazeTrackerState() {
        Log.i(TAG, "gaze : " + isTrackerValid() + ", tracking " + isTracking());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnInitGaze.setEnabled(isConnected && !isTrackerValid());
                btnReleaseGaze.setEnabled(isConnected && isTrackerValid());
                btnStartTracking.setEnabled(isConnected && isTrackerValid() && !isTracking());
                btnStopTracking.setEnabled(isConnected && isTracking());
                btnStartCalibration.setEnabled(isConnected && isTracking());
                btnStopCalibration.setEnabled(isConnected && isTracking());
                btnSetCalibration.setEnabled(isConnected && isTrackerValid());
                if (!isTracking()) {
                    hideCalibrationView();
                }
            }
        });
    }

    private void setStatusSwitchState(final boolean isEnabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isEnabled) {
                    swStatusBlink.setEnabled(false);
                    swStatusAttention.setEnabled(false);
                    swStatusDrowsiness.setEnabled(false);
                } else {
                    swStatusBlink.setEnabled(true);
                    swStatusAttention.setEnabled(true);
                    swStatusDrowsiness.setEnabled(true);
                }
            }
        });
    }

    // view end

    // gazeTracker
    private boolean isTrackerValid() {
      return gazeTrackerManager.hasGazeTracker();
    }

    private boolean isTracking() {
        // cameraManager isTracking
      return isStartPreview;
    }

    private final InitializationCallback initializationCallback = new InitializationCallback() {
        @Override
        public void onInitialized(GazeTracker gazeTracker, InitializationErrorType error) {
            if (gazeTracker != null) {
                initSuccess();
            } else {
                initFail(error);
            }
        }
    };

    private void initSuccess() {
        CameraPosition current = gazeTrackerManager.getCameraPosition();
        gazeTrackerManager.setCameraPosition(new CameraPosition("USB-Camera", current.screenWidth, current.screenHeight, USB_CAMERA_ORIGIN_X, USB_CAMERA_ORIGIN_Y));
        gazeTrackerManager.setGazeTrackerCallbacks(gazeCallback, calibrationCallback, userStatusCallback);
        setViewAtGazeTrackerState();
        hideProgress();
    }

    private void initFail(InitializationErrorType error) {
        hideProgress();
        Log.e(TAG, "init failed  : " + error.toString());
    }

    private final OneEuroFilterManager oneEuroFilterManager = new OneEuroFilterManager(2);
    private final GazeCallback gazeCallback = new GazeCallback() {
      @Override
      public void onGaze(GazeInfo gazeInfo) {
        processOnGaze(gazeInfo);
        Log.i(TAG, "check eyeMovement " + gazeInfo.eyeMovementState);
      }
    };

    private final UserStatusCallback userStatusCallback = new UserStatusCallback() {
        @Override
        public void onAttention(long timestampBegin, long timestampEnd, float attentionScore) {
          Log.i(TAG, "check User Status Attention Rate " + attentionScore);
            viewAttention.setAttention(attentionScore);
        }

        @Override
        public void onBlink(long timestamp, boolean isBlinkLeft, boolean isBlinkRight, boolean isBlink, float leftOpenness, float rightOpenness) {
          Log.i(TAG, "check User Status Blink " +  "Left: " + isBlinkLeft + ", Right: " + isBlinkRight + ", Blink: " + isBlink + ", eyeOpenness: " + leftOpenness +", " + rightOpenness);
          viewEyeBlink.setLeftEyeBlink(isBlinkLeft);
          viewEyeBlink.setRightEyeBlink(isBlinkRight);
          viewEyeBlink.setEyeBlink(isBlink);
        }

        @Override
        public void onDrowsiness(long timestamp, boolean isDrowsiness, float intensity) {
          Log.i(TAG, "check User Status Drowsiness " + isDrowsiness);
          viewDrowsiness.setDrowsiness(isDrowsiness);
        }
    };

    private void processOnGaze(GazeInfo gazeInfo) {
      if (gazeInfo.trackingState == TrackingState.SUCCESS) {
        hideTrackingWarning();
        if (!gazeTrackerManager.isCalibrating()) {
          float[] filtered_gaze = filterGaze(gazeInfo);
          showGazePoint(filtered_gaze[0], filtered_gaze[1], gazeInfo.screenState);
        }
      } else {
        showTrackingWarning();
      }
    }

    private float[] filterGaze(GazeInfo gazeInfo) {
      if (isUseGazeFilter) {
        if (oneEuroFilterManager.filterValues(gazeInfo.timestamp, gazeInfo.x, gazeInfo.y)) {
          return oneEuroFilterManager.getFilteredValues();
        }
      }
      return new float[]{gazeInfo.x, gazeInfo.y};
    }

    private final CalibrationCallback calibrationCallback = new CalibrationCallback() {
        @Override
        public void onCalibrationProgress(float progress) {
            setCalibrationProgress(progress);
        }

        @Override
        public void onCalibrationNextPoint(final float x, final float y) {
            setCalibrationPoint(x, y);
            // Give time to eyes find calibration coordinates, then collect data samples
            backgroundHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startCollectSamples();
                }
            }, 1000);
        }

        @Override
        public void onCalibrationFinished(double[] calibrationData) {
            // When calibration is finished, calibration data is stored to SharedPreference

            hideCalibrationView();
            showToast("calibrationFinished", true);
        }
    };

    private void initGaze() {
        showProgress();

        UserStatusOption userStatusOption = new UserStatusOption();
        if (isStatusAttention) {
          userStatusOption.useAttention();
        }
        if (isStatusBlink) {
          userStatusOption.useBlink();
        }
        if (isStatusDrowsiness) {
          userStatusOption.useDrowsiness();
        }
        userStatusOption.useExternalMode(640, 480, 50);
        Log.i(TAG, "init option attention " + isStatusAttention + ", blink " + isStatusBlink + ", drowsiness " + isStatusDrowsiness);

        gazeTrackerManager.initGazeTracker(initializationCallback, userStatusOption);
        setStatusSwitchState(false);
    }

    private void releaseGaze() {
      gazeTrackerManager.deinitGazeTracker();
      setStatusSwitchState(true);
      stopTracking();
      setViewAtGazeTrackerState();
    }

    private void startTracking() {
        if(mUVCCamera != null){
           mUVCCamera.startPreview();
            isStartPreview = true;
        }
        setViewAtGazeTrackerState();
    }

    private void stopTracking() {
        if(mUVCCamera != null){
            mUVCCamera.stopPreview();
            isStartPreview = false;
        }
        setViewAtGazeTrackerState();
    }

    private void startCalibration() {
      boolean isSuccess = gazeTrackerManager.startCalibration(calibrationType, criteria);
      if (!isSuccess) {
        showToast("calibration start fail", false);
      }
      setViewAtGazeTrackerState();
    }

    // Collect the data samples used for calibration
    private void startCollectSamples() {
      gazeTrackerManager.startCollectingCalibrationSamples();
      setViewAtGazeTrackerState();
    }

    private void stopCalibration() {
      gazeTrackerManager.stopCalibration();
      hideCalibrationView();
      setViewAtGazeTrackerState();
    }

    private void setCalibration() {
      LoadCalibrationResult result = gazeTrackerManager.loadCalibrationData();
      switch (result) {
        case SUCCESS:
          showToast("setCalibrationData success", false);
          break;
        case FAIL_DOING_CALIBRATION:
          showToast("calibrating", false);
          break;
        case FAIL_NO_CALIBRATION_DATA:
          showToast("Calibration data is null", true);
          break;
        case FAIL_HAS_NO_TRACKER:
          showToast("No tracker has initialized", true);
          break;
      }
      setViewAtGazeTrackerState();
    }
    @Override
    public void onAttach(UsbDevice device) {
        if (mUSBMonitor != null) {
            mUSBMonitor.requestPermission(device);
        }
    }

    @Override
    public void onDetach(UsbDevice device) {
        if (mUSBMonitor != null) {
            if (mUVCCamera != null) {
                mUVCCamera.close();
                mUVCCamera = null;
            }
        }
    }

    @Override
    public void onConnect(UsbDevice device, UsbControlBlock ctrlBlock, boolean createNew) {
        if (device.getManufacturerName() == null || !Objects.requireNonNull(
            device).getManufacturerName().equals("Unknown")) {
            mUVCCamera = new UVCCamera();
            mUVCCamera.open(ctrlBlock);
            mUVCCamera.setPreviewSize(640,480);
            mUVCCamera.setPreviewTexture(this.preview.getSurfaceTexture());
            mUVCCamera.setFrameCallback(new IFrameCallback() {
                @Override
                public void onFrame(ByteBuffer frame) {
                    gazeTrackerManager.addImageBuffer(frame, 640, 480);
                }
            }, UVCCamera.PIXEL_FORMAT_NV21);
            isConnected = true;
            setViewAtGazeTrackerState();
        } else {
            showToast("Failed Usb device connected", false);
        }
    }

    @Override
    public void onDisconnect(UsbDevice device, UsbControlBlock ctrlBlock) {
        if (mUVCCamera != null) {
            mUVCCamera.close();
            mUVCCamera = null;
        }
    }
    @Override
    public void onCancel(UsbDevice device) {

    }
}
