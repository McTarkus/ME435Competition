package edu.rosehulman.alumbajt.integratedimagerec;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;


import edu.rosehulman.me435.NavUtils;
import edu.rosehulman.me435.RobotActivity;

public class GolfBallDeliveryActivity extends ImageRecActivity {

    /**
     * Constant used with logging that you'll see later.
     */
    public static final String TAG = "GolfBallDelivery";
    protected long mFirebaseUpdateCounter;

    public enum State {
        READY_FOR_MISSION,
        NEAR_BALL_SCRIPT,
        DRIVE_TOWARDS_FAR_BALL,
        FAR_BALL_SCRIPT,
        DRIVE_TOWARD_HOME,
        WAITING_FOR_PICKUP,
        SEEKING_HOME
    }

    public State mState;

    /**
     * An enum used for variables when a ball color needs to be referenced.
     */
    public enum BallColor {
        NONE, BLUE, RED, YELLOW, GREEN, BLACK, WHITE
    }

    /**
     * An array (of size 3) that stores what color is present in each golf ball stand location.
     */
    public BallColor[] mLocationColors = new BallColor[]{BallColor.NONE, BallColor.NONE, BallColor.NONE};

    /**
     * Simple boolean that is updated when the Team button is pressed to switch teams.
     */
    public boolean mOnRedTeam = false;


    // ---------------------- UI References ----------------------
    /**
     * An array (of size 3) that keeps a reference to the 3 balls displayed on the UI.
     */
    private ImageButton[] mBallImageButtons;

    /**
     * References to the buttons on the UI that can change color.
     */
    private Button mTeamChangeButton, mGoOrMissionCompleteButton, mGoOrMissionCompleteButtonJumbo;

    /**
     * An array constants (of size 7) that keeps a reference to the different ball color images resources.
     */
    // Note, the order is important and must be the same throughout the app.
    private static final int[] BALL_DRAWABLE_RESOURCES = new int[]{R.drawable.none_ball, R.drawable.blue_ball,
            R.drawable.red_ball, R.drawable.yellow_ball, R.drawable.green_ball, R.drawable.black_ball, R.drawable.white_ball};

    /**
     * TextViews that can change values.
     */
    private TextView mCurrentStateTextView, mStateTimeTextView, mGpsInfoTextView, mSensorOrientationTextView,
            mGuessXYTextView, mLeftDutyCycleTextView, mRightDutyCycleTextView, mMatchTimeTextView;

    private TextView mJumboXTextView, mJumboYTextView;



    protected LinearLayout mBackgroundJumbo;

    // ---------------------- End of UI References ----------------------


    // ---------------------- Mission strategy values ----------------------
    /**
     * Constants for the known locations.
     */
    public static final long NEAR_BALL_GPS_X = 90;
    public static final long FAR_BALL_GPS_X = 240;


    /**
     * Variables that will be either 50 or -50 depending on the balls we get.
     */
    private double mNearBallGpsY, mFarBallGpsY;

    /**
     * If that ball is present the values will be 1, 2, or 3.
     * If not present the value will be 0.
     * For example if we have the black ball, then mWhiteBallLocation will equal 0.
     */
    public int mNearBallLocation, mFarBallLocation, mWhiteBallLocation;
    // ----------------- End of mission strategy values ----------------------

    private Scripts mScripts;
    // ---------------------------- Timing area ------------------------------
    /**
     * Time when the state began (saved as the number of millisecond since epoch).
     */
    private long mStateStartTime;

    /**
     * Time when the match began, ie when Go! was pressed (saved as the number of millisecond since epoch).
     */
    private long mMatchStartTime;

    /**
     * Constant that holds the maximum length of the match (saved in milliseconds).
     */
    private long MATCH_LENGTH_MS = 300000; // 5 minutes in milliseconds (5 * 60 * 1000)
    // ----------------------- End of timing area --------------------------------


    // ---------------------------- Driving area ---------------------------------
    /**
     * When driving towards a target, using a seek strategy, consider that state a success when the
     * GPS distance to the target is less than (or equal to) this value.
     */
    public static final double ACCEPTED_DISTANCE_AWAY_FT = 10.0; // Within 10 feet is close enough.

    /**
     * Multiplier used during seeking to calculate a PWM value based on the turn amount needed.
     */
    private static final double SEEKING_DUTY_CYCLE_PER_ANGLE_OFF_MULTIPLIER = 3.0;  // units are (PWM value)/degrees

    /**
     * Variable used to cap the slowest PWM duty cycle used while seeking. Pick a value from -255 to 255.
     */
    private static final int LOWEST_DESIRABLE_SEEKING_DUTY_CYCLE = 150;

