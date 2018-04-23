package edu.rosehulman.me435;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

/**
 * Helper class to convert GPS readings into field X and Y values. Units
 * returned will be in feet and bearings will be in degrees using conventional
 * Cartesian coordinates with the positive X axis being zero degrees. To use
 * this class you will need to define the field and provide a listener.
 * 
 * @author fisherds@gmail.com (Dave Fisher)
 */
public class FieldGps implements LocationListener {

  public static final String TAG = FieldGps.class.getSimpleName();

  private static final String[] LOCATION_PERMS = {
    Manifest.permission.ACCESS_FINE_LOCATION
  };
  private static final int LOCATION_REQUEST = 0;

  /** GPS will give updates no faster than this. */
  private static final long DEFAULT_MIN_TIME_MS = 1000;  // 1 second
  /** GPS will give updates if the moved is greater than this. */
  private static final float DEFAULT_MIN_DISTANCE = 0.1f;  // 10 cm 
  /** Value returned for the heading if no bearing is available. */
  public static final double NO_BEARING_AVAILABLE = Double.NaN; 
  /** Value returned for the accuracy if no accuracy is available. */
  public static final double NO_ACCURACY_AVAILABE = Double.NaN;
  /** Conversion factor to convert to feet. */
  public static final double FEET_PER_METER = 3.28084;
  
  /** Some default field coordinates if unknown initially. */
  public static final double ROSE_FRONT_CIRCLE_LATITUDE = 39.482363;
  public static final double ROSE_FRONT_CIRCLE_LONGITUDE = -87.323982;
  public static final double ROSE_FRONT_ENTRANCE_LATITUDE = 39.480954;
  public static final double ROSE_FRONT_ENTRANCE_LONGITUDE = -87.323427;

  /** Member variables that define the field origin and X-Axis. */
  private double mLatitudeOrigin, mLongitudeOrigin, mLatitudeOnXAxis, mLongitudeOnXAxis;

  /** Listener that will be called when new GPS locations are available. */
  private FieldGpsListener mListener;

  /** Context used to fetch the LocationManager. */
  private Context mContext;
  
  /** Last GOS Location received. */
  private Location mLastGpsLocation = null;
  
  /**
   * Instantiate the FieldGps with only a listener.  Note that the origin and
   * X axis must be defined, but if they are unknown initially you can use this
   * method and they will be set to the Rose-Hulman front entry.  Front entry
   * origin is the intersection near the front circle.  Positive X is to the entrance.
   * 
   * See main constructor for more info.
   * 
   * @param listener
   *          Listener that implements FieldGpsListener that will be called with
   *          updates.
   */
  public FieldGps(FieldGpsListener listener) {
    this(listener, 
        ROSE_FRONT_CIRCLE_LATITUDE, ROSE_FRONT_CIRCLE_LONGITUDE,
        ROSE_FRONT_ENTRANCE_LATITUDE, ROSE_FRONT_ENTRANCE_LONGITUDE);
  }

  /**
   * Instantiate the FieldGps with a listener and field latitudes and
   * longitudes. Note that the listener will not be called until after the GPS
   * system is attached. So a client must create a FieldGps then call
   * requestLocationUpdates.
   * 
   * @param listener
   *          Listener that implements FieldGpsListener that will be called with
   *          updates.
   * @param latitudeOrigin
   *          Latitude of the field origin.
   * @param longitudeOrigin
   *          Longitude of the field origin.
   * @param latitudeOnXAxis
   *          Latitude of any point that is on the X axis.
   * @param longitudeOnXAxis
   *          Longitude of any point that is on the X axis.
   */
  public FieldGps(FieldGpsListener listener, double latitudeOrigin, double longitudeOrigin,
      double latitudeOnXAxis, double longitudeOnXAxis) {
    mListener = listener;
    mLatitudeOrigin = latitudeOrigin;
    mLongitudeOrigin = longitudeOrigin;
    mLatitudeOnXAxis = latitudeOnXAxis;
    mLongitudeOnXAxis = longitudeOnXAxis;
  }

  /**
   * Begin receiving GPS updates. FieldGps will convert the latitude and
   * longitude values into field feet X and Y values, and bearings into theta
   * values from the X axis. This version uses default values for the update
   * frequency. The defaults provide a steady stream of updates that will be
   * nearly as accurate as you can hope for.
   * 
   * @param context
   *          A context (usually an Activity) used to get the LocationManager.
   */
  public void requestLocationUpdates(Context context) {
    requestLocationUpdates(context, DEFAULT_MIN_TIME_MS, DEFAULT_MIN_DISTANCE);
  }

