package edu.edu.rosehuman.alumbajt.conefinder;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.List;















public class ConeFinderActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    public static final String TAG = "ConeFinder";
    /**
     * References to the UI widgets used in this demo app.
     */
    private TextView mLeftRightLocationTextView, mTopBottomLocationTextView, mSizePercentageTextView;

    /**
     * Constants and variables used by OpenCV4Android. Don't mess with these. ;)
     */
    private org.opencv.samples.colorblobdetect.ColorBlobDetector mDetector;
    private Scalar CONTOUR_COLOR = new Scalar(0, 0, 255, 255);
    private CameraBridgeViewBase mOpenCvCameraView;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    /**
     * Target color. An inside cone has an orange hue around 5 - 15, full saturation and value. (change as needed when outside)
     */
    private static final int TARGET_COLOR_HUE = 10;
    private static final int TARGET_COLOR_SATURATION = 255;
    private static final int TARGET_COLOR_VALUE = 255;

    /**
     * Range of acceptable colors. (change as needed)
     */
    private static final int TARGET_COLOR_HUE_RANGE = 25;
    private static final int TARGET_COLOR_SATURATION_RANGE = 50;
    private static final int TARGET_COLOR_VALUE_RANGE = 50;

    /**
     * Minimum size needed to consider the target a cone. (change as needed)
     */
    private static final double MIN_SIZE_PERCENTAGE = 0.001;

    /**
     * Screen size variables.
     */
    private double mCameraViewWidth;
    private double mCameraViewHeight;
    private double mCameraViewArea;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cone_finder);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mLeftRightLocationTextView = findViewById(R.id.left_right_location_value);
        mTopBottomLocationTextView = findViewById(R.id.top_bottom_location_value);
        mSizePercentageTextView = findViewById(R.id.size_percentage_value);

        mOpenCvCameraView = findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
            Log.d(TAG, "Everything should be fine with using the camera.");
        } else {
            Log.d(TAG, "Requesting permission to use the camera.");
            String[] CAMERA_PERMISSONS = {
                    Manifest.permission.CAMERA
            };
            ActivityCompat.requestPermissions(this, CAMERA_PERMISSONS, 0);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
    }


    /**
     * Displays the blob target info in the text views.
     */
    public void onImageRecComplete(boolean coneFound, double leftRightLocation, double topBottomLocation, double sizePercentage) {
        if (coneFound) {
            mLeftRightLocationTextView.setText(String.format("%.3f", leftRightLocation));
            mTopBottomLocationTextView.setText(String.format("%.3f", topBottomLocation));
            mSizePercentageTextView.setText(String.format("%.5f", sizePercentage));
        } else {
            mLeftRightLocationTextView.setText("---");
            mTopBottomLocationTextView.setText("---");
            mSizePercentageTextView.setText("---");
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mDetector = new org.opencv.samples.colorblobdetect.ColorBlobDetector();
        // Now DONE: Added our stuff.
        // Setup the target color.
        Scalar targetColorHsv = new Scalar(255);
        targetColorHsv.val[0] = TARGET_COLOR_HUE;
        targetColorHsv.val[1] = TARGET_COLOR_SATURATION;
        targetColorHsv.val[2] = TARGET_COLOR_VALUE;
        mDetector.setHsvColor(targetColorHsv);

        // Setup the range of values around the color to accept.
        Scalar colorRangeHsv = new Scalar(255);
        colorRangeHsv.val[0] = TARGET_COLOR_HUE_RANGE;
        colorRangeHsv.val[1] = TARGET_COLOR_SATURATION_RANGE;
        colorRangeHsv.val[2] = TARGET_COLOR_VALUE_RANGE;
        mDetector.setColorRadius(colorRangeHsv);

        // Record the screen size constants
        mCameraViewWidth = (double) width;
        mCameraViewHeight = (double) height;
        mCameraViewArea = mCameraViewWidth * mCameraViewHeight;
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        mDetector.process(rgba);
        List<MatOfPoint> contours = mDetector.getContours(); // For the outline
        Imgproc.drawContours(rgba, contours, -1, CONTOUR_COLOR);

        // Now DONE: Add our stuff.
        // Find the center of the cone.
        double[] coneResult = new double[3];
        final boolean coneFound = findCone(contours, MIN_SIZE_PERCENTAGE, coneResult);
        final double leftRightLocation = coneResult[0]; // -1 for left ...  1 for right
        final double topBottomLocation = coneResult[1]; // 1 for top ... 0 for bottom
        final double sizePercentage = coneResult[2];
        if (coneFound) {
            // Draw a circle on the screen at the center.
            double coneCenterX = topBottomLocation * mCameraViewWidth;
            double coneCenterY = (leftRightLocation + 1.0) / 2.0 * mCameraViewHeight;
            Imgproc.circle(rgba, new Point(coneCenterX, coneCenterY), 5, CONTOUR_COLOR, -1);
        }
        runOnUiThread(new Runnable() {
            public void run() {
                onImageRecComplete(coneFound, leftRightLocation, topBottomLocation, sizePercentage);
            }
        });


        return rgba;
    }

    /**
     * Performs the math to find the leftRightLocation, topBottomLocation, and sizePercentage values.
     *
     * @param contours          List of matrices containing points that match the target color.
     * @param minSizePercentage Minimum size percentage needed to call a blob a match. 0.005 would be 0.5%
     * @param coneResult        Array that will be populated with the results of this math.
     * @return True if a cone is found, False if no cone is found.
     */
    private boolean findCone(List<MatOfPoint> contours, double minSizePercentage, double[] coneResult) {
        // Step #0: Determine if any contour regions were found that match the target color criteria.
        if (contours.size() == 0) {
            return false; // No contours found.
        }

        // Step #1: Use only the largest contour. Other contours (potential other cones) will be ignored.
        MatOfPoint largestContour = contours.get(0);
        double largestArea = Imgproc.contourArea(largestContour);
        for (int i = 1; i < contours.size(); ++i) {
            MatOfPoint currentContour = contours.get(0);
            double currentArea = Imgproc.contourArea(currentContour);
            if (currentArea > largestArea) {
                largestArea = currentArea;
                largestContour = currentContour;
            }
        }

        // Step #2: Determine if this target meets the size requirement.
        double sizePercentage = largestArea / mCameraViewArea;
        if (sizePercentage < minSizePercentage) {
            return false; // No cone found meeting the size requirement.
        }

        // Step #3: Calculate the center of the blob.
//        Moments moments = Imgproc.moments(largestContour, false);
        // yep, the line above fails.  Comment out the line above and uncomment the line below.  For more info visit this page https://github.com/Itseez/opencv/issues/5017
        Moments moments = contourMoments(largestContour);
        double aveX = moments.get_m10() / moments.get_m00();
        double aveY = moments.get_m01() / moments.get_m00();

        // Step #4: Convert the X and Y values into leftRight and topBottom values.
        // X is 0 on the left (which is really the bottom) divide by width to scale the topBottomLocation
        // Y is 0 on the top of the view (object is left of the robot) divide by height to scale
        double leftRightLocation = aveY / (mCameraViewHeight / 2.0) - 1.0;
        double topBottomLocation = aveX / mCameraViewWidth;

        // Step #5: Populate the results array.
        coneResult[0] = leftRightLocation;
        coneResult[1] = topBottomLocation;
        coneResult[2] = sizePercentage;
        return true;
    }

    public Moments contourMoments(MatOfPoint contour) {
        Moments m = new Moments();
        int lpt = contour.checkVector(2);
        boolean is_float = true;//(contour.depth() == CvType.CV_32F);
        Point[] ptsi = contour.toArray();
//PointF[] ptsf = contour.toArray();

        //CV_Assert( contour.depth() == CV_32S || contour.depth() == CV_32F );

        if (lpt == 0)
            return m;

        double a00 = 0, a10 = 0, a01 = 0, a20 = 0, a11 = 0, a02 = 0, a30 = 0, a21 = 0, a12 = 0, a03 = 0;
        double xi, yi, xi2, yi2, xi_1, yi_1, xi_12, yi_12, dxy, xii_1, yii_1;


        {
            xi_1 = ptsi[lpt - 1].x;
            yi_1 = ptsi[lpt - 1].y;
        }

        xi_12 = xi_1 * xi_1;
        yi_12 = yi_1 * yi_1;

        for (int i = 0; i < lpt; i++) {

            {
                xi = ptsi[i].x;
                yi = ptsi[i].y;
            }

            xi2 = xi * xi;
            yi2 = yi * yi;
            dxy = xi_1 * yi - xi * yi_1;
            xii_1 = xi_1 + xi;
            yii_1 = yi_1 + yi;

            a00 += dxy;
            a10 += dxy * xii_1;
            a01 += dxy * yii_1;
            a20 += dxy * (xi_1 * xii_1 + xi2);
            a11 += dxy * (xi_1 * (yii_1 + yi_1) + xi * (yii_1 + yi));
            a02 += dxy * (yi_1 * yii_1 + yi2);
            a30 += dxy * xii_1 * (xi_12 + xi2);
            a03 += dxy * yii_1 * (yi_12 + yi2);
            a21 += dxy * (xi_12 * (3 * yi_1 + yi) + 2 * xi * xi_1 * yii_1 +
                    xi2 * (yi_1 + 3 * yi));
            a12 += dxy * (yi_12 * (3 * xi_1 + xi) + 2 * yi * yi_1 * xii_1 +
                    yi2 * (xi_1 + 3 * xi));
            xi_1 = xi;
            yi_1 = yi;
            xi_12 = xi2;
            yi_12 = yi2;
        }
        float FLT_EPSILON = 1.19209e-07f;
        if (Math.abs(a00) > FLT_EPSILON) {
            double db1_2, db1_6, db1_12, db1_24, db1_20, db1_60;

            if (a00 > 0) {
                db1_2 = 0.5;
                db1_6 = 0.16666666666666666666666666666667;
                db1_12 = 0.083333333333333333333333333333333;
                db1_24 = 0.041666666666666666666666666666667;
                db1_20 = 0.05;
                db1_60 = 0.016666666666666666666666666666667;
            } else {
                db1_2 = -0.5;
                db1_6 = -0.16666666666666666666666666666667;
                db1_12 = -0.083333333333333333333333333333333;
                db1_24 = -0.041666666666666666666666666666667;
                db1_20 = -0.05;
                db1_60 = -0.016666666666666666666666666666667;
            }

            // spatial moments
            m.m00 = a00 * db1_2;
            m.m10 = a10 * db1_6;
            m.m01 = a01 * db1_6;
            m.m20 = a20 * db1_12;
            m.m11 = a11 * db1_24;
            m.m02 = a02 * db1_12;
            m.m30 = a30 * db1_20;
            m.m21 = a21 * db1_60;
            m.m12 = a12 * db1_60;
            m.m03 = a03 * db1_20;

            m.completeState();
        }
        return m;
    }


}
