package jp.ne.hatena.hackugyo.procon.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;

public class DrawableUtils {

    /**
     * {@link android.content.res.Resources#getDrawable(int)}がDeprecatedになったので、これをかわりに使います．
     *
     * @see <a href="http://shekeenlab.hatenablog.com/entry/2015/04/20/003036">参考リンク</a>
     * @param context
     * @param id
     * @return Drawable
     */
    @SuppressWarnings("deprecation")
    public static Drawable getDrawable(Context context, int id){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            return context.getDrawable(id);
        }
        else{
            return context.getResources().getDrawable(id);
        }
    }
}
