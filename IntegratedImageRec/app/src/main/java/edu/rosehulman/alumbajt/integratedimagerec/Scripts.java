package edu.rosehulman.alumbajt.integratedimagerec;

import android.os.Handler;
import android.widget.Toast;


import edu.rosehulman.me435.NavUtils;
import edu.rosehulman.me435.RobotActivity;

public class Scripts {

    private Handler mCommandHandler = new Handler();

    private GolfBallDeliveryActivity mActivity;

    private int ARM_REMOVAL_TIME = 5000;

    public Scripts(GolfBallDeliveryActivity activity) {
        mActivity = activity;
    }

    private String HOME = "0 90 0 -90 90";
    private String BALL1_OPEN = "32 134 -87 -180 13";
    private String BALL1_FLICK = "55 134 -87 -180 13";
    private String BALL2_OPEN = "1 125 -87 -173 13";
    private String BALL2_PRELAUNCH = "1 94 -97 -173 13";
    private String BALL3_OPEN = "-33 130 -97 -180 13";
    private String BALL3_FLICK = "-68 130 -87 -180 13";

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

    protected void removeBallAtLocation(final int location) {
        mActivity.sendCommand("ATTACH 111111"); // Just in case
        switch (location) {
            case 1:
                mActivity.sendCommand("POSITION " + HOME);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + BALL1_OPEN);
                    }
                }, 1000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + BALL1_FLICK);
                    }
                }, 2500);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + HOME);
                    }
                }, 3000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.setLocationToColor(location, GolfBallDeliveryActivity.BallColor.NONE);
                    }
                }, ARM_REMOVAL_TIME);
                break;
            case 2:
                mActivity.sendCommand("POSITION " + HOME);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + BALL2_OPEN);
                    }
                }, 1000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + BALL2_PRELAUNCH);
                    }
                }, 2500);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + BALL2_OPEN);
                    }
                }, 3500);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + HOME);
                    }
                }, 4500);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.setLocationToColor(location, GolfBallDeliveryActivity.BallColor.NONE);
                    }
                }, ARM_REMOVAL_TIME);
                break;
            case 3:
                mActivity.sendCommand("POSITION " + HOME);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + BALL3_OPEN);
                    }
                }, 1000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + BALL3_FLICK);
                    }
                }, 2500);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + HOME);
                    }
                }, 3000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.setLocationToColor(location, GolfBallDeliveryActivity.BallColor.NONE);
                    }
                }, ARM_REMOVAL_TIME);
                break;
        }


    }
}
