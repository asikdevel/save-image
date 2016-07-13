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
	private String mFileName;

	// Consts
	public static final String EMPTY_STR = "";

	public static final String JPG_FORMAT = "JPG";
	public static final String PNG_FORMAT = "PNG";

	// actions constants
	public static final String SAVE_BASE64_ACTION = "SaveToPhotoAlbum";
	public static final String REMOVE_IMAGE_ACTION = "removeImageFromLibrary";

	@Override
	public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {
					final String imgname = args.getString(0);
					String filename = imgname.substring(imgname.lastIndexOf("/") + 1);
					getImage(imgname, filename);
					
					MediaStore.Images.Media.insertImage(cordova.getActivity().getContentResolver(), mBitmap, filename, filename);

//					saveFile(mBitmap, filename);
					
					callbackContext.success();
//				} catch (IOException e1) {
//					e1.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		return true;
	}

	private Runnable saveFileRunnable = new Runnable() {
		@Override
		public void run() {
			try {
				savePhoto(mBitmap, "zhaopao_", "JPG", 100);
				// saveFile(mBitmap, mFileName);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	};

	/**
	 * 保存文件
	 * 
	 * @param bm
	 * @param fileName
	 * @throws IOException
	 */
	public void saveFile(Bitmap bm, String fileName) throws IOException {
		File dirFile = new File(ALBUM_PATH);
		if (!dirFile.exists()) {
			dirFile.mkdir();
		}
		File myCaptureFile = new File(ALBUM_PATH + fileName);
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
		bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
		bos.flush();
		bos.close();
		
		scanPhoto(myCaptureFile);
	}

	private void getImage(String url, String filename) {
		try {
			String filePath = url;
			mFileName = filename;

			// 以下是取得图片的两种方法
			//////////////// 方法1：取得的是byte数组, 从byte数组生成bitmap
			// byte[] data = getImage(filePath);
			// if (data != null) {
			// mBitmap = BitmapFactory.decodeByteArray(data, 0,
			// data.length);// bitmap
			// } else {
			// }
			////////////////////////////////////////////////////////

			// ******** 方法2：取得的是InputStream，直接从InputStream生成bitmap
			// ***********/
			mBitmap = BitmapFactory.decodeStream(getImageStream(filePath));
			// ********************************************************************/

			// 发送消息，通知handler在主线程中更新UI
			// connectHanlder.sendEmptyMessage(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * 连接网络 由于在4.0中不允许在主线程中访问网络，所以需要在子线程中访问
	 */
	private Runnable connectNet = new Runnable() {
		@Override
		public void run() {
			try {
				String filePath = "http://img.my.csdn.net/uploads/201211/21/1353511891_4579.jpg";
				mFileName = "test.jpg";

				// 以下是取得图片的两种方法
				//////////////// 方法1：取得的是byte数组, 从byte数组生成bitmap
				// byte[] data = getImage(filePath);
				// if (data != null) {
				// mBitmap = BitmapFactory.decodeByteArray(data, 0,
				// data.length);// bitmap
				// } else {
				// }
				////////////////////////////////////////////////////////

				// ******** 方法2：取得的是InputStream，直接从InputStream生成bitmap
				// ***********/
				mBitmap = BitmapFactory.decodeStream(getImageStream(filePath));
				// ********************************************************************/

				// 发送消息，通知handler在主线程中更新UI
				// connectHanlder.sendEmptyMessage(0);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	};

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
	 * It deletes an image from the given path.
	 */
	private void removeImage(JSONArray args, CallbackContext callbackContext) throws JSONException {
		String filename = args.optString(0);

		// isEmpty() requires API level 9
		if (filename.equals(EMPTY_STR)) {
			callbackContext.error("Missing filename string");
		}

		File file = new File(filename);
		if (file.exists()) {
			try {
				file.delete();
			} catch (Exception ex) {
				callbackContext.error(ex.getMessage());
			}
		}

		callbackContext.success(filename);

	}

	/**
	 * It saves a Base64 String into an image.
	 */
	private void saveBase64Image(JSONArray args, CallbackContext callbackContext) throws JSONException {
		String base64 = args.optString(0);
		String filePrefix = args.optString(1);
		boolean mediaScannerEnabled = args.optBoolean(2);
		String format = args.optString(3);
		int quality = args.optInt(4);

		List<String> allowedFormats = Arrays.asList(new String[] { JPG_FORMAT, PNG_FORMAT });

		// isEmpty() requires API level 9
		if (base64.equals(EMPTY_STR)) {
			callbackContext.error("Missing base64 string");
		}

		// isEmpty() requires API level 9
		if (format.equals(EMPTY_STR) || !allowedFormats.contains(format.toUpperCase())) {
			format = JPG_FORMAT;
		}

		if (quality <= 0) {
			quality = 100;
		}

		// Create the bitmap from the base64 string
		byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
		Bitmap bmp = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

		if (bmp == null) {
			callbackContext.error("The image could not be decoded");

		} else {

			// Save the image
			File imageFile = savePhoto(bmp, filePrefix, format, quality);

			if (imageFile == null) {
				callbackContext.error("Error while saving image");
			}

			// Update image gallery
			if (mediaScannerEnabled) {
				scanPhoto(imageFile);
			}

			String path = imageFile.toString();

			if (!path.startsWith("file://")) {
				path = "file://" + path;
			}

			callbackContext.success(path);
		}
	}

	/**
	 * Private method to save a {@link Bitmap} into the photo library/temp
	 * folder with a format, a prefix and with the given quality.
	 */
	private File savePhoto(Bitmap bmp, String prefix, String format, int quality) {
		// File retVal = null;
		File retVal = null;

		try {
			String deviceVersion = Build.VERSION.RELEASE;
			Calendar c = Calendar.getInstance();
			String date = EMPTY_STR + c.get(Calendar.YEAR) + c.get(Calendar.MONTH) + c.get(Calendar.DAY_OF_MONTH)
					+ c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE) + c.get(Calendar.SECOND);

			int check = deviceVersion.compareTo("2.3.3");

			File folder;

			/*
			 * File path = Environment.getExternalStoragePublicDirectory(
			 * Environment.DIRECTORY_PICTURES ); //this throws error in Android
			 * 2.2
			 */
			if (check >= 1) {
				folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

			} else {
				folder = Environment.getExternalStorageDirectory();
			}

			if (!folder.exists()) {
				folder.mkdirs();
			}

			// building the filename
			String fileName = prefix + date;
			Bitmap.CompressFormat compressFormat = null;
			// switch for String is not valid for java < 1.6, so we avoid it
			if (format.equalsIgnoreCase(JPG_FORMAT)) {
				fileName += ".jpeg";
				compressFormat = Bitmap.CompressFormat.JPEG;
			} else if (format.equalsIgnoreCase(PNG_FORMAT)) {
				fileName += ".png";
				compressFormat = Bitmap.CompressFormat.PNG;
			} else {
				// default case
				fileName += ".jpeg";
				compressFormat = Bitmap.CompressFormat.JPEG;
			}

			// now we create the image in the folder
			File imageFile = new File(folder, fileName);
			FileOutputStream out = new FileOutputStream(folder + "/" + fileName);
			// compress it
			bmp.compress(compressFormat, quality, out);
			out.flush();
			out.close();

			// retVal = folder+"/"+fileName;
			retVal = imageFile;

		} catch (Exception e) {
			Log.e("SaveToPhotoAlbum", "An exception occured while saving image: " + e.toString());
		}

		return retVal;
	}

	/**
	 * Invoke the system's media scanner to add your photo to the Media
	 * Provider's database, making it available in the Android Gallery
	 * application and to other apps.
	 */
	private void scanPhoto(File imageFile) {
		Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		Uri contentUri = Uri.fromFile(imageFile);

		mediaScanIntent.setData(contentUri);

		cordova.getActivity().sendBroadcast(mediaScanIntent);
	}
}
