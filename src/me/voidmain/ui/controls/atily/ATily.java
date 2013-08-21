package me.voidmain.ui.controls.atily;

import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import me.voidmain.ui.controls.atily.TileSystemMeta.MetaFetchListener;
import me.voidmain.ui.controls.atily.TileSystemMeta.MetaFetchStatus;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.view.GestureDetectorCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

/**
 * A set of UI controls that displays images generated from tily.rb
 * 
 * @Project aTily
 * @Package me.voidmain.ui.controls.atily
 * @Class ATily
 * @author voidmain
 */
public class ATily extends SurfaceView implements SurfaceHolder.Callback,
		MetaFetchListener, OnScaleGestureListener, OnGestureListener {
	public static final String TAG = ATily.class.getCanonicalName();

	public static final int NETWORK_TIMEOUT = 5000;

	protected String mBaseURL;
	protected RectF mViewportRect;
	protected float mViewportWidth;
	protected float mViewportHeight;
	protected float mWorldSize;
	protected float mCurLevel;

	protected Paint mBitmapPaint;

	protected TileSystemMeta mTSMeta;

	protected Set<Tile> mCoveredTiles;
	protected Set<String> mCurCoveredTileQuadKeys;
	protected Set<String> mLastCoveredTileQuadKeys;

	protected GestureDetectorCompat mGestureDetector;
	protected ScaleGestureDetector mScaleDetector;

	public ATily(Context context) {
		super(context);
		initialize(null);
	}

	public ATily(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(attrs);
	}

	public ATily(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize(attrs);
	}

	public void initialize(AttributeSet attrs) {
		getHolder().addCallback(this);

		mViewportRect = new RectF();
		mCoveredTiles = new HashSet<Tile>();
		mCurCoveredTileQuadKeys = new HashSet<String>();
		mLastCoveredTileQuadKeys = new HashSet<String>();

		mGestureDetector = new GestureDetectorCompat(getContext(), this);
		mScaleDetector = new ScaleGestureDetector(getContext(), this);

		mBitmapPaint = new Paint();

		mBaseURL = "";
		parseAttrSet(attrs);

		if (!TextUtils.isEmpty(mBaseURL)) {
			mTSMeta = new TileSystemMeta(mBaseURL + "/meta.json", this);
		} else {
			Toast.makeText(getContext(), R.string.error_base_url_not_specified,
					Toast.LENGTH_LONG).show();
		}
	}

	protected void doRedraw() {
		Canvas canvas = getHolder().lockCanvas();
		// Draws grey color to clear the canvas
		canvas.drawColor(Color.GRAY);

		// Draws current covered tiles
		for (Tile tile : mCoveredTiles) {
			tile.draw(canvas, mViewportRect, mCurLevel, mBitmapPaint);
		}
		getHolder().unlockCanvasAndPost(canvas);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mGestureDetector.onTouchEvent(event);
		mScaleDetector.onTouchEvent(event);

		super.onTouchEvent(event);
		return true;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		mViewportWidth = width;
		mViewportHeight = height;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mViewportWidth = getMeasuredWidth();
		mViewportHeight = getMeasuredHeight();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	private void parseAttrSet(AttributeSet attrs) {
		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs,
					R.styleable.ATily);
			final int N = a.getIndexCount();

			for (int i = 0; i < N; i++) {
				int attr = a.getIndex(i);
				switch (attr) {
				case R.styleable.ATily_base_url:
					mBaseURL = a.getString(attr);
					break;

				default:
					break;
				}
			}

			a.recycle();
		}
	}

	@Override
	public void metaFetched(MetaFetchStatus status) {
		if (status == MetaFetchStatus.ERROR) {
			Toast.makeText(getContext(), R.string.error_response_parsing_error,
					Toast.LENGTH_LONG).show();
		} else { // Go ahead and use the meta info
			Log.d(TAG, mTSMeta.toString());
			mCurLevel = mTSMeta.getTotalLevel() / 2;
			mWorldSize = (float) Math.pow(2, mCurLevel) * mTSMeta.getUnitSize();
			fillRectWithCenter(mWorldSize / 2, mWorldSize / 2, mViewportRect);

			updateCoveredTiles();
		}
	}

	private void fillRectWithCenter(float x, float y, RectF rect) {
		PointF center = new PointF(x, y);
		fillRectWithCenter(center, rect);
	}

	private void fillRectWithCenter(PointF center, RectF rect) {
		rect.left = center.x - mViewportWidth / 2;
		rect.right = center.x + mViewportWidth / 2;
		rect.top = center.y - mViewportHeight / 2;
		rect.bottom = center.y + mViewportHeight / 2;
	}

	private void scaleWorld(float scaleFactor, float focusX, float focusY) {
		float newWorldSize = mWorldSize * scaleFactor;
		float newLevel = (float) (Math
				.log(newWorldSize / mTSMeta.getUnitSize()) / Math.log(2));

		// Only update world size and level size when they are in bound
		// the bound is [1 .. mTSMeta.getTotalLevel() + 1)
		if (newLevel >= 1
				&& ((int) Math.floor(newLevel) <= mTSMeta.getTotalLevel())) {
			mWorldSize = newWorldSize;
			mCurLevel = newLevel;
		}

		Log.d(TAG, "Cur level: " + mCurLevel);

		float worldFocusX = mViewportRect.left + focusX;
		float worldFocusY = mViewportRect.top + focusY;

		float worldDiffX = worldFocusX * (scaleFactor - 1);
		float worldDiffY = worldFocusY * (scaleFactor - 1);
		mViewportRect.offset(worldDiffX, worldDiffY);

		// Finally, update covered tiles
		updateCoveredTiles();
	}

	private int normalizeIndex(int index, int max) {
		if (index < 0)
			index = 0;
		if (index >= max)
			index = max - 1;
		return index;
	}

	private void updateCoveredTiles() {
		// Remove last tiles
		mCurCoveredTileQuadKeys.clear();
		for (Tile tile : mCoveredTiles) {
			mCurCoveredTileQuadKeys.add(tile.getQuadKey());
		}
		mLastCoveredTileQuadKeys = new HashSet<String>(mCurCoveredTileQuadKeys);
		mCurCoveredTileQuadKeys.clear();

		// Calcuates covered tiles
		int level = (int) Math.floor(mCurLevel);
		float scaleFactor = (float) Math.pow(2, mCurLevel - level);
		float scaledUnitSize = scaleFactor * mTSMeta.getUnitSize();

		int maxCount = (int) Math.pow(2, level);
		int startX = normalizeIndex(
				(int) Math.floor(mViewportRect.left / scaledUnitSize), maxCount);
		int startY = normalizeIndex(
				(int) Math.floor(mViewportRect.top / scaledUnitSize), maxCount);
		int endX = normalizeIndex(
				(int) Math.floor(mViewportRect.right / scaledUnitSize),
				maxCount);
		int endY = normalizeIndex(
				(int) Math.floor(mViewportRect.bottom / scaledUnitSize),
				maxCount);
		for (int x = startX; x <= endX; x++) {
			for (int y = startY; y <= endY; y++) {
				mCurCoveredTileQuadKeys.add(tileXYToQuadKey(x, y, level));
			}
		}

		if (!mCurCoveredTileQuadKeys.equals(mLastCoveredTileQuadKeys)) {
			Set<String> removedTiles = new HashSet<String>(
					mLastCoveredTileQuadKeys);
			removedTiles.removeAll(mCurCoveredTileQuadKeys);

			Log.e(TAG, "Removed " + removedTiles.size());
			for (Iterator<Tile> i = mCoveredTiles.iterator(); i.hasNext();) {
				Tile tile = i.next();
				if (removedTiles.contains(tile.getQuadKey())) {
					tile.dispose();
					i.remove();
				}
			}

			Set<String> newTiles = new HashSet<String>(mCurCoveredTileQuadKeys);
			newTiles.removeAll(mLastCoveredTileQuadKeys);
			for (String quadKey : newTiles) {
				Tile newTile = new Tile(quadKey, mTSMeta);
				mCoveredTiles.add(newTile);
				
				TileFetcher fetcher = new TileFetcher();
				fetcher.execute(newTile);
			}
		}

		// Whether new tiles are added to the view, we should redraw due to
		// scale
		doRedraw();
	}

	private String buildTileUrl(String quadKey) {
		return mBaseURL + "/" + quadKey.length() + "/" + quadKey + ".png";
	}

	private String tileXYToQuadKey(int tileX, int tileY, int level) {
		StringBuilder quadKey = new StringBuilder();
		for (int i = level; i > 0; i--) {
			char digit = '0';
			int mask = 1 << (i - 1);
			if ((tileX & mask) != 0) {
				digit++;
			}
			if ((tileY & mask) != 0) {
				digit++;
				digit++;
			}
			quadKey.append(digit);
		}
		return quadKey.toString();
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		scaleWorld(detector.getScaleFactor(), detector.getFocusX(),
				detector.getFocusY());
		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {

	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		mViewportRect.offset(distanceX, distanceY);
		updateCoveredTiles();
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	public class TileFetcher extends AsyncTask<Tile, Void, Bitmap> {
		private Tile mCurTile = null;

		@Override
		protected Bitmap doInBackground(Tile... params) {
			try {
				mCurTile = params[0];
				String quadKey = mCurTile.getQuadKey();
				String url = buildTileUrl(quadKey);

				Bitmap bmp = ImageCache.tryCache(url);
				if (bmp == null) {
					InputStream is = (InputStream) new URL(url).getContent();
					bmp = drawableToBitmap(Drawable.createFromStream(is,
							quadKey));
					ImageCache.cacheImage(url, bmp);
				}
				return bmp;
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(Bitmap bmp) {
			mCurTile.setBitmap(bmp);
			Log.d(TAG, "Current tile count: " + mCoveredTiles.size());

			doRedraw();
		}

		private Bitmap drawableToBitmap(Drawable drawable) {
			if (drawable instanceof BitmapDrawable) {
				return ((BitmapDrawable) drawable).getBitmap();
			}

			int width = drawable.getIntrinsicWidth();
			width = width > 0 ? width : 1;
			int height = drawable.getIntrinsicHeight();
			height = height > 0 ? height : 1;

			Bitmap bitmap = Bitmap
					.createBitmap(width, height, Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			drawable.draw(canvas);

			return bitmap;
		}

	}

}
