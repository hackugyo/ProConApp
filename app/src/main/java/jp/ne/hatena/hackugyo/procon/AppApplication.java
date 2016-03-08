package jp.ne.hatena.hackugyo.procon;

import android.app.Application;
import android.content.Context;

import com.beardedhen.androidbootstrap.TypefaceProvider;
import com.github.kubode.rxeventbus.RxEventBus;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import net.danlew.android.joda.JodaTimeAndroid;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import jp.ne.hatena.hackugyo.procon.event.DatabaseService;
import jp.ne.hatena.hackugyo.procon.event.RxBusProvider;
import jp.ne.hatena.hackugyo.procon.model.ChatTheme;
import jp.ne.hatena.hackugyo.procon.model.ChatThemeRepository;
import jp.ne.hatena.hackugyo.procon.model.MemoRepository;
import jp.ne.hatena.hackugyo.procon.util.LogUtils;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

/**
 * アプリケーション。アプリ起動時の初回読み込み
 */
public class AppApplication extends Application {

    private static Context sContext;
    private OkHttpClient mOkHttpClient;
    private RxEventBus bus;
    private DatabaseService databaseService;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
        JodaTimeAndroid.init(this);
        TypefaceProvider.registerDefaultIconSets();
        mOkHttpClient = buildOkHttpClient();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://api.yourdomain.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(mOkHttpClient)
                .build();
        Picasso picasso = new Picasso.Builder(this)
                .downloader(new OkHttpDownloader(mOkHttpClient))
                .build();

        ChatThemeRepository chatThemeRepository = new ChatThemeRepository(this);
        List<ChatTheme> all = chatThemeRepository.findAll();
        if (all.size() == 0) {
            chatThemeRepository.save(new ChatTheme("最初の議題"));
        }
        chatThemeRepository.onPause(); // 停止

        bus = RxBusProvider.getInstance();

        databaseService = new DatabaseService(this, bus, new MemoRepository(this));
        // unregisterをする必要はない。#onTerminate()はテスト用メソッドのため、呼ばれない
    }

    public static OkHttpClient provideOkHttpClient(Context context) {
        AppApplication application = (AppApplication) context.getApplicationContext();
        return application.mOkHttpClient;
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
        if (BuildConfig.DEBUG) {
            client.interceptors().add(buildLoggingInterceptor());
            client.interceptors().add(buildResponseTimeInterceptor());
        }
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

        int cacheSize = 10 * 1024 * 1024; // 10 MiB
        File cacheDir = new File(System.getProperty("java.io.tmpdir"), "okhttp-cache");
        client.setCache(new Cache(cacheDir, cacheSize));

        return client;
    }

    public static Interceptor buildLoggingInterceptor() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        // set your desired log level
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        return logging;
    }

    public static Interceptor buildResponseTimeInterceptor() {
        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();

                LogUtils.d( String.format("Requesting for %s", request.url()));

                long t1 = System.nanoTime();
                Response response = chain.proceed(request);
                long t2 = System.nanoTime();
                LogUtils.d( String.format("  Received response for %s in %.1fms%n%s",
                        response.request().url(), (t2 - t1) / 1e6d, response.headers())
                );
                return response;
            }
        };
    }


}
