package visual.camp.sample.app.activity;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.filter.OneEuroFilterManager;
import camp.visual.gazetracker.gaze.GazeInfo;
import camp.visual.gazetracker.state.EyeMovementState;
import camp.visual.gazetracker.util.ViewLayoutChecker;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import visual.camp.sample.app.GazeTrackerManager;
import visual.camp.sample.app.R;
import visual.camp.sample.view.GazePathView;

public class DemoActivity extends AppCompatActivity {
  private static final String TAG = DemoActivity.class.getSimpleName();
  private final ViewLayoutChecker viewLayoutChecker = new ViewLayoutChecker();
  private GazePathView gazePathView;
  private GazeTrackerManager gazeTrackerManager;
  private final OneEuroFilterManager oneEuroFilterManager = new OneEuroFilterManager(
      2, 30, 0.5F, 0.001F, 1.0F);

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demo);
    gazeTrackerManager = GazeTrackerManager.getInstance();
    Log.i(TAG, "gazeTracker version: " + GazeTracker.getVersionName());
  }

  @Override
  protected void onStart() {
    super.onStart();
    Log.i(TAG, "onStart");
    gazeTrackerManager.setGazeTrackerCallbacks(gazeCallback);
    initView();
  }

  @Override
  protected void onResume() {
    super.onResume();
    gazeTrackerManager.startGazeTracking();
    setOffsetOfView();
    Log.i(TAG, "onResume");
  }

  @Override
  protected void onPause() {
    super.onPause();
    gazeTrackerManager.stopGazeTracking();
    Log.i(TAG, "onPause");
  }

  @Override
  protected void onStop() {
    super.onStop();
    gazeTrackerManager.removeCallbacks(gazeCallback);
    Log.i(TAG, "onStop");
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  private void initView() {
    gazePathView = findViewById(R.id.gazePathView);

    AssetManager am = getResources().getAssets();
    InputStream is = null;

    try {
      is = am.open("palace_seoul.jpg");
      Bitmap bm = BitmapFactory.decodeStream(is);
      ImageView catView = findViewById(R.id.catImage);
      catView.setImageBitmap(bm);
      is.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void setOffsetOfView() {
    viewLayoutChecker.setOverlayView(gazePathView, new ViewLayoutChecker.ViewLayoutListener() {
      @Override
      public void getOffset(int x, int y) {
        gazePathView.setOffset(x, y);
      }
    });
  }

  private final GazeCallback gazeCallback = new GazeCallback() {
    @Override
    public void onGaze(GazeInfo gazeInfo) {
      if (oneEuroFilterManager.filterValues(gazeInfo.timestamp, gazeInfo.x, gazeInfo.y)) {
        float[] filtered = oneEuroFilterManager.getFilteredValues();
        gazePathView.onGaze(filtered[0], filtered[1], gazeInfo.eyeMovementState == EyeMovementState.FIXATION);
      }
    }
  };
}
