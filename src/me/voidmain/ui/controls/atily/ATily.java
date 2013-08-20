package me.voidmain.ui.controls.atily;

import me.voidmain.ui.controls.atily.TileSystemMeta.MetaFetchListener;
import me.voidmain.ui.controls.atily.TileSystemMeta.MetaFetchStatus;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
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
public class ATily extends SurfaceView implements SurfaceHolder.Callback, MetaFetchListener {
	public static final String TAG = ATily.class.getCanonicalName();

	public static final int NETWORK_TIMEOUT = 5000;

	protected String mBaseURL;
	protected RectF mViewportRect;
	protected float mViewportWidth;
	protected float mViewportHeight;

	protected TileSystemMeta mTSMeta;

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

		mBaseURL = "";
		parseAttrSet(attrs);

		if (!TextUtils.isEmpty(mBaseURL)) {
			mTSMeta = new TileSystemMeta(mBaseURL + "/meta.json", this);
		} else {
			Toast.makeText(getContext(), R.string.error_base_url_not_specified,
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return super.onTouchEvent(event);
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
		if(status == MetaFetchStatus.ERROR) {
			Toast.makeText(getContext(), R.string.error_response_parsing_error, Toast.LENGTH_LONG).show();
		} else { // Go ahead and use the meta info
			Log.d(TAG, mTSMeta.toString());
			// TODO calculate start
		}
	}

}
