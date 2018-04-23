package edu.rosehulman.me435;

/**
 * In order to use the FieldOrientation class you must implement the
 * FieldOrientationListener interface.  The field heading is the angle with
 * respect to the field's positive X axis.  Units are in degrees.  In
 * addition to the field heading there is an array of three floats that contain
 * the azimuth (rotation about Z), pitch (rotation about X), and roll (rotation
 * about Y).  The aximuth is the same as the field heading but with due north
 * always being 0 degrees.
 *
 * @author fisherds@gmail.com (Dave Fisher)
 */
public interface FieldOrientationListener {
	public void onSensorChanged(double fieldHeading, float[] orientationValues);
}
