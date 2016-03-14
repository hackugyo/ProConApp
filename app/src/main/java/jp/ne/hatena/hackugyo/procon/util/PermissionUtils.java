package jp.ne.hatena.hackugyo.procon.util;


import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by kwatanabe on 16/01/04.
 */
public class PermissionUtils {
    private PermissionUtils() {

    }

    /**
     * 必要なパーミッションがない場合、falseを返し、ダイアログを表示する。
     * @param activity
     * @param requestCodeWhenShowing
     * @param permissions 許可を依頼したいパーミッション。AndroidManifestに記述していない場合、{@link ActivityCompat#requestPermissions(Activity, String[], int)}を呼んでもダイアログが出ないので注意。
     * @return 必要なパーミッションがとれている場合true
     */
    public static boolean checkSelfPermission(Activity activity, int requestCodeWhenShowing, String... permissions) {
        if (permissions == null || permissions.length == 0) return true;
        if (StringUtils.isEmpty(permissions[0])) return true;
        String[] permissionsNotGranted = getPermissionsNotGranted(activity, permissions);
        String[] permissionsShouldShowRequestPermissionRationale = getPermissionsShouldShowRequestPermissionRationale(activity, permissionsNotGranted);
        if (permissionsShouldShowRequestPermissionRationale != null && permissionsShouldShowRequestPermissionRationale.length > 0) {
            // ダイアログを表示する場合。
            // ここで一発アクセスする目的を説明してからリクエストするのが推奨されている。
            Toast.makeText(activity, "必要なパーミッションです。", Toast.LENGTH_LONG).show();
        }

        if (permissionsNotGranted != null && permissionsNotGranted.length > 0) {
            ActivityCompat.requestPermissions(activity,
                    permissionsNotGranted,
                    requestCodeWhenShowing);
            return false;
        }
        return true;
    }

    private static String[] getPermissionsNotGranted(Activity activity, String... permissions) {
        if (permissions == null) return null;
        ArrayList<String> result = new ArrayList<>();
        for (String p : permissions) {
            if (p == null) continue;
            int permissionCheck = ContextCompat.checkSelfPermission(activity, p);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) result.add(p);
        }
        String[] array = new String[result.size()];
        return result.toArray(array);
    }


    private static String[] getPermissionsShouldShowRequestPermissionRationale(Activity activity, String... permissions) {
        if (permissions == null) return null;
        ArrayList<String> result = new ArrayList<>();
        for (String p : permissions) {
            if (p == null) continue;
            boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, p);
            if (showRationale) result.add(p);
        }
        String[] array = new String[result.size()];
        return result.toArray(array);
    }

    public static boolean hasGranted(int[] grantResults) {
        return grantResults == null || !containsNot(grantResults, PackageManager.PERMISSION_DENIED);
    }
    /**
     * 指定されたint以外のものが含まれていたらtrue, 含まれていなければfalseを返します。
     *
     * @param grantResults
     * @param prohibited
     * @return
     */
    private static boolean containsNot(int[] grantResults, int prohibited) {
        if (grantResults == null) return true;
        for (int i : grantResults) {
            if (i != prohibited) return false;
        }
        return true;
    }
}
