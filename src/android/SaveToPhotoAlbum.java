package net.zhaopao.cordova.savetophotoalbum;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.Arrays;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.widget.*;
import android.provider.MediaStore;

/**
 * SaveToPhotoAlbum.java
 *
 * Extended Android implementation of the Base64ToGallery for iOS. Inspirated by
 * StefanoMagrassi's code https://github.com/Nexxa/cordova-base64-to-gallery
 *
 * @author Alejandro Gomez <agommor@gmail.com>
 */
public class SaveToPhotoAlbum extends CordovaPlugin {

	private final static String ALBUM_PATH = Environment.getExternalStorageDirectory() + "/zhaopao/";
	private Bitmap mBitmap;

	// Consts
	public static final String EMPTY_STR = "";

	public static final String JPG_FORMAT = "JPG";
	public static final String PNG_FORMAT = "PNG";

	// actions constants
	public static final String SAVE_BASE64_ACTION = "SaveToPhotoAlbum";
	public static final String REMOVE_IMAGE_ACTION = "removeImageFromLibrary";

	@Override
	public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext)
			throws JSONException {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {
					final String imgname = args.getString(0);
					String filename = imgname.substring(imgname.lastIndexOf("/") + 1);
					getImage(imgname, filename);

					callbackContext.success();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		return true;
	}

	private void getImage(String url, String filename) {
		try {
			String filePath = url;
			
			File dirFile = new File(ALBUM_PATH);
			if (!dirFile.exists()) {
				dirFile.mkdir();
			}

			InputStream is = getImageStream(filePath);
			FileOutputStream fos = null;
			// 创建文件名
			File file = new File(ALBUM_PATH+filename);
			if (!file.exists()) {
				file.createNewFile();
			}
			// 打开一个已存在文件的输出流
			fos = new FileOutputStream(file);
			// 将输入流is写入文件输出流fos中
			int ch = 0;
			try {
				while ((ch = is.read()) != -1) {
					fos.write(ch);
				}
				
				//插入到图库，然后删除
				MediaStore.Images.Media.insertImage(cordova.getActivity().getContentResolver(),
						ALBUM_PATH+filename, filename, filename);
				file.delete();
				
				scanPhoto(file);
			} catch (IOException e1) {
				e1.printStackTrace();
			} finally {
				// 关闭输入流等
				fos.close();
				is.close();
			}

			// 发送消息，通知handler在主线程中更新UI
			// connectHanlder.sendEmptyMessage(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get image from newwork
	 * 
	 * @param path
	 *            The path of image
	 * @return InputStream
	 * @throws Exception
	 */
	public InputStream getImageStream(String path) throws Exception {
		URL url = new URL(path);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(5 * 1000);
		conn.setRequestMethod("GET");
		if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
			return conn.getInputStream();
		}
		return null;
	}

	/**
	 * Invoke the system's media scanner to add your photo to the Media
	 * Provider's database, making it available in the Android Gallery
	 * application and to other apps.
	 */
	private void scanPhoto(File imageFile) {
		Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		Uri contentUri = Uri.fromFile(imageFile);
		
		Log.e("net.zhaopao.app", "contentUri: " + contentUri.getPath());

		mediaScanIntent.setData(contentUri);

		cordova.getActivity().sendBroadcast(mediaScanIntent);
	}
}
