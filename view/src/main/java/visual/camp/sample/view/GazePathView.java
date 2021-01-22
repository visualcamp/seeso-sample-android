package visual.camp.sample.view;

import android.animation.FloatEvaluator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class GazePathView extends View {

  private static final float MIN_POINT_RADIUS = 10;
  private static final float MAX_POINT_RADIUS = 100;
  private static final long MAX_FIXATION_SIZE_TIME = 3500;
  private static final float SACCADE_LINE_WIDTH = 2.F;
  private static final int SACCADE_POINT_MODIFY_COUNT = 5;

  public GazePathView(Context context) {
    super(context);
    init();
  }

  public GazePathView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public GazePathView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  public GazePathView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init();
  }

  private int defaultColor = Color.rgb(0x00, 0x00, 0xff);

  private Paint pointPaint;
  private Paint linePaint;

  private void init() {
    pointPaint = new Paint();
    pointPaint.setColor(defaultColor);

    linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    linePaint.setColor(defaultColor);
    linePaint.setStrokeWidth(SACCADE_LINE_WIDTH);

    curPointSize = MIN_POINT_RADIUS;

    evaluator = new FloatEvaluator();
  }
  private FloatEvaluator evaluator;
  private float offsetX, offsetY;
  private PointF fixationPoint = new PointF();
  private PointF saccadeTarget = null;
  private int saccadeCount = 0;
  private float curPointSize;
  private long firstFixationTime = 0;

  public void setOffset(int x, int y) {
    offsetX = x;
    offsetY = y;
  }

  public void onGaze(float x, float y, boolean is_fixation) {
    if (saccadeCount > 0 || !is_fixation) {
      if (saccadeTarget == null) saccadeTarget = new PointF(x - offsetX, y - offsetY);
      if (saccadeCount % SACCADE_POINT_MODIFY_COUNT == 0) fixationPoint = saccadeTarget;
      saccadeTarget = new PointF(x - offsetX, y - offsetY);
    } else {
      saccadeTarget = null;
      fixationPoint.x = x - offsetX;
      fixationPoint.y = y - offsetY;
    }
    if (is_fixation) {
      saccadeCount = 0;
    } else {
      saccadeCount ++;
    }
    calculatePointSize(is_fixation);
    invalidate();
  }

  private void calculatePointSize(boolean is_fixation) {
    if (!is_fixation) {
      firstFixationTime = 0;
      curPointSize = MIN_POINT_RADIUS;
    } else {
      if (firstFixationTime == 0) {
        curPointSize = MIN_POINT_RADIUS;
        firstFixationTime = System.currentTimeMillis();
      } else {
        long timeDiff = System.currentTimeMillis() - firstFixationTime;
        curPointSize = evaluator.evaluate((float)timeDiff / MAX_FIXATION_SIZE_TIME, MIN_POINT_RADIUS, MAX_POINT_RADIUS);
        curPointSize = Math.min(curPointSize, MAX_POINT_RADIUS);
      }
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.drawCircle(fixationPoint.x, fixationPoint.y, curPointSize, pointPaint);
    if (saccadeTarget != null) {
      canvas.drawCircle(saccadeTarget.x, saccadeTarget.y, curPointSize, pointPaint);
      canvas.drawLine(fixationPoint.x, fixationPoint.y, saccadeTarget.x, saccadeTarget.y, linePaint);
    }
  }
}
