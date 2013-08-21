package me.voidmain.ui.controls.atily;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

/**
 * Caches the image
 * 
 * @Project aTily
 * @Package me.voidmain.ui.controls.atily
 * @Class ImageCache
 * @author voidmain
 */
public class ImageCache {

	public static void cacheImage(String url, Bitmap bmp) {
		try {
			File file = cacheImageFile(url);
			FileOutputStream out = new FileOutputStream(file);
			bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Bitmap tryCache(String url) {
		File cacheFile = cacheImageFile(url);
		if(!cacheFile.exists()) return null;
		
		return BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
	}

	private static File cacheImageFile(String url) {
		String cacheFileName = hashString(url) + ".png";
		File imgFolder = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		File baseFolder = new File(imgFolder, "ATily");

		if (!baseFolder.exists()) {
			baseFolder.mkdirs();
		}

		return new File(baseFolder, cacheFileName);
	}

	private static String hashString(String str) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		digest.update(str.getBytes());

		byte byteData[] = digest.digest();

		// convert the byte to hex format method 1
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < byteData.length; i++) {
			sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16)
					.substring(1));
		}

		return sb.toString();
	}

}