  /**
   * Begin receiving GPS updates. FieldGps will convert the latitude and
   * longitude values into field feet X and Y values, and bearings into theta
   * values from the X axis. This version uses parameters to set the update
   * frequency. There is a time and distance criteria, whichever criteria is met
   * first (usually time) will trigger a GPS update.
   * 
   * @param context
   *          A context (usually an Activity) used to get the LocationManager.
   * @param minTime_ms
   *          Time in milliseconds between GPS location updates.
   * @param minDistance_meters
   *          Distance in meters between GPS location updates.
   */
  public void requestLocationUpdates(Context context, long minTime_ms, float minDistance_meters) {
    mContext = context;
    LocationManager locationManager = (LocationManager) context
        .getSystemService(Context.LOCATION_SERVICE);
    final boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    if (!gpsEnabled) {
      Toast.makeText(context, "Enable GPS in Settings", Toast.LENGTH_LONG).show();
      context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    } else {

      if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
              mContext, Manifest.permission.ACCESS_FINE_LOCATION)) {
        Log.d(TAG, "Begin requesting locations.");
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime_ms,
                minDistance_meters, this);
      } else {
        // Added due to:
        // http://stackoverflow.com/questions/32083913/android-gps-requires-access-fine-location-error-even-though-my-manifest-file
        Log.d(TAG, "Requesting permission for fine location.");
        ActivityCompat.requestPermissions((Activity)mContext, LOCATION_PERMS, LOCATION_REQUEST);
      }
    }
  }


  /**
   * Stop receiving GPS updates.
   */
  public void removeUpdates() {
    LocationManager locationManager = (LocationManager) mContext
        .getSystemService(Context.LOCATION_SERVICE);
    if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
            mContext, Manifest.permission.ACCESS_FINE_LOCATION)) {
      locationManager.removeUpdates(this);
    }
  }

  /**
   * Callback used by the GPS API to provide a new GPS location.
   */
  public void onLocationChanged(Location location) {
    mLastGpsLocation = location;
    dispatchOnLocationChangedEvent(location);
  }

  /**
   * Convert this location into field feet X and Y, and convert the bearing into
   * degrees (0 degrees is right on X axis). Once conversion is complete call
   * the listener.
   * 
   * @param location
   *          Location to convert and send.
   */
  public void dispatchOnLocationChangedEvent(Location location) {
    float[] originToXAxisLocation = new float[2];
    Location.distanceBetween(mLatitudeOrigin, mLongitudeOrigin, mLatitudeOnXAxis,
        mLongitudeOnXAxis, originToXAxisLocation);
    float[] originToCurrentLocation = new float[2];
    Location.distanceBetween(mLatitudeOrigin, mLongitudeOrigin, location.getLatitude(),
        location.getLongitude(), originToCurrentLocation);
    double thetaRadians = (originToXAxisLocation[1] - originToCurrentLocation[1]) * Math.PI / 180.0;
    double fieldX = originToCurrentLocation[0] * Math.cos(thetaRadians) * FEET_PER_METER;
    double fieldY = originToCurrentLocation[0] * Math.sin(thetaRadians) * FEET_PER_METER;
    double fieldBearing = NO_BEARING_AVAILABLE;
    if (location.hasBearing()) {
      fieldBearing = originToXAxisLocation[1] - location.getBearing();
      fieldBearing = normalizeAngle(fieldBearing);
    }
    mListener.onLocationChanged(fieldX, fieldY, fieldBearing, mLastGpsLocation);
  }

  /**
   * Normalize any angle to -180 to 180.
   * 
   * @param angle
   *          Original angle that is not normalized.
   * @return A normalized equivalent angle.
   */
  private double normalizeAngle(double angle) {
    while (angle <= -180.0)
      angle += 360.0;
    while (angle > 180.0)
      angle -= 360.0;
    return angle;
  }

  // Other required methods from the LocationListener.
  public void onProviderDisabled(String provider) {
    // Intentionally left blank.
  }

  public void onProviderEnabled(String provider) {
    // Intentionally left blank.
  }

  public void onStatusChanged(String provider, int status, Bundle extras) {
    // Intentionally left blank.
  }

  /** Changing the field origin to last GPS reading. */
  public void setCurrentLocationAsOrigin() {
    if (mLastGpsLocation != null) {
      setOriginLocation(mLastGpsLocation);      
    }
  }

  /** Update the origin location. */
  public void setOriginLocation(Location newOrigin) {
    mLatitudeOrigin = newOrigin.getLatitude();
    mLongitudeOrigin = newOrigin.getLongitude();
    dispatchOnLocationChangedEvent(newOrigin);
  }

  /** Change the field X axis location to last GPS reading. */
  public void setCurrentLocationAsLocationOnXAxis() {
    if (mLastGpsLocation != null) {
      setLocationOnXAxis(mLastGpsLocation);
    }
  }

  /** Update the location on the X axis. */
  public void setLocationOnXAxis(Location newLocationOnXAxis) {
    mLatitudeOnXAxis = newLocationOnXAxis.getLatitude();
    mLongitudeOnXAxis = newLocationOnXAxis.getLongitude();
    dispatchOnLocationChangedEvent(newLocationOnXAxis);
  }
  
  /** Return the last GPS Location received. */
  public Location getGpsLocation() {
    return mLastGpsLocation;
  }
}

