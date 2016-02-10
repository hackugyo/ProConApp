package jp.ne.hatena.hackugyo.procon;

import android.app.Application;
import android.content.Context;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.TimeZone;

/**
 * アプリケーション。アプリ起動時の初回読み込み
 */
public class AppApplication extends Application {

    private static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
        JodaTimeAndroid.init(this);
    }

    public static Object getRefWatcher(Context context) {
        AppApplication application = (AppApplication) context.getApplicationContext();
        return application.getRefWatcherInner();
    }

    public static Context getContext() {
        return sContext;
    }

    public Object getRefWatcherInner() {
        return null;
    }

    /**
     * Return the {@link TimeZone} the app is set to use (either user or conference).
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static TimeZone getDisplayTimeZone(Context context) {
        TimeZone defaultTz = TimeZone.getDefault();
        return defaultTz; // return (isUsingLocalTime(context) && defaultTz != null) ? defaultTz : Config.CONFERENCE_TIMEZONE;
    }

}
