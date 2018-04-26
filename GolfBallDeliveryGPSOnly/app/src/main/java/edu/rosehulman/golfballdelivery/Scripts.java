package edu.rosehulman.golfballdelivery;

import android.os.Handler;
import android.widget.Toast;

import edu.rosehulman.me435.NavUtils;
import edu.rosehulman.me435.RobotActivity;

public class Scripts {

    private Handler mCommandHandler = new Handler();

    private GolfBallDeliveryActivity mActivity;

    private int ARM_REMOVAL_TIME = 3000;

    public Scripts(GolfBallDeliveryActivity activity) {
        mActivity = activity;
    }

    public void testStraightScript() {
        Toast.makeText(mActivity, "Begin Short straight drive test at " +
                        mActivity.mLeftStraightPwmValue + "  " + mActivity.mRightStraightPwmValue,
                Toast.LENGTH_SHORT).show();
        mActivity.sendWheelSpeed(mActivity.mLeftStraightPwmValue, mActivity.mRightStraightPwmValue);
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mActivity, "End Short straight drive test", Toast.LENGTH_SHORT).show();
                mActivity.sendWheelSpeed(0, 0);
            }
        }, 8000);
    }

    public void nearBallScript() {

        Toast.makeText(mActivity, "Drive 103 ft to near ball.", Toast.LENGTH_SHORT).show();
        double distanceToNearBall = NavUtils.getDistance(15, 0, 90, 50);
        long driveTimeToNearBallMs = (long) (distanceToNearBall / RobotActivity.DEFAULT_SPEED_FT_PER_SEC * 1000);
        // Well done with the math, but now let’s cheat
        driveTimeToNearBallMs = 3000; // Make this mock script not take so long.
        mActivity.sendWheelSpeed(mActivity.mLeftStraightPwmValue, mActivity.mRightStraightPwmValue);
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                removeBallAtLocation(mActivity.mNearBallLocation);
            }
        }, driveTimeToNearBallMs);
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mActivity.mState == GolfBallDeliveryActivity.State.NEAR_BALL_SCRIPT) {
                    mActivity.setState(GolfBallDeliveryActivity.State.DRIVE_TOWARDS_FAR_BALL);
                }
            }
        }, driveTimeToNearBallMs + ARM_REMOVAL_TIME);
    }

    public void farBallScript() {
        mActivity.sendWheelSpeed(0, 0);
        Toast.makeText(mActivity, "Figure out which ball(s) to remove and do it.", Toast.LENGTH_SHORT).show();
        removeBallAtLocation(mActivity.mFarBallLocation);
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mActivity.mWhiteBallLocation > 0) {
                    removeBallAtLocation(mActivity.mWhiteBallLocation);
                }
                if (mActivity.mState == GolfBallDeliveryActivity.State.FAR_BALL_SCRIPT) {
                    mActivity.setState(GolfBallDeliveryActivity.State.DRIVE_TOWARD_HOME);
                }
            }
        }, ARM_REMOVAL_TIME);
    }

    private void removeBallAtLocation(final int location) {
        // TODO: Replace with a script that might actually remove a ball. :)
        mActivity.sendCommand("ATTACH 111111"); // Just in case
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mActivity.sendCommand("POSITION 83 90 0 -90 90");
            }
        }, 10);
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mActivity.sendCommand("POSITION 90 141 -60 -180 169");
            }
        }, 2000);
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mActivity.setLocationToColor(location, GolfBallDeliveryActivity.BallColor.NONE);
            }
        }, ARM_REMOVAL_TIME);
    }
}
