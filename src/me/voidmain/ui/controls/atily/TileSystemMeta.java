package me.voidmain.ui.controls.atily;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.os.AsyncTask;

/**
 * Meta data read from tile system
 * 
 * @Project aTily
 * @Package me.voidmain.ui.controls.atily
 * @Class TileSystemMeta
 * @author voidmain
 */
public class TileSystemMeta {
	protected int mTotalLevel;
	protected int mUnitSize;
	protected int mRawWidth;
	protected int mRawHeight;
	protected MetaFetcher mFetcher;
	
	enum MetaFetchStatus {
		DONE, 
		ERROR
	}
	
	public interface MetaFetchListener {
		public void metaFetched(MetaFetchStatus status);
	}
	
	protected MetaFetchListener mListener;

	public TileSystemMeta(String metaUrl, MetaFetchListener listener) {
		mFetcher = new MetaFetcher();
		mFetcher.execute(metaUrl);
		mListener = listener;
	}

	public int getTotalLevel() {
		return mTotalLevel;
	}

	public void setTotalLevel(int totalLevel) {
		this.mTotalLevel = totalLevel;
	}

	public int getUnitSize() {
		return mUnitSize;
	}

	public void setUnitSize(int unitSize) {
		this.mUnitSize = unitSize;
	}

	public int getRawWidth() {
		return mRawWidth;
	}

	public void setRawWidth(int rawWidth) {
		this.mRawWidth = rawWidth;
	}

	public int getRawHeight() {
		return mRawHeight;
	}

	public void setRawHeight(int rawHeight) {
		this.mRawHeight = rawHeight;
	}

	@Override
	public String toString() {
		return "[TileSystemMeta] TotalLevel: " + mTotalLevel + ", UnitSize: " + mUnitSize + ", RawWidth: " + mRawWidth + ", RawHeight: " + mRawHeight;
	}

	class MetaFetcher extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... params) {
			String result = null;
			HttpClient httpClient = new DefaultHttpClient();

			HttpGet httpGet = new HttpGet(params[0]);
			try {
				HttpResponse response = httpClient.execute(httpGet);
				StringBuilder builder = new StringBuilder();
				Reader in = new BufferedReader(new InputStreamReader(response
						.getEntity().getContent(), "UTF-8"));
				char[] buf = new char[1024];
				int l = 0;
				while(l >= 0) {
					builder.append(buf, 0, l);
					l = in.read(buf);
				}
				
				result = builder.toString();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			if (result == null) {
				if (mListener != null) {
					mListener.metaFetched(MetaFetchStatus.ERROR);
				}
				return;
			}

			try {
				JSONObject rawMeta = (JSONObject) new JSONTokener(result)
						.nextValue();
				setTotalLevel(rawMeta.getInt("total_level"));
				setUnitSize(rawMeta.getInt("unit_size"));
				setRawWidth(rawMeta.getInt("raw_width"));
				setRawHeight(rawMeta.getInt("raw_height"));
				
				if(mListener != null) {
					mListener.metaFetched(MetaFetchStatus.DONE);
				}
			} catch (JSONException e) {
				if (mListener != null) {
					mListener.metaFetched(MetaFetchStatus.ERROR);
				}
				e.printStackTrace();
			}
		}

	}

}
