package jp.ne.hatena.hackugyo.procon.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * ネットワークコネクションについてのユーティリティメソッド
 */
public class ConnectionUtils {

    private ConnectionUtils() {

    }

    public static boolean isConnected(Context context) {
        if (AppUtils.isDebuggable(context)) {
            context.enforceCallingOrSelfPermission(//
                    android.Manifest.permission.ACCESS_NETWORK_STATE, "need permission: ACCESS_NETWORK_STATE");
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null) {
            return cm.getActiveNetworkInfo().isConnected();
        }
        return false;
    }
}