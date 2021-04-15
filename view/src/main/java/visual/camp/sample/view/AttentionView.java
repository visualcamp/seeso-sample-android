package visual.camp.sample.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;

public class AttentionView extends LinearLayout {
  public AttentionView(Context context) {
    super(context);
    init();
  }

  public AttentionView(Context context,
                       @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public AttentionView(Context context,
                       @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }
  @RequiresApi(VERSION_CODES.LOLLIPOP)
  public AttentionView(Context context, AttributeSet attrs,
                       int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init();
  }

  private Handler uiHandler = new Handler(Looper.getMainLooper());
  private ArrayList<Float> attentionHistory = new ArrayList<Float>();
  private int averageFpsTime = 150;
  private float threshold = 0.75f;
  private ImageView imgAttention, imgAttentionAvg;
  private Drawable drawableAttentionOn, drawableAttentionOff;
  private Drawable drawableAttentionAveOn, drawableAttentionAveOff;
  private void init() {
    inflate(getContext(), R.layout.view_attention, this);
    imgAttention = findViewById(R.id.img_attention);
    imgAttentionAvg = findViewById(R.id.img_attention_avg);

    drawableAttentionOn = getContext().getDrawable(R.drawable.attention_on_48);
    drawableAttentionOff = getContext().getDrawable(R.drawable.attention_off_48);
    drawableAttentionAveOn = getContext().getDrawable(R.drawable.attention_on_avg_48);;
    drawableAttentionAveOff = getContext().getDrawable(R.drawable.attention_off_avg_48);;
  }

  public void setAverageFpsTime(final int fpsTime) {
    averageFpsTime = fpsTime;
  }

  public void setAverageVisible(final boolean isVisible) {
    if (isVisible) {
      imgAttentionAvg.setVisibility(View.VISIBLE);
    } else {
      imgAttentionAvg.setVisibility(View.INVISIBLE);
    }
  }

  public void setAttentionAvg(final float attention) {
    uiHandler.post(new Runnable() {
      @Override
      public void run() {
        attentionHistory.add(new Float(attention));

        if (attentionHistory.size() > averageFpsTime) {
          attentionHistory.remove(0);

          float average = 0.0f;

          for (Float att : attentionHistory) {
            average = average + att.floatValue();
          }
          average /= attentionHistory.size();

          if (average >= threshold) {
            imgAttentionAvg.setImageDrawable(drawableAttentionAveOn);
          } else{
            imgAttentionAvg.setImageDrawable(drawableAttentionAveOff);
          }
        }
      }
    });
  }

  public void setAttention(final float attention) {
    uiHandler.post(new Runnable() {
      @Override
      public void run() {

        if (attention >= threshold) {
          imgAttention.setImageDrawable(drawableAttentionOn);
        } else {
          imgAttention.setImageDrawable(drawableAttentionOff);
        }
      }
    });
  }
}
