package com.proinlab.proincanvas;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ProinCanvas extends View {

	private static final int MAXHISTORY = 30;

	public static final int PENSTYLE_ERASER = 0;
	public static final int PENSTYLE_PEN = 1;
	public static final int PENSTYLE_BRUSH = 2;

	private Bitmap HistoryBitmap;
	private Canvas mCanvas;
	private Path mPath;
	private Paint mBitmapPaint;
	private Paint mPaint;

	private ArrayList<Path> PathArray = new ArrayList<Path>();
	private ArrayList<Paint> PaintArray = new ArrayList<Paint>();
	private int HistoryPoint = 0;
	private Bitmap preBitmap;

	private int penstyle = PENSTYLE_PEN;
	private int penwidth = 4;
	private int pencolor = Color.BLACK;
	private int penalpha = 255;

	private int width, height;

	public ProinCanvas(Context context) {
		super(context);
	}

	public ProinCanvas(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ProinCanvas(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawBitmap(preBitmap, 0, 0, mBitmapPaint);

		if (penstyle != PENSTYLE_ERASER)
			canvas.drawPath(mPath, mPaint);
	}

	private float mX, mY;
	private static final float TOUCH_TOLERANCE = 1;

	private void touch_start(float x, float y) {
		mPath.reset();
		mPath.moveTo(x, y);
		mX = x;
		mY = y;
	}

	private void touch_move(float x, float y) {
		float dx = Math.abs(x - mX);
		float dy = Math.abs(y - mY);
		if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
			mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
			mX = x;
			mY = y;
		}
		if (penstyle == PENSTYLE_ERASER) {
			Canvas tmpCanvas = new Canvas(preBitmap);
			tmpCanvas.drawPath(mPath, mPaint);
		}
	}

	private void touch_up() {
		HistoryPoint++;
		if (HistoryPoint == MAXHISTORY)
			HistoryPoint--;

		mPath.lineTo(mX, mY);
		PathArray.add(mPath);
		PaintArray.add(mPaint);

		Canvas tmpCanvas = new Canvas(preBitmap);
		tmpCanvas.drawPath(mPath, mPaint);
		if (PathArray.size() == MAXHISTORY - 1) {
			Canvas HistoryCanvas = new Canvas(HistoryBitmap);
			HistoryCanvas.drawPath(PathArray.get(0), PaintArray.get(0));
			PathArray.remove(0);
			PaintArray.remove(0);
		}

		for (int i = MAXHISTORY - 1; i > HistoryPoint - 2; i--) {
			if (PathArray.size() > i) {
				PathArray.remove(i);
				PaintArray.remove(i);
			}
		}
		mPath = new Path();
	}

	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		float x = event.getX();
		float y = event.getY();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			touch_start(x, y);
			break;
		case MotionEvent.ACTION_MOVE:
			touch_move(x, y);
			break;
		case MotionEvent.ACTION_UP:
			touch_up();
			break;
		}
		invalidate();
		return true;
	}

	/**
	 * Canvas를 비트맵으로 가져온다
	 */
	public Bitmap getCanvasBitmap() {
		Bitmap tmpBit = Bitmap.createBitmap(this.width, this.height,
				Bitmap.Config.ARGB_8888);
		Canvas tmpC = new Canvas(tmpBit);
		tmpC.drawBitmap(HistoryBitmap, 0, 0, mBitmapPaint);
		for (int i = 0; i < PathArray.size(); i++)
			tmpC.drawPath(PathArray.get(i), PaintArray.get(i));
		return tmpBit;
	}

	/**
	 * 작업내역 비트맵을 불러온다
	 */
	public void setCanvasBitmap(Bitmap bitmap) {
		if (bitmap == null)
			return;
		HistoryBitmap = Bitmap.createBitmap(this.width, this.height,
				Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(HistoryBitmap);
		mCanvas.drawBitmap(bitmap, 0, 0, mBitmapPaint);
		bitmap.recycle();
		invalidate();

		preBitmap = Bitmap.createBitmap(this.width, this.height,
				Bitmap.Config.ARGB_8888);
		Canvas tmpCanvas = new Canvas(preBitmap);
		tmpCanvas.drawBitmap(HistoryBitmap, 0, 0, mBitmapPaint);

		PathArray = new ArrayList<Path>();
		PaintArray = new ArrayList<Paint>();

	}

	/**
	 * 되돌리기
	 */
	public void undo() {
		if (undoable()) {
			preBitmap = Bitmap.createBitmap(this.width, this.height,
					Bitmap.Config.ARGB_8888);
			Canvas tmpCanvas = new Canvas(preBitmap);
			tmpCanvas.drawBitmap(HistoryBitmap, 0, 0, mBitmapPaint);
			HistoryPoint--;
			for (int i = 0; i < HistoryPoint; i++)
				tmpCanvas.drawPath(PathArray.get(i), PaintArray.get(i));

			invalidate();
		}
	}

	/**
	 * 되돌리기가 가능 여부
	 */
	public boolean undoable() {
		if (HistoryPoint > 0)
			return true;
		else
			return false;
	}

	/**
	 * 복귀
	 */
	public void redo() {
		if (redoable()) {
			preBitmap = Bitmap.createBitmap(this.width, this.height,
					Bitmap.Config.ARGB_8888);
			Canvas tmpCanvas = new Canvas(preBitmap);
			tmpCanvas.drawBitmap(HistoryBitmap, 0, 0, mBitmapPaint);
			HistoryPoint++;
			for (int i = 0; i < HistoryPoint; i++)
				tmpCanvas.drawPath(PathArray.get(i), PaintArray.get(i));

			invalidate();
		}
	}

	/**
	 * 복귀 가능 여부
	 */
	public boolean redoable() {
		if (PathArray.size() - HistoryPoint > 0)
			return true;
		else
			return false;
	}

	/**
	 * 펜의 색상을 변경해준다. Default : Black
	 */
	public void setPenColor(int color) {
		pencolor = color;
		setPaintStyle();
	}

	/**
	 * 펜의 두께를 변경해준다. Default : 4
	 */
	public void setPenWidth(int size) {
		penwidth = size;
		setPaintStyle();
	}

	/**
	 * 펜의 스타일을 변경해준다. AboutPen.penStyle
	 */
	public void setPenStyle(int penStyle) {
		penstyle = penStyle;
		setPaintStyle();
	}

	/**
	 * 펜의 투명도를 설정해준다
	 */
	public void setPenAlpha(int alpha) {
		penalpha = alpha;
		setPaintStyle();
	}

	/**
	 * 펜의 색상을 반환한다
	 */
	public int getPenColor() {
		return pencolor;
	}

	/**
	 * 펜의 투명도를 반환한다
	 */
	public int getPenAlpha() {
		return penalpha;
	}

	/**
	 * 펜의 두께를 반환한다
	 */
	public int getPenWidth() {
		return penwidth;
	}

	/**
	 * 펜의 스타일을 반환한다
	 */
	public String getPenStyle() {
		switch (penstyle) {
		case PENSTYLE_PEN:
			return "Pen";
		case PENSTYLE_BRUSH:
			return "Brush";
		case PENSTYLE_ERASER:
			return "PENSTYLE_ERASER";
		}
		return null;

	}

	/**
	 * CanvasView를 메모리상에서 free시킨다.
	 */
	public void distroyView() {
		HistoryBitmap.recycle();
	}

	/*--------------------------------------------------------*/
	// Custom View Functions
	/*--------------------------------------------------------*/

	@Override
	protected void onFinishInflate() {
		setClickable(true);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = 0;
		switch (heightMode) {
		case MeasureSpec.UNSPECIFIED:
			heightSize = heightMeasureSpec;
			break;
		case MeasureSpec.AT_MOST:
			heightSize = 20;
			break;
		case MeasureSpec.EXACTLY:
			heightSize = MeasureSpec.getSize(heightMeasureSpec);
			break;
		}

		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = 0;
		switch (widthMode) {
		case MeasureSpec.UNSPECIFIED:
			widthSize = widthMeasureSpec;
			break;
		case MeasureSpec.AT_MOST:
			widthSize = 100;
			break;
		case MeasureSpec.EXACTLY:
			widthSize = MeasureSpec.getSize(widthMeasureSpec);
			break;
		}

		setMeasuredDimension(widthSize, heightSize);

		width = widthSize;
		height = heightSize;

		initCanvas();

	}

	private void initCanvas() {
		HistoryBitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(HistoryBitmap);
		mPath = new Path();
		mBitmapPaint = new Paint(Paint.DITHER_FLAG);

		preBitmap = Bitmap.createBitmap(this.width, this.height,
				Bitmap.Config.ARGB_8888);

		setPaintStyle();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {

	}

	/*--------------------------------------------------------*/
	// ProinCanvas Private Functions
	/*--------------------------------------------------------*/

	private void setPaintStyle() {
		switch (penstyle) {
		case PENSTYLE_PEN:
			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			mPaint.setDither(true);
			mPaint.setColor(pencolor);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeJoin(Paint.Join.ROUND);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setStrokeWidth(penwidth);
			mPaint.setAlpha(penalpha);
			break;
		case PENSTYLE_ERASER:
			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			mPaint.setDither(true);
			mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeJoin(Paint.Join.ROUND);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setAlpha(0);
			mPaint.setStrokeWidth(penwidth);
			break;
		case PENSTYLE_BRUSH:
			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			mPaint.setDither(true);
			mPaint.setColor(pencolor);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeJoin(Paint.Join.ROUND);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setStrokeWidth(penwidth);
			mPaint.setAlpha(penalpha);
			break;
		}
	}

}
