package me.panavtec.drawableview;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Build;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import me.panavtec.drawableview.gestures.CanvasDrawerListener;
import me.panavtec.drawableview.gestures.CanvasDrawer;
import me.panavtec.drawableview.gestures.GestureDrawer;
import me.panavtec.drawableview.gestures.GestureDrawerListener;
import me.panavtec.drawableview.gestures.Scaler;
import me.panavtec.drawableview.gestures.ScalerListener;
import me.panavtec.drawableview.gestures.Scroller;
import me.panavtec.drawableview.gestures.ScrollerListener;
import me.panavtec.drawableview.internal.DrawableViewSaveState;
import me.panavtec.drawableview.internal.SerializablePath;

public class DrawableView extends View
    implements View.OnTouchListener, ScrollerListener, GestureDrawerListener, ScalerListener,
    CanvasDrawerListener {

  private final ArrayList<SerializablePath> historyPaths = new ArrayList<>();
  private Scroller scroller;
  private Scaler scaler;
  private GestureDrawer gestureGestureDrawer;
  private CanvasDrawer canvasDrawer;
  private int canvasHeight;
  private int canvasWidth;

  public DrawableView(Context context) {
    super(context);
    init();
  }

  public DrawableView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public DrawableView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP) public DrawableView(Context context, AttributeSet attrs,
      int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init();
  }
  
  private void init() {
    scroller = new Scroller(getContext(), this, true);
    scaler = new Scaler(getContext(), this);
    gestureGestureDrawer = new GestureDrawer(this);
    canvasDrawer = new CanvasDrawer(this);
    setOnTouchListener(this);
  }

  public void setConfig(DrawableViewConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("Paint configuration cannot be null");
    }

    this.canvasWidth = config.getCanvasWidth();
    this.canvasHeight = config.getCanvasHeight();
    canvasDrawer.setCanvasBounds(canvasWidth, canvasHeight);
    scaler.setMinZoom(config.getMinZoom());
    scaler.setMaxZoom(config.getMaxZoom());
    gestureGestureDrawer.setConfig(config);
    scroller.setCanvasBounds(canvasWidth, canvasHeight);
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
    int viewHeight = MeasureSpec.getSize(heightMeasureSpec);
    scroller.setViewBounds(viewWidth, viewHeight);
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

  @Override public boolean onTouch(View v, MotionEvent event) {
    scaler.onTouchEvent(event);
    scroller.onTouchEvent(event);
    gestureGestureDrawer.onTouchEvent(event);
    invalidate();
    return true;
  }

  public void undo() {
    if (historyPaths.size() > 0) {
      historyPaths.remove(historyPaths.size() - 1);
      invalidate();
    }
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.save();
    canvasDrawer.onDraw(canvas);
    scaler.onDraw(canvas);
    scroller.onDraw(canvas);
    drawGestures(canvas);
    canvas.restore();
  }

  private void drawGestures(Canvas canvas) {
    gestureGestureDrawer.drawGestures(canvas, historyPaths);
    gestureGestureDrawer.onDraw(canvas);
  }

  public void clear() {
    historyPaths.clear();
    invalidate();
  }

  public Bitmap obtainBitmap() {
    Bitmap bmp = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
    Canvas composeCanvas = new Canvas(bmp);
    drawGestures(composeCanvas);
    return bmp;
  }

  @Override protected Parcelable onSaveInstanceState() {
    return new DrawableViewSaveState(super.onSaveInstanceState(), historyPaths);
  }

  @Override protected void onRestoreInstanceState(Parcelable state) {
    super.onRestoreInstanceState(state);
    if (state instanceof DrawableViewSaveState) {
      restoreState((DrawableViewSaveState) state);
    }
  }

  private void restoreState(DrawableViewSaveState saveState) {
    ArrayList<SerializablePath> savedPaths = saveState.getHistoryPaths();
    for (SerializablePath p : savedPaths) {
      p.loadPathPointsAsQuadTo();
    }
    this.historyPaths.addAll(savedPaths);
  }

  @Override public void onViewPortChange(RectF currentViewport) {
    canvasDrawer.changedViewPort(currentViewport);
    gestureGestureDrawer.changedViewPort(currentViewport);
  }

  @Override public void onGestureDrawedOk(SerializablePath serializablePath) {
    historyPaths.add(serializablePath);
  }

  @Override public void onScaleChange(float scaleFactor) {
    canvasDrawer.onScaleChange(scaleFactor);
    scroller.onScaleChange(scaleFactor);
    gestureGestureDrawer.onScaleChange(scaleFactor);
  }

  @Override public void onCanvasPortChanged(RectF canvasRect) {
    gestureGestureDrawer.onCanvasPortChanged(canvasRect);
  }
  
}
