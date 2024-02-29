package lu.fisch.canze.classes;

import android.util.Log;

import lu.fisch.canze.activities.MainActivity;

public class Crashlytics {
    public static void logException (Exception e) {
        Log.e(MainActivity.TAG, "An error occurred", e);
    }

    public static void logString (String m) {
        Log.w(MainActivity.TAG, m);
    }
}
