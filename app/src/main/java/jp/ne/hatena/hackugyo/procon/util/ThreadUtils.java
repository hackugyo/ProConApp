package jp.ne.hatena.hackugyo.procon.util;

import android.content.Context;

/**
 * {@link java.lang.Thread}処理関連
 */
public class ThreadUtils {
    private ThreadUtils() {}

    /**
     * このメソッドを呼び出したメソッドの実行がUIスレッドかどうかを返します。
     * @see <a href="http://blog.livedoor.jp/sylc/archives/1564156.html">参考リンク</a>
     * @param context
     * @return UIスレッドかどうか
     */
    public static boolean isCurrentThreadMain(Context context) {
        if (context == null) return false;
        return Thread.currentThread().equals(context.getMainLooper().getThread());
    }
}
