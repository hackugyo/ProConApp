package jp.ne.hatena.hackugyo.procon.util;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class AppUtils {
    private AppUtils() {

    }
    /**
     * デバッグモードかどうかを取得します.
     *
     * @return true の場合はデバッグモード（apkをインストールして実行したものではないということ，実行モードは関係ない）
     */
    public static boolean isDebuggable(Context context) {
        PackageManager pm = null;
        try {
            pm = context.getPackageManager();
        } catch (NullPointerException e) {
            // contextが取得できないということはアプリが起動していないのにこのメソッドが呼ばれたということなので，
            // テスト実行中と見なす．Debuggableとする
            return true;
        }
        ApplicationInfo info = null;
        try {
            info = pm.getApplicationInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
        if ((info.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE) {
            return true;
        }
        return false;
    }

    public static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }
}
