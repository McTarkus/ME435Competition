package edu.rosehulman.alumbajt.integratedimagerec;

import android.os.Handler;
import android.widget.Toast;


import edu.rosehulman.me435.NavUtils;
import edu.rosehulman.me435.RobotActivity;

public class Scripts {

    private Handler mCommandHandler = new Handler();

    private GolfBallDeliveryActivity mActivity;

    protected int ARM_REMOVAL_TIME = 6000;

    public Scripts(GolfBallDeliveryActivity activity) {
        mActivity = activity;
    }

    protected String HOME = "0 90 0 -90 90";
    private String BALL1_OPEN = "32 134 -87 -180 13";
    private String BALL1_FLICK = "55 134 -87 -180 13";
    private String BALL2_OPEN = "1 125 -87 -173 13";
    private String BALL2_PRELAUNCH = "1 94 -97 -173 13";
    private String BALL3_OPEN = "-33 130 -97 -180 13";
    private String BALL3_FLICK = "-68 130 -87 -180 13";
    protected String ZERO = "0 0 0 0 0";

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

//    public void nearBallScript() {
//
//        Toast.makeText(mActivity, "Drive 103 ft to near ball.", Toast.LENGTH_SHORT).show();
//        double distanceToNearBall = NavUtils.getDistance(15, 0, 90, 50);
//        long driveTimeToNearBallMs = (long) (distanceToNearBall / RobotActivity.DEFAULT_SPEED_FT_PER_SEC * 1000);
//        // Well done with the math, but now let’s cheat
//        driveTimeToNearBallMs = 3000; // Make this mock script not take so long.
//        mActivity.sendWheelSpeed(mActivity.mLeftStraightPwmValue, mActivity.mRightStraightPwmValue);
//        mCommandHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                removeBallAtLocation(mActivity.mNearBallLocation);
//            }
//        }, driveTimeToNearBallMs);
//        mCommandHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (mActivity.mState == GolfBallDeliveryActivity.State.NEAR_BALL_SCRIPT) {
//                    mActivity.setState(GolfBallDeliveryActivity.State.DRIVE_TOWARDS_FAR_BALL);
//                }
//            }
//        }, driveTimeToNearBallMs + ARM_REMOVAL_TIME);
//    }
//
//
//    public void farBallScript() {
//        mActivity.sendWheelSpeed(0, 0);
//        Toast.makeText(mActivity, "Figure out which ball(s) to remove and do it.", Toast.LENGTH_SHORT).show();
//        removeBallAtLocation(mActivity.mFarBallLocation);
//        mCommandHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (mActivity.mWhiteBallLocation > 0) {
//                    removeBallAtLocation(mActivity.mWhiteBallLocation);
//                }
//                if (mActivity.mState == GolfBallDeliveryActivity.State.FAR_BALL_SCRIPT) {
//                    mActivity.setState(GolfBallDeliveryActivity.State.DRIVE_TOWARD_HOME);
//                }
//            }
//        }, ARM_REMOVAL_TIME);
//    }

    protected void removeBallAtLocation(final int location) {
        mActivity.sendCommand("POSITION " + HOME);
        switch (location) {
            case 1:
                mActivity.sendCommand("POSITION " + HOME);
                mActivity.mFirebaseRef.child("messages").setValue("ome position");
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + BALL1_OPEN);
                        mActivity.mFirebaseRef.child("messages").setValue("ball 1 open position");
                    }
                }, ARM_REMOVAL_TIME - 4000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + BALL1_FLICK);
                        mActivity.mFirebaseRef.child("messages").setValue("ball 1 flick position");
                    }
                }, ARM_REMOVAL_TIME - 1500);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + HOME);
                        mActivity.mFirebaseRef.child("messages").setValue("home position");
                    }
                }, ARM_REMOVAL_TIME-1000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.setLocationToColor(location, GolfBallDeliveryActivity.BallColor.NONE);
                        mActivity.mFirebaseRef.child("messages").setValue("clearing ball 1");
                    }
                }, ARM_REMOVAL_TIME);
                break;
            case 2:
                mActivity.sendCommand("POSITION " + HOME);
                mActivity.mFirebaseRef.child("messages").setValue("home position");
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + BALL2_OPEN);
                        mActivity.mFirebaseRef.child("messages").setValue("ball 2 open position");
                    }
                }, ARM_REMOVAL_TIME - 4000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + BALL2_PRELAUNCH);
                        mActivity.mFirebaseRef.child("messages").setValue("ball 2 prelaunch position");
                    }
                }, ARM_REMOVAL_TIME - 2500);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + BALL2_OPEN);
                        mActivity.mFirebaseRef.child("messages").setValue("ball 2 open position");
                    }
                }, ARM_REMOVAL_TIME - 1500);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + HOME);
                        mActivity.mFirebaseRef.child("messages").setValue("home position");
                    }
                }, 5500);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.setLocationToColor(location, GolfBallDeliveryActivity.BallColor.NONE);
                        mActivity.mFirebaseRef.child("messages").setValue("clearing ball 2");
                    }
                }, ARM_REMOVAL_TIME);
                break;
            case 3:
                mActivity.sendCommand("POSITION " + HOME);
                mActivity.mFirebaseRef.child("messages").setValue("home position");
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + BALL3_OPEN);
                        mActivity.mFirebaseRef.child("messages").setValue("ball 3 open position");
                    }
                }, ARM_REMOVAL_TIME - 4000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + BALL3_FLICK);
                        mActivity.mFirebaseRef.child("messages").setValue("ball 3 flick position");
                    }
                }, ARM_REMOVAL_TIME - 1500);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION " + HOME);
                        mActivity.mFirebaseRef.child("messages").setValue("home position");
                    }
                }, ARM_REMOVAL_TIME - 500);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.setLocationToColor(location, GolfBallDeliveryActivity.BallColor.NONE);
                        mActivity.mFirebaseRef.child("messages").setValue("clearing ball 3");
                    }
                }, ARM_REMOVAL_TIME);
                break;
        }


    }
}
