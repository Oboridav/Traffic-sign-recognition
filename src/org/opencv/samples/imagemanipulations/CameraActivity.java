package org.opencv.samples.imagemanipulations;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
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

	// Initializing OpenCV

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

	/* Called when the activity is first created. */
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
		// setting up OpenCV version - IMPORTANT to be CORRECT!
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

	// Menu activated with Android "more options" button
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
		if (item == mItemPreviewRGBA)
			viewMode = VIEW_MODE_RGBA;
		else if (item == mItemPreviewRecognition)
			viewMode = VIEW_MODE_Recognition;
		return true;
	}

	// initialize image
	public void onCameraViewStarted(int width, int height) {
		src = new Mat();
	}

	// Explicitly deallocate Mats
	public void onCameraViewStopped() {
		if (src != null)
			src.release();
		src = null;
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

		Mat rgba = inputFrame.rgba();
		Mat gray1 = inputFrame.gray();

		ArrayList<Mat> channels = new ArrayList<Mat>(3);

		Mat src1 = Mat.zeros(rgba.size(), CvType.CV_8UC3);
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();

		double ratio = 0;
		// Mat crop = Mat.zeros(rgbaInnerWindow.size(),CvType.CV_8UC1);

		switch (CameraActivity.viewMode) {
		// if RGBA mode is selected no algorithm needs to run
		case CameraActivity.VIEW_MODE_RGBA:
			break;
		case CameraActivity.VIEW_MODE_Recognition:
			// split the RGB image into R,G,B channels respectively
			Core.split(rgba, channels);
			// get only the spectrum of colors (threshold) that could be in a
			// sign
			Mat b = channels.get(0);
			Imgproc.threshold(b, b, 90, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
			Mat g = channels.get(1);
			Imgproc.threshold(g, g, 0, 70, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
			Mat r = channels.get(2);
			Imgproc.threshold(r, r, 0, 70, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
			// put back thresholded channels into one RGB image
			Core.merge(channels, src1);
			// get rid of noise
			Imgproc.medianBlur(src1, src1, 3);
			// create gray image in order to further threshold the result
			Imgproc.cvtColor(src1, gray1, Imgproc.COLOR_BGR2GRAY);

			Imgproc.threshold(gray1, gray1, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
			// find the region of interest - traffic signs by drawing lines
			// (contours) around it
			Imgproc.findContours(gray1, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

			for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
				// Minimum size allowed for consideration
				MatOfPoint2f approxCurve = new MatOfPoint2f();
				MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(contourIdx).toArray());
				// Processing on mMOP2f1 which is in type MatOfPoint2f
				double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;// 0.02
				Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

				// Convert back to MatOfPoint
				MatOfPoint points = new MatOfPoint(approxCurve.toArray());
				// Get bounding rect of contour
				Rect rect = Imgproc.boundingRect(points);
				double a = Imgproc.contourArea(contours.get(contourIdx));

				ratio = (double) rect.height / (double) rect.width;
				// set maximum size and ratio of rect
				if (a > 2000 && a < 8000 && ratio > 0.8 && ratio < 1.2) {
					Core.rectangle(rgba, new Point(rect.x, rect.y),
							new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255), 3);
				}
			}
			break;
		}
		return rgba;
	}
}
