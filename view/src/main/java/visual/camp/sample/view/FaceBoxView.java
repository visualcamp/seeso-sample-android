package visual.camp.sample.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class FaceBoxView extends View {
    public FaceBoxView(Context context) {
        super(context);
        init();
    }

    public FaceBoxView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FaceBoxView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public FaceBoxView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private int defaultColor = Color.rgb(0xFF, 0x00, 0xff);
    private Paint paint;
    private RectF box = new RectF();
    private int screenWidth, screenHeight;
    private void init() {
        paint = new Paint();
        paint.setColor(defaultColor);
        paint.setStyle(Style.STROKE);

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealMetrics(metrics); // 화면의 전체크기를 구함
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
    }

    private float offsetX, offsetY;
    public void setOffset(int x, int y) {
        offsetX = x;
        offsetY = y;
    }
    public void setPosition(float left, float top, float right, float bottom, float width, float height) {
        box.left = left * (screenWidth / width) - offsetX;
        box.top = top * (screenHeight/height) - offsetY;
        box.right = right * (screenWidth / width) - offsetX;
        box.bottom = bottom * (screenHeight/height) - offsetY;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(box,paint);
    }
}
