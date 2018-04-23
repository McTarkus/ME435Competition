package edu.rosehulman.me435;

import android.location.Location;

/**
 * In order to use the FieldGps class you must implement the FieldGpsListener
 * interface.  The X and Y will be in feet.  The heading will be in degrees.
 * Heading will always be between -180 and 180.  If this location has no bearing
 * available the value returned is NaN.
 *
 * @author fisherds@gmail.com (Dave Fisher)
 */
public interface FieldGpsListener {
	public void onLocationChanged(double x, double y, double heading, Location location);
}
