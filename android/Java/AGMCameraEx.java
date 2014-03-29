package ${YYAndroidPackageName};

import android.util.Log;
import java.lang.reflect.Field;
import java.lang.Exception;
import java.io.IOException;
import android.hardware.Camera;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.View;
import android.content.Context;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.io.ByteArrayOutputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.YuvImage;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import ${YYAndroidPackageName}.RunnerActivity;

/**
* Author : Cedric Creusot (twitter: @Calimeraw)
* Date : 26/04/2014
* This is the main file for the extension. It's currently in developement. You can use it if you want for any project you like.
* The Documentation will need to be done, I'll do it later. When this extension will be stabilized.
* Also the minimum Android SDK version is 8.
**/
public class AGMCameraEx
{
	private static final String YOYO_TAG = "yoyo";

	private static CameraPreview mCameraPreview;

	// One way to get the preview system for android.
	// Also, this could change in the future, currently, that working because the yoyorunner look for all the extension and then try to find the Init method.
	// This way it's working, but if they change the behavior, I will need to find another way.
	public static void Init() {
		mCameraPreview = new CameraPreview(RunnerActivity.CurrentActivity);
		RunnerActivity.CurrentActivity.addContentView(mCameraPreview, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	}

	public static double camera_start() {
		mCameraPreview.startPreview();
		return 0;
	}

	public static double camera_stop() {
		mCameraPreview.stopPreview();
		return 0;
	}

	public static double camera_release() {
		mCameraPreview.release();
		return 0;
	}

	public static double camera_get_texture_size() {
		return mCameraPreview.getTextureSize();
	}

	public static double camera_get_image_width() {
		return mCameraPreview.getImageWidth();
	}

	public static double camera_get_image_height() {
		return mCameraPreview.getImageHeight();
	}

	public static double camera_get_uv_top() {
		return 0;
	}

	public static double camera_get_uv_left() {
		return 0;
	}

	public static double camera_get_uv_right() {
		return camera_get_image_width() / camera_get_texture_size();
	}

	public static double camera_get_uv_bottom() {
		return camera_get_image_height() / camera_get_texture_size();
	}

	public static double camera_get_texture() {
		return mCameraPreview.genTexture();
	}

	public static class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
		private SurfaceHolder mHolder;
		private Camera mCamera;
		private int[] mCameraTexture;
		private byte[] mGlCameraFrame;
		private int mFrameSize = 512; // This will be the texture size, currently it's a texture of 512x512
		private int mWidth = 320;
		private int mHeight = 240;
		private int mOrientation = 0;
		private Bitmap mFrameBitmap;


		public CameraPreview(Context context) {
			super(context);
			mCamera = getCameraInstance();

			// Install a SurfaceHolder.Callback so we get notified when the
			// underlying surface is created and destroyed.
			mHolder = getHolder();
			mHolder.addCallback(this);
			// deprecated setting, but required on Android versions prior to 3.0
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		/** A safe way to get an instance of the Camera object. */
		public Camera getCameraInstance(){
			Camera c = null;
			try {
				c = Camera.open(); // attempt to get a Camera instance
			}
			catch (Exception e){
			// Camera is not available (in use or does not exist)
			}
			return c; // returns null if camera is unavailable
		}

		public void startPreview() {
		if (mHolder.getSurface() == null)
			return;
			stopPreview();
			// set preview size and make any resize, rotate or
			// reformatting changes here
			Camera.Parameters params = mCamera.getParameters();
			params.setPreviewSize(mWidth, mHeight);
			mCamera.setParameters(params);
			// start preview with new settings
			try {
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();
				mCamera.setPreviewCallback(this);
				Log.i(YOYO_TAG, "camera_start_preview");
			} catch (Exception e){
				Log.d(YOYO_TAG, "Error starting camera preview: " + e.getMessage());
			}
		}

		public void stopPreview() {
			try {
				Log.i(YOYO_TAG, "camera_stop_preview");
				mCamera.stopPreview();
				mCamera.setPreviewCallback(null);
			} catch (Exception e){
			// ignore: tried to stop a non-existent preview
			}
		}

		public int genTexture() {
			synchronized(this) {				
				int tex = 0;
				if (mCameraTexture == null) {
					mCameraTexture = new int[1];
					mFrameBitmap = Bitmap.createBitmap(mFrameSize, mFrameSize, Bitmap.Config.ARGB_8888);
					GLES20.glGenTextures(1, mCameraTexture, 0);
					tex = mCameraTexture[0];
				
					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
					GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
					GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
				}
				else {
					tex =  mCameraTexture[0];
					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
				}
				// Send the data to the texture
				if (mGlCameraFrame != null) {
					BitmapFactory.Options opt = new BitmapFactory.Options();
					opt.inDither=false;
					opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
					Bitmap bitmap = BitmapFactory.decodeByteArray(mGlCameraFrame, 0, mGlCameraFrame.length, opt);

					GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mFrameBitmap, 0);
					GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap);
					bitmap.recycle();
				}
			}
			return mCameraTexture[0];
		}

		public void release() {
			GLES20.glDeleteTextures(1, mCameraTexture, 0);
			mCamera.release();
			mGlCameraFrame = null;
			mCameraTexture = null;
			System.gc();
		}

		public int getImageWidth() {
			return mWidth;
		}

		public int getImageHeight() {
			return mHeight;
		}

		public int getTextureSize() {
			return mFrameSize;
		}

		public void surfaceCreated(SurfaceHolder holder) { // Do nothing
		}

		public void surfaceDestroyed(SurfaceHolder holder) { // Do nothing
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) { // Do nothing
		}

		public void onPreviewFrame(byte[] yuvs, Camera camera) {
			// We're getting the data from the camera!
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				YuvImage yuvImage = new YuvImage(yuvs, ImageFormat.NV21, mWidth, mHeight, null);
				yuvImage.compressToJpeg(new Rect(0, 0, mWidth, mHeight), 100, out);
				mGlCameraFrame = out.toByteArray();
				yuvImage = null;
				out.close();
				System.gc();					
			} catch (IOException e) {
				Log.e(YOYO_TAG, "Error when getting the image yuv data.", e);
			}
		}
	}
}