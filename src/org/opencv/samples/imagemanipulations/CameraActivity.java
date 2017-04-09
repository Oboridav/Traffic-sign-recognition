package org.opencv.samples.imagemanipulations;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

public class CameraActivity extends Activity implements CvCameraViewListener2 {
	private static final String TAG = "OCVSample::Activity";

	public static final int VIEW_MODE_RGBA = 0;
	public static final int VIEW_MODE_Recognition = 1;

	private MenuItem mItemPreviewRGBA;
	private MenuItem mItemPreviewRecognition;

	private CameraBridgeViewBase mOpenCvCameraView;

	private Mat src;

	public static int viewMode = VIEW_MODE_RGBA;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public CameraActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.camera_activity);

		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera);
		mOpenCvCameraView.setCvCameraViewListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this, mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "called onCreateOptionsMenu");
		mItemPreviewRGBA = menu.add("Preview RGBA");
		mItemPreviewRecognition = menu.add("Recognition");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
		if (item == mItemPreviewRGBA)
			viewMode = VIEW_MODE_RGBA;
		else if (item == mItemPreviewRecognition)
			viewMode = VIEW_MODE_Recognition;
		return true;
	}

	public void onCameraViewStarted(int width, int height) {
		src = new Mat();
	}

	public void onCameraViewStopped() {
		// Explicitly deallocate Mats
		if (src != null)
			src.release();
		src = null;
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		Mat rgba = inputFrame.rgba();
		Size sizeRgba = rgba.size();

		Mat rgbaInnerWindow;

		int rows = (int) sizeRgba.height;
		int cols = (int) sizeRgba.width;

		int left = cols / 8;
		int top = rows / 8;

		int width = cols * 3 / 4;
		int height = rows * 3 / 4;

		switch (CameraActivity.viewMode) {
		case CameraActivity.VIEW_MODE_RGBA:
			break;

		case CameraActivity.VIEW_MODE_Recognition:
			Mat gray1 = inputFrame.gray();
			Mat grayInnerWindow1 = gray1.submat(top, top + height, left, left + width);
			rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);

			rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);

			Imgproc.adaptiveThreshold(grayInnerWindow1, src, 120, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY,
					15, 7);

			// Imgproc.threshold(src, src, 128, 255, Imgproc.THRESH_BINARY |
			// Imgproc.THRESH_OTSU);

			Imgproc.medianBlur(src, src, 3);
			/*
			 * Mat srcClose = new Mat(); Mat srcOpen = new Mat();
			 */
			Imgproc.erode(src, src, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)));
			/*
			 * Imgproc.dilate(src, srcOpen,
			 * Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,
			 * 3))); Core.absdiff(srcClose, srcOpen, src);
			 */

			/*
			 * List<MatOfPoint>contours= new ArrayList<MatOfPoint>();
			 * Imgproc.findContours(src, contours, new Mat(), Imgproc.RETR_LIST,
			 * Imgproc.CHAIN_APPROX_SIMPLE); Mat hierarchy = new Mat(); // find
			 * contours: Imgproc.findContours(src, contours, hierarchy,
			 * Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE); for (int
			 * contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
			 * Imgproc.drawContours(rgbInnerWindow1, contours, contourIdx, new
			 * Scalar(0, 0, 255), -1); }
			 */

			/*
			 * Core.convertScaleAbs(src, src, 10, 0); Imgproc.cvtColor(src,
			 * rgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4);
			 * grayInnerWindow1.release(); rgbaInnerWindow.release();
			 */

			Core.convertScaleAbs(src, src, 10, 0);
			Imgproc.cvtColor(src, rgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4);
			grayInnerWindow1.release();
			rgbaInnerWindow.release();
			break;
		}

		return rgba;
	}
}
