package me.voidmain.ui.controls.atily;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**   
 * Represents a tile on the screen.
 * 
 * @Project aTily
 * @Package me.voidmain.ui.controls.atily
 * @Class Tile
 * @author voidmain
 */
public class Tile {
	private Bitmap mTileImage = null;
	private String mQuadKey = null;
	private int    mTileX = 0;
	private int    mTileY = 0;
	private int    mLevel = 0;
	private TileSystemMeta mTSMeta = null;
	
	public int getLevel() {
		return mLevel;
	}
	
	public String getQuadKey() {
		return mQuadKey;
	}
	
	public Tile(String quadKey, TileSystemMeta meta) {
		mTSMeta = meta;
		
		mQuadKey = quadKey;
		int[] result = quadKeyToTileXY(quadKey);
		mLevel = result[0];
		mTileX = result[1];
		mTileY = result[2];
	}
	
	public void setBitmap(Bitmap bmp) {
		mTileImage = bmp.copy(Bitmap.Config.ARGB_8888, true);
		bmp.recycle();
	}
	
	public void draw(Canvas canvas, RectF viewportRect, float curLevel, Paint bitmapPaint) {
		if(mTileImage == null) return;
		
		float scaleFactor = calculateScaleFactor(curLevel);
		float scaledUnitSize = mTSMeta.getUnitSize() * scaleFactor;
		float worldX = mTileX * scaledUnitSize;
		float worldY = mTileY * scaledUnitSize;
		RectF dest = new RectF(worldX - viewportRect.left, worldY
				- viewportRect.top, worldX - viewportRect.left
				+ scaledUnitSize, worldY - viewportRect.top + scaledUnitSize);

		// Draws the image
		canvas.drawBitmap(mTileImage, null, dest, bitmapPaint);
	}
	
	public void dispose() {
		if(mTileImage != null) {
			mTileImage.recycle();
		}
	}
	
	private float calculateScaleFactor(float curLevel) {
		int level = (int)Math.floor(curLevel);
		return (float)Math.pow(2, (curLevel + mLevel) - 2 * level);
	}
	
	private int[] quadKeyToTileXY(String quadKey) {
		int[] result = new int[3];
		int tileX = 0;
		int tileY = 0;
		int levelOfDetail = quadKey.length();
		for (int i = levelOfDetail; i > 0; i--) {
			int mask = 1 << (i - 1);
			switch (quadKey.charAt(levelOfDetail - i)) {
			case '0':
				break;

			case '1':
				tileX |= mask;
				break;

			case '2':
				tileY |= mask;
				break;

			case '3':
				tileX |= mask;
				tileY |= mask;
				break;

			default:
				break;
			}
		}
		result[0] = levelOfDetail;
		result[1] = tileX;
		result[2] = tileY;

		return result;
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof Tile) {
			Tile tile = (Tile)o;
			return tile.getQuadKey().equals(this.getQuadKey());
		}
		return false;
	}
}
