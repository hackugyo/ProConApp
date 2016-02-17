package jp.ne.hatena.hackugyo.procon.util;


import android.content.Context;
import android.util.Log;

import jp.ne.hatena.hackugyo.procon.AppApplication;
import jp.ne.hatena.hackugyo.procon.BuildConfig;

/**
 * Log出力クラス.
 * 基本的に、アプリケーションでのログ出力はこのクラスを使う
 *
 * @author User
 */
public final class LogUtils {

    private LogUtils() {

    }

    /**
     * タグを指定してデバッグログを出力します.
     *
     * @param tag
     *            タグ
     * @param msg
     *            デバッグログ
     */
    public static void d(String tag, CharSequence msg) {
        if (!Log.isLoggable(tag, Log.DEBUG)) return;
        Log.d(tag, getLogForm(Thread.currentThread().getStackTrace()) + msg);
    }

    /**
     * デバッグログを出力します.
     *
     * @param msg
     *            デバッグログ
     */
    public static void d(CharSequence msg) {
        if (!Log.isLoggable(getLogTag(), Log.DEBUG)) return;
        Log.d(getLogTag(), getLogForm(Thread.currentThread().getStackTrace()) + msg);
    }

    public static void i(CharSequence msg) {
        if (!Log.isLoggable(getLogTag(), Log.INFO)) return;
        Log.i(getLogTag(), getLogForm(Thread.currentThread().getStackTrace()) + msg);
    }

    public static void i(String tag, CharSequence msg) {
        if (!Log.isLoggable(tag, Log.INFO)) return;
        Log.i(tag, getLogForm(Thread.currentThread().getStackTrace()) + msg);
    }

    /**
     * エラーログを出力します.
     *
     * @param msg
     *            エラーログ
     */
    public static void e(CharSequence msg) {
        Log.e(getLogTag(), getLogForm(Thread.currentThread().getStackTrace()) + msg);
    }

    /**
     * エラーログを出力します.
     *
     * @param tag
     *            タグ
     * @param msg
     *            エラーログ
     */
    public static void e(String tag, CharSequence msg) {
        Log.e(tag, getLogForm(Thread.currentThread().getStackTrace()) + msg);
    }

    /**
     * エラーログを出力します.
     *
     * @param msg
     *            エラーログ
     * @param e
     *            例外
     */
    public static void e(CharSequence msg, Throwable e) {
        Log.e(getLogTag(), getLogForm(Thread.currentThread().getStackTrace()) + msg, e);
    }

    /**
     * エラーログを出力します.
     *
     * @param tag
     *            タグ
     * @param msg
     *            エラーログ
     * @param e
     *            例外
     */
    public static void e(String tag, CharSequence msg, Throwable e) {
        Log.e(tag, getLogForm(Thread.currentThread().getStackTrace()) + msg, e);
    }

    /**
     * タグを指定してワーニングログを出力します.
     *
     * @param tag
     *            タグ
     * @param msg
     *            ワーニングログ
     */
    public static void w(String tag, CharSequence msg) {
        if (!Log.isLoggable(tag, Log.WARN)) return;
        Log.w(tag, getLogForm(Thread.currentThread().getStackTrace()) + msg);
    }

    /**
     * ワーニングログを出力します.
     *
     * @param msg
     *            ワーニングログ
     */
    public static void w(CharSequence msg) {
        if (!Log.isLoggable(getLogTag(), Log.WARN)) return;
        Log.w(getLogTag(), getLogForm(Thread.currentThread().getStackTrace()) + msg);
    }

    /**
     * タグを指定してVerboseログを出力します.
     *
     * @param tag
     *            タグ
     * @param msg
     *            Verboseログ
     */
    public static void v(String tag, CharSequence msg) {
        if (!Log.isLoggable(tag, Log.VERBOSE)) return;
        Log.v(tag, getLogForm(Thread.currentThread().getStackTrace()) + msg);
    }

    /**
     * Verboseログを出力します.
     *
     * @param msg
     *            Verboseログ
     */
    public static void v(CharSequence msg) {
        if (!Log.isLoggable(getLogTag(), Log.VERBOSE)) return;
        Log.v(getLogTag(), getLogForm(Thread.currentThread().getStackTrace()) + msg);
    }

    /**
     * ログ出力した箇所のメソッド情報に加え，そのメソッドを呼び出したメソッドの情報も表示します．
     *
     * @param logLevel
     * @param maxSteps
     * @param msg
     */
    public static void withCaller(int logLevel, int maxSteps, CharSequence msg) {
        if (!Log.isLoggable(getLogTag(), logLevel)) return;
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        if (stackTrace.length <= 3 || stackTrace == null) return;
        String message = getLogForm(stackTrace[3]) + msg;
        maxSteps = Math.min(3 + maxSteps, stackTrace.length - 1);
        for (int step = 4; step <= maxSteps; step++) {
            message = StringUtils.build(//
                    message, StringUtils.getCRLF(),//
                    "at ", getLogForm(stackTrace[step])//
            );
        }
        switch (logLevel) {
            case Log.VERBOSE:
                Log.v(getLogTag(), message);
                break;
            case Log.DEBUG:
                Log.d(getLogTag(), message);
                break;
            case Log.INFO:
                Log.i(getLogTag(), message);
                break;
            case Log.WARN:
                Log.w(getLogTag(), message);
                break;
            case Log.ERROR:
                Log.e(getLogTag(), message);
                break;
            default:
                break;
        }
    }

    /**
     * ログのヘッダ情報を整形します.
     *
     * @param elements
     *            実行中のメソッド情報
     * @return ヘッダ情報
     */
    private static String getLogForm(StackTraceElement[] elements) {
        if (elements.length <= 3 || elements == null) return "";
        return getLogForm(elements[3]);
    }

    private static String getLogForm(StackTraceElement element) {

        StringBuilder sb = new StringBuilder();
        try {
            String file = element.getFileName();
            String method = element.getMethodName();
            int line = element.getLineNumber();
            sb.append(StringUtils.ellipsizeMiddle(file.replace(".java", ""), 25, true));
            sb.append("#").append(StringUtils.ellipsize(method, 18, true));
            sb.append("() [").append(String.format("%1$04d", line)).append("] : ");
        } catch (NullPointerException ignore) {
            // ignore. return blank string.
            // リリースビルドでは，elements[3]がnullになるようなので，ここで握りつぶしておく．
        }
        return sb.toString();
    }

    private static String getLogTag() {
        Context context = AppApplication.getContext();

        return context == null ? LogUtils.class.getSimpleName() : BuildConfig.APPLICATION_ID.substring(0, Math.min(BuildConfig.APPLICATION_ID.length(), 23));
    }


    private static final String LOG_PREFIX = "procon_";
    private static final int LOG_PREFIX_LENGTH = LOG_PREFIX.length();
    private static final int MAX_LOG_TAG_LENGTH = 23;

    public static String makeDebugName(String str) {
        if (str.length() > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            return LOG_PREFIX + str.substring(0, MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH - 1);
        }

        return LOG_PREFIX + str;
    }
    public static String makeDebugName(Class cls) {
        return makeDebugName(cls.getSimpleName());
    }

}
