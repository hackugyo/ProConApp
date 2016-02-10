package jp.ne.hatena.hackugyo.procon.util;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;

import java.util.HashMap;

import rx.Observable;
import rx.functions.Func1;


public class ColorUtils {
    public static final String TRANSPARENT = "#00FFFFFF";
    private final HashMap<Integer, String> mHashMap;

    private ColorUtils() {
        mHashMap = new HashMap<Integer, String>();
    }

    public static ColorUtils getDefault() {
        return new ColorUtils();
    }

    public String getColorARGBStringFromColorId(Context context, int colorId) {
        if (mHashMap.containsKey(colorId)) return mHashMap.get(colorId);

        int colorInt = ContextCompat.getColor(context, colorId);
        String colorArgb = String.format("#%08X", 0xFFFFFFFF & colorInt);
        mHashMap.put(colorId, colorArgb);
        return colorArgb;
    }

    public int getColorIntFromColorString(String colorArgb) {
        if (colorArgb == null) return Color.TRANSPARENT;
        return Color.parseColor(colorArgb);
    }


    public String[] eachColors(final Context context, int[] colorArgbs) {
        String[] result = new String[colorArgbs.length];
        return Observable.from(ArrayUtils.asList(colorArgbs))
                .map(new Func1<Integer, String>() {
                    @Override
                    public String call(Integer colorArgb) {
                        return String.format("#%08X", 0xFFFFFFFF & colorArgb);
                    }
                })
                .toList()
                .toBlocking()
                .single()
                .toArray(result);
    }

}
