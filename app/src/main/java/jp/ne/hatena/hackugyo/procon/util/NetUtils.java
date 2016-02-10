package jp.ne.hatena.hackugyo.procon.util;

import android.content.Context;
import android.content.pm.PackageManager;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;

import okio.BufferedSource;
import retrofit.HttpException;
import retrofit.Response;

/**
 * ユーザエージェントの設定
 */
public class NetUtils {

    private NetUtils() {

    }

    private static String sUserAgent = null;

    public static String getUserAgent(String appName, Context context) {
        if (sUserAgent == null) {
            sUserAgent = appName;
            try {
                String packageName = context.getPackageName();
                String version = context.getPackageManager().getPackageInfo(packageName, 0).versionName;
                sUserAgent = sUserAgent + " (" + packageName + "/" + version + ")";
                LogUtils.d("User agent set to: " + sUserAgent);
            } catch (PackageManager.NameNotFoundException e) {
                LogUtils.e("Unable to find self by package name", e);
            }
        }
        return sUserAgent;
    }

    public static HttpException getTypicalHttpException(String message) {
        return new HttpException(Response.error(0, new ResponseBody() {
            @Override
            public MediaType contentType() {
                return MediaType.parse("application/json; charset=utf-8");
            }

            @Override
            public long contentLength() throws IOException {
                return 0;
            }

            @Override
            public BufferedSource source() throws IOException {
                return null;
            }
        }));

    }

    public static boolean is4XX(HttpException e) {
        return e.code() / 100 == 4;
    }
    public static boolean is5XX(HttpException e) {
        return e.code() / 100 == 5;
    }
}
