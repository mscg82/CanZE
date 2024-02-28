package lu.fisch.canze.classes;

import android.util.Log;

public class Crashlytics {
    public static void logException (Exception e) {
        Log.e("CanZE", "An error occurred", e);
    }

    public static void logString (String m) {
        Log.w("CanZE", m);
    }
}
