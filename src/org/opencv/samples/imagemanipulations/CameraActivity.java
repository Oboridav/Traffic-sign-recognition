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
		Mat grayInnerWindow1;
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
			
			rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);
			
			ArrayList<Mat> channels = new ArrayList<Mat>(3);

			Core.split(rgbaInnerWindow, channels);

			Mat r = channels.get(0);
			Mat g = channels.get(1);
			Mat b = channels.get(2);

			/*
			channels.set(0, r);
			channels.set(1, g);
			channels.set(2, b);
			*/

			Imgproc.threshold(r, r, 90, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
			Imgproc.threshold(g, g, 0, 70, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
			Imgproc.threshold(b, b, 0, 70, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

			Mat src1 = rgbaInnerWindow;

			Core.merge(channels, src1);

			Imgproc.medianBlur(src1, src1, 5);

			Imgproc.cvtColor(src1, src1, Imgproc.COLOR_RGB2GRAY);
			
			Imgproc.threshold(src1, src1, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
			
			List<MatOfPoint>contours= new ArrayList<MatOfPoint>();
		    Mat hierarchy = new Mat();
		    
		    Imgproc.findContours(src1, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
		
		    for ( int contourIdx=0; contourIdx < contours.size(); contourIdx++ )
		    {
		        // Minimum size allowed for consideration
		        MatOfPoint2f approxCurve = new MatOfPoint2f();
		        MatOfPoint2f contour2f = new MatOfPoint2f( contours.get(contourIdx).toArray() );
		        //Processing on mMOP2f1 which is in type MatOfPoint2f
		        double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
		        Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

		        //Convert back to MatOfPoint
		        MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

		        // Get bounding rect of contour
		        Rect rect = Imgproc.boundingRect(points);

		        Core.rectangle(src1, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0, 255), 3);
			
		    }
			
			Core.convertScaleAbs(src1, rgbaInnerWindow, 10, 0);
			//Imgproc.cvtColor(src1, rgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4);
		
			//grayInnerWindow1.release();
			rgbaInnerWindow.release();
			break;
		}

		return rgba;
	}
}
