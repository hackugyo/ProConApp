package jp.ne.hatena.hackugyo.procon;

import android.app.Application;
import android.content.Context;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import net.danlew.android.joda.JodaTimeAndroid;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

/**
 * アプリケーション。アプリ起動時の初回読み込み
 */
public class AppApplication extends Application {

    private static Context sContext;
    private OkHttpClient mOkHttpClient;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
        JodaTimeAndroid.init(this);
        mOkHttpClient = buildOkHttpClient();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://api.yourdomain.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(mOkHttpClient)
                .build();
        Picasso picasso = new Picasso.Builder(this)
                .downloader(new OkHttpDownloader(mOkHttpClient))
                .build();
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


    public static OkHttpClient buildOkHttpClient() {
        OkHttpClient client = new OkHttpClient();

        if (BuildConfig.IS_STAGING) { // STAGING環境のみ、SSLを無視する
            final TrustManager[] trustManagers = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                            // 特に何もしない
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                            // 特に何もしない
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                    }
            };
            try {
                final SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustManagers, new java.security.SecureRandom());
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                client.setSslSocketFactory(sslSocketFactory);
                client.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        // ホスト名の検証を行わない
                        return true;
                    }
                });
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                e.printStackTrace();
            }
        }

        client.setConnectTimeout(Config.HTTP_CLIENT_TIMEOUT_SECOND, TimeUnit.SECONDS);
        client.setWriteTimeout(Config.HTTP_CLIENT_TIMEOUT_SECOND, TimeUnit.SECONDS);
        client.setReadTimeout(Config.HTTP_CLIENT_TIMEOUT_SECOND, TimeUnit.SECONDS);

        return client;
    }
}
