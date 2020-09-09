package visual.camp.sample.app.calibration;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Arrays;

public class CalibrationDataStorage {
  private static final String TAG = CalibrationDataStorage.class.getSimpleName();
  private static final String CALIBRATION_DATA = "calibrationData";

  // 캘리브레이션 데이터를 SharedPreferences에 저장
  public static void saveCalibrationData(Context context, double[] calibrationData) {
    if (calibrationData != null && calibrationData.length > 0) {
      SharedPreferences.Editor editor = context.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit();
      editor.putString(CALIBRATION_DATA, Arrays.toString(calibrationData));
      editor.apply();
    } else {
      Log.e(TAG, "Abnormal calibration Data");
    }
  }

  // SharedPreferences에 저장한 캘리브레이션 데이터를 가져옴
  public static @Nullable
  double[] loadCalibrationData(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
    String saveData = prefs.getString(CALIBRATION_DATA, null);

    if (saveData != null) {
      try {
        String[] split = saveData.substring(1, saveData.length() - 1).split(", ");
        double[] array = new double[split.length];
        for (int i = 0; i < split.length; i++) {
          array[i] = Double.parseDouble(split[i]);
        }
        return array;
      } catch (Exception e) {
        e.printStackTrace();
        Log.e(TAG, "Maybe unmatched type of calibration data");
      }
    }
    return null;
  }
}