    /**
     * PWM duty cycle values used with the drive straight dialog that make your robot drive straightest.
     */
    public int mLeftStraightPwmValue = 255, mRightStraightPwmValue = 255;
    // ------------------------ End of Driving area ------------------------------


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mBallImageButtons = new ImageButton[]{(ImageButton) findViewById(R.id.location_1_image_button),
                (ImageButton) findViewById(R.id.location_2_image_button),
                (ImageButton) findViewById(R.id.location_3_image_button)};
        mTeamChangeButton = (Button) findViewById(R.id.team_change_button);
        mCurrentStateTextView = (TextView) findViewById(R.id.current_state_textview);
        mStateTimeTextView = (TextView) findViewById(R.id.state_time_textview);
        mGpsInfoTextView = (TextView) findViewById(R.id.gps_info_textview);
        mSensorOrientationTextView = (TextView) findViewById(R.id.orientation_textview);
        mGuessXYTextView = (TextView) findViewById(R.id.guess_location_textview);
        mLeftDutyCycleTextView = (TextView) findViewById(R.id.left_duty_cycle_textview);
        mRightDutyCycleTextView = (TextView) findViewById(R.id.right_duty_cycle_textview);
        mMatchTimeTextView = (TextView) findViewById(R.id.match_time_textview);
        mGoOrMissionCompleteButton = (Button) findViewById(R.id.go_or_mission_complete_button);
        mGoOrMissionCompleteButtonJumbo = (Button)findViewById(R.id.go_or_mission_complete_button_jumbo);
        mJumboXTextView = (TextView) findViewById(R.id.jumbo_x);
        mJumboYTextView = (TextView) findViewById(R.id.jumbo_y);
        mBackgroundJumbo = findViewById(R.id.background_jumbo);

        mViewFlipper = (ViewFlipper) findViewById(R.id.my_view_flipper);

