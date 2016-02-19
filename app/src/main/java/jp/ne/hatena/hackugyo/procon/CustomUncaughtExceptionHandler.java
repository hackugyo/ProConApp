package jp.ne.hatena.hackugyo.procon;


import android.content.Context;

import jp.ne.hatena.hackugyo.procon.util.AppUtils;

/**
 * キャッチされなかった例外を処理する
 *
 * @author kwatanabe
 *
 */
public class CustomUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;

    public CustomUncaughtExceptionHandler(Context context) {
        // デフォルト例外ハンドラを保持する。
        mDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (AppUtils.isDebuggable(AppApplication.getContext())) ex.printStackTrace();

        // デフォルト例外ハンドラを実行し、強制終了します。
        mDefaultUncaughtExceptionHandler.uncaughtException(thread, ex);
    }
}