        // When you start using the real hardware you don't need test buttons.
        boolean hideFakeGpsButtons = false;
        if (hideFakeGpsButtons) {
            TableLayout fakeGpsButtonTable = (TableLayout) findViewById(R.id.fake_gps_button_table);
            fakeGpsButtonTable.setVisibility(View.GONE);
        }
        mScripts = new Scripts(this);
        setState(State.READY_FOR_MISSION);
        setLocationToColor(1, BallColor.RED);
        setLocationToColor(2, BallColor.WHITE);
        setLocationToColor(3, BallColor.BLUE);
    }

    /**
     * Use this helper method to set the color of a ball.
     * The location value here is 1 based.  Send 1, 2, or 3
     * Side effect: Updates the UI with the appropriate ball color resource image.
     */
    public void setLocationToColor(int location, BallColor ballColor) {
        mBallImageButtons[location - 1].setImageResource(BALL_DRAWABLE_RESOURCES[ballColor.ordinal()]);
        mLocationColors[location - 1] = ballColor;
    }

    /**
     * Used to get the state time in milliseconds.
     */
    private long getStateTimeMs() {
        return System.currentTimeMillis() - mStateStartTime;
    }

    /**
     * Used to get the match time in milliseconds.
     */
    private long getMatchTimeMs() {
        return System.currentTimeMillis() - mMatchStartTime;
    }


    // --------------------------- Methods added ---------------------------

    @Override
    public void loop() {
        super.loop();
        mStateTimeTextView.setText("" + getStateTimeMs()/1000);
        mGuessXYTextView.setText("(" + (int)mGuessX + ", " + (int)mGuessY + ")");

        mJumboYTextView.setText(""+(int)mGuessX);
        mJumboXTextView.setText(""+(int)mGuessY);

        // Match timer.
        long timeRemainingSeconds = MATCH_LENGTH_MS / 1000;
        if (mState != State.READY_FOR_MISSION) {
            timeRemainingSeconds = (MATCH_LENGTH_MS - getMatchTimeMs()) / 1000;
            if (getMatchTimeMs() > MATCH_LENGTH_MS) {
                setState(State.READY_FOR_MISSION);
            }
        }
        String matchTime = getString(R.string.time_format, timeRemainingSeconds / 60, timeRemainingSeconds % 60);
        mMatchTimeTextView.setText(matchTime);
        //Once every 2 seconds (20 calls to this function) send the match and state times to Firebase
        mFirebaseUpdateCounter++;

        if (mFirebaseUpdateCounter%20 == 0 && mState != State.READY_FOR_MISSION){
            //send the match time
            mFirebaseRef.child("time").child("matchTime").setValue(matchTime);

            //send the match time
            mFirebaseRef.child("time").child("stateTime").setValue(getStateTimeMs()/1000);
        }

        switch (mState) {

            case READY_FOR_MISSION:
                break;
            case NEAR_BALL_SCRIPT:
                break;
            case DRIVE_TOWARDS_FAR_BALL:
                seekTargetAt(FAR_BALL_GPS_X, mFarBallGpsY);
                break;
            case FAR_BALL_SCRIPT:
                break;
            case DRIVE_TOWARD_HOME:
                seekTargetAt(0,0);
                break;
            case WAITING_FOR_PICKUP:
                if (getStateTimeMs()>8000){
                    setState(State.SEEKING_HOME);
                }
                break;
            case SEEKING_HOME:
                seekTargetAt(0,0);
                if (getStateTimeMs()>8000){
                    setState(State.SEEKING_HOME);
                }
                break;
        }

        if (mConeFound){
            if (mConeLeftRightLocation < 0){

            }
            if (mConeSize > 0.1) {
                mBackgroundJumbo.setBackgroundColor(Color.parseColor("#ff8000"));
            }
            else {

                mBackgroundJumbo.setBackgroundColor(Color.GRAY);
            }
        }
    }

    private void seekTargetAt(double x, double y) {
//TODO: the right thing
        sendWheelSpeed((int)x,(int)y);
    }

    // --------------------------- Drive command ---------------------------

    @Override
    public void sendWheelSpeed(int leftDutyCycle, int rightDutyCycle) {
        super.sendWheelSpeed(leftDutyCycle, rightDutyCycle); // Send the values to the
        mLeftDutyCycleTextView.setText("Left\n" + leftDutyCycle);
        mRightDutyCycleTextView.setText("Right\n" + rightDutyCycle);
    }



    // --------------------------- Sensor listeners ---------------------------

    @Override
    public void onLocationChanged(double x, double y, double heading, Location location) {
        super.onLocationChanged(x, y, heading, location);

        mFirebaseRef.child("gps").child("x").setValue((int)mCurrentGpsY);
        mFirebaseRef.child("gps").child("y").setValue((int)mCurrentGpsY);

        String gpsInfo = getString(R.string.xy_format, mCurrentGpsX, mCurrentGpsY);
        if (mCurrentGpsHeading != NO_HEADING) {
            gpsInfo += " " + getString(R.string.degrees_format, mCurrentGpsHeading);
            mBackgroundJumbo.setBackgroundColor(Color.GRAY);
            mFirebaseRef.child("gps").child("heading").setValue((int)mCurrentGpsHeading);
        } else {
            mBackgroundJumbo.setBackgroundColor(Color.GREEN);
            mFirebaseRef.child("gps").child("heading").setValue("No Heading");
            gpsInfo += " ?°";
        }
        gpsInfo += "   " + mGpsCounter;
        mGpsInfoTextView.setText(gpsInfo);

        if (mState == State.DRIVE_TOWARDS_FAR_BALL) {
            double distanceFromTarget = NavUtils.getDistance(mCurrentGpsX, mCurrentGpsY, FAR_BALL_GPS_X, mFarBallGpsY);
            if (distanceFromTarget < ACCEPTED_DISTANCE_AWAY_FT) {
                setState(State.FAR_BALL_SCRIPT);
            }
        }

        if (mState == State.DRIVE_TOWARD_HOME) {
            double distanceFromTarget = NavUtils.getDistance(mCurrentGpsX, mCurrentGpsY, 0,0);
            if (distanceFromTarget < ACCEPTED_DISTANCE_AWAY_FT) {
                setState(State.WAITING_FOR_PICKUP);
            }
        }

    }


    // --------------------------- Button Handlers ----------------------------

    /**
     * Helper method that is called by all three golf ball clicks.
     */
    private void handleBallClickForLocation(final int location) {
        AlertDialog.Builder builder = new AlertDialog.Builder(GolfBallDeliveryActivity.this);
        builder.setTitle("What was the real color?").setItems(R.array.ball_colors,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        GolfBallDeliveryActivity.this.setLocationToColor(location, BallColor.values()[which]);
                    }
                });
        builder.create().show();
    }

    /**
     * Click to the far left image button (Location 1).
     */
    public void handleBallAtLocation1Click(View view) {
        handleBallClickForLocation(1);
    }

    /**
     * Click to the center image button (Location 2).
     */
    public void handleBallAtLocation2Click(View view) {
        handleBallClickForLocation(2);
    }

    /**
     * Click to the far right image button (Location 3).
     */
    public void handleBallAtLocation3Click(View view) {
        handleBallClickForLocation(3);
    }

    /**
     * Sets the mOnRedTeam boolean value as appropriate
     * Side effects: Clears the balls
     *
     * @param view
     */
    public void handleTeamChange(View view) {
        setLocationToColor(1, BallColor.NONE);
        setLocationToColor(2, BallColor.NONE);
        setLocationToColor(3, BallColor.NONE);
        if (mOnRedTeam) {
            mOnRedTeam = false;
            mTeamChangeButton.setBackgroundResource(R.drawable.blue_button);
            mTeamChangeButton.setText("Team Blue");
        } else {
            mOnRedTeam = true;
            mTeamChangeButton.setBackgroundResource(R.drawable.red_button);
            mTeamChangeButton.setText("Team Red");
        }
        // setTeamToRed(mOnRedTeam); // This call is optional. It will reset your GPS and sensor heading values.
    }

    /**
     * Sends a message to Arduino to perform a ball color test.
     */
    public void handlePerformBallTest(View view) {
//        sendCommand();

        onCommandReceived("1R");
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                onCommandReceived("2W");
            }
        }, 1000);
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                onCommandReceived("3B");
            }
        }, 2000);
    }


    AlertDialog alert;

    /**
     * Clicks to the red arrow image button that should show a dialog window.
     */
    public void handleDrivingStraight(View view) {
        Toast.makeText(this, "handleDrivingStraight", Toast.LENGTH_SHORT).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(GolfBallDeliveryActivity.this);
        builder.setTitle("Driving Straight Calibration");
        View dialoglayout = getLayoutInflater().inflate(R.layout.driving_straight_dialog, (ViewGroup) getCurrentFocus());
        builder.setView(dialoglayout);
        final NumberPicker rightDutyCyclePicker = (NumberPicker) dialoglayout.findViewById(R.id.right_pwm_number_picker);
        rightDutyCyclePicker.setMaxValue(255);
        rightDutyCyclePicker.setMinValue(0);
        rightDutyCyclePicker.setValue(mRightStraightPwmValue);
        rightDutyCyclePicker.setWrapSelectorWheel(false);
        final NumberPicker leftDutyCyclePicker = (NumberPicker) dialoglayout.findViewById(R.id.left_pwm_number_picker);
        leftDutyCyclePicker.setMaxValue(255);
        leftDutyCyclePicker.setMinValue(0);
        leftDutyCyclePicker.setValue(mLeftStraightPwmValue);
        leftDutyCyclePicker.setWrapSelectorWheel(false);
        Button doneButton = (Button) dialoglayout.findViewById(R.id.done_button);
        doneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mLeftStraightPwmValue = leftDutyCyclePicker.getValue();
                mRightStraightPwmValue = rightDutyCyclePicker.getValue();
                alert.dismiss();
            }
        });
        final Button testStraightButton = (Button) dialoglayout.findViewById(R.id.test_straight_button);
        testStraightButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mLeftStraightPwmValue = leftDutyCyclePicker.getValue();
                mRightStraightPwmValue = rightDutyCyclePicker.getValue();
                Toast.makeText(GolfBallDeliveryActivity.this, "TODO: Implement the drive straight test", Toast.LENGTH_SHORT).show();
                mScripts.testStraightScript();
            }
        });
        alert = builder.create();
        alert.show();
    }

    /**
     * Test GPS point when going to the Far ball (assumes Blue Team heading to red ball).
     */
    public void handleFakeGpsF0(View view) {
        onLocationChanged(165, 50, NO_HEADING, null); // Midfield
    }

    public void handleFakeGpsF1(View view) {
        onLocationChanged(209, 50, 0, null);  // Out of range so ignored.
    }

    public void handleFakeGpsF2(View view) {
        onLocationChanged(231, 50, 135, null);  // Within range (terrible heading)
    }

    public void handleFakeGpsF3(View view) {
        onLocationChanged(240, 41, 35, null);  // Within range
    }

    public void handleFakeGpsH0(View view) {
        onLocationChanged(165, 0, -180, null); // Midfield
    }

    public void handleFakeGpsH1(View view) {
        onLocationChanged(11, 0, -180, null);  // Out of range so ignored.
    }

    public void handleFakeGpsH2(View view) {
        onLocationChanged(9, 0, -170, null);  // Within range
    }

    public void handleFakeGpsH3(View view) {
        onLocationChanged(0, -9, -170, null);  // Within range
    }


    public void handleSetOrigin(View view) {
        mFieldGps.setCurrentLocationAsOrigin();
        sendMessage("Setting Origin");
    }

    public void handleSetXAxis(View view) {
        mFieldGps.setCurrentLocationAsLocationOnXAxis();
    }

    public void handleZeroHeading(View view) {
        mFieldOrientation.setCurrentFieldHeading(0);
    }

    public void handleGoOrMissionComplete(View view) {
        if (mState == State.READY_FOR_MISSION) {
            mMatchStartTime = System.currentTimeMillis();
            updateMissionStrategyVariables();
            mGoOrMissionCompleteButton.setBackgroundResource(R.drawable.red_button);
            mGoOrMissionCompleteButton.setText("Mission Complete!");
            mGoOrMissionCompleteButtonJumbo.setBackgroundResource(R.drawable.red_button);
            mGoOrMissionCompleteButtonJumbo.setText("Stop!");
            setState(State.NEAR_BALL_SCRIPT);
        } else {
            setState(State.READY_FOR_MISSION);
        }
    }

    @Override
    protected void onCommandReceived(String receivedCommand) {
        super.onCommandReceived(receivedCommand);

        if (receivedCommand.equalsIgnoreCase("1R")) {
            setLocationToColor(1, BallColor.RED);
        } else if (receivedCommand.equalsIgnoreCase("2W")) {
            setLocationToColor(2, BallColor.WHITE);
        } else if (receivedCommand.equalsIgnoreCase("3B")) {
            setLocationToColor(3, BallColor.BLUE);
        }
    }

    @Override
    public void onSensorChanged(double fieldHeading, float[] orientationValues) {
        super.onSensorChanged(fieldHeading, orientationValues);
        mSensorOrientationTextView.setText(getString(R.string.degrees_format,
                mCurrentSensorHeading));
    }

    public void setState(State newState) {
        mFirebaseRef.child("state").setValue(newState);
        // Make sure when the match ends that no scheduled timer events from scripts change the FSM state.
        if (mState == State.READY_FOR_MISSION && newState != State.NEAR_BALL_SCRIPT) {
            Toast.makeText(this, "Illegal state transition out of READY_FOR_MISSION", Toast.LENGTH_SHORT).show();
            return;
        }
        mStateStartTime = System.currentTimeMillis();
        mCurrentStateTextView.setText(newState.name());
        speak(newState.name().replace("_", " ").toLowerCase());
        switch (newState) {
            case READY_FOR_MISSION:
                mGoOrMissionCompleteButton.setBackgroundResource(R.drawable.green_button);
                mGoOrMissionCompleteButton.setText("Go!");
                mGoOrMissionCompleteButtonJumbo.setBackgroundResource(R.drawable.green_button);
                mGoOrMissionCompleteButtonJumbo.setText("Go!");
                mViewFlipper.setDisplayedChild(0);
                sendWheelSpeed(0, 0);
                break;
            case NEAR_BALL_SCRIPT:
                mGpsInfoTextView.setText("---"); // Clear GPS display (optional)
                mGuessXYTextView.setText("---"); // Clear guess display (optional)
                mScripts.nearBallScript();

                mViewFlipper.setDisplayedChild(2);
                break;
            case DRIVE_TOWARDS_FAR_BALL:
                // All actions handled in the loop function.
                break;
            case FAR_BALL_SCRIPT:
                mScripts.farBallScript();
                break;
            case DRIVE_TOWARD_HOME:
                // All actions handled in the loop function.
                break;
            case WAITING_FOR_PICKUP:
                sendWheelSpeed(0, 0);
                break;
            case SEEKING_HOME:
                // Actions handled in the loop function.
                break;
        }
        mState = newState;
    }

    private void updateMissionStrategyVariables() {
        //Goal is to set these values
        mNearBallGpsY = -50;
        mFarBallGpsY = 50;
        mNearBallLocation = 1;
        mWhiteBallLocation = 0;
        mFarBallLocation = 3;

        //Example of how you might write this code
        for (int i = 0; i < 3; i++) {
            BallColor currentLocationColor = mLocationColors[i];
            if (currentLocationColor == BallColor.WHITE) {
                mWhiteBallLocation = i + 1;
            }
        }
        // TODO: In your project you’ll add more code to calculate the values below correctly!

        if (mOnRedTeam) {
            Log.d(TAG, "I'm on the red team!");
        } else {
            Log.d(TAG, "I'm on the blue team!");
        }


        Log.d(TAG, "Near ball location: " + mNearBallLocation + " drop off at " + mNearBallGpsY);
        Log.d(TAG, "Far ball location: " + mFarBallLocation + " drop off at " + mFarBallGpsY);
        Log.d(TAG, "White ball location: " + mWhiteBallLocation);
    }

}
