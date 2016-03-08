package jp.ne.hatena.hackugyo.procon.ui.widget;

import android.content.Context;
import android.support.annotation.ColorInt;

import com.beardedhen.androidbootstrap.api.attributes.BootstrapBrand;

import jp.ne.hatena.hackugyo.procon.R;

import static com.beardedhen.androidbootstrap.utils.ColorUtils.ACTIVE_OPACITY_FACTOR_EDGE;
import static com.beardedhen.androidbootstrap.utils.ColorUtils.ACTIVE_OPACITY_FACTOR_FILL;
import static com.beardedhen.androidbootstrap.utils.ColorUtils.DISABLED_ALPHA_EDGE;
import static com.beardedhen.androidbootstrap.utils.ColorUtils.DISABLED_ALPHA_FILL;
import static com.beardedhen.androidbootstrap.utils.ColorUtils.decreaseRgbChannels;
import static com.beardedhen.androidbootstrap.utils.ColorUtils.increaseOpacity;
import static com.beardedhen.androidbootstrap.utils.ColorUtils.resolveColor;

/**
 * Created by kwatanabe on 16/03/04.
 */
public enum CustomBootStrapBrand implements BootstrapBrand {

    PRO(R.color.button_color_pros, R.color.button_text_color_light),
    CON(R.color.button_color_cons, R.color.button_text_color_light),
    OTHER(R.color.button_color, R.color.button_text_color_light);

    private final int textColor;
    private final int color;

    CustomBootStrapBrand(int color) {
        this.color = color;
        this.textColor =  android.R.color.white;
    }

    CustomBootStrapBrand(int color, int textColor) {
        this.color = color;
        this.textColor =  textColor;
    }

    @ColorInt
    public int defaultFill(Context context) {
        return resolveColor(color, context);
    }

    @ColorInt public int defaultEdge(Context context) {
        return decreaseRgbChannels(context, color, ACTIVE_OPACITY_FACTOR_EDGE);
    }

    @ColorInt public int activeFill(Context context) {
        return decreaseRgbChannels(context, color, ACTIVE_OPACITY_FACTOR_FILL);
    }

    @ColorInt public int activeEdge(Context context) {
        return decreaseRgbChannels(context, color, ACTIVE_OPACITY_FACTOR_FILL + ACTIVE_OPACITY_FACTOR_EDGE);
    }

    @ColorInt public int disabledFill(Context context) {
        return increaseOpacity(context, color, DISABLED_ALPHA_FILL);
    }

    @ColorInt public int disabledEdge(Context context) {
        return increaseOpacity(context, color, DISABLED_ALPHA_FILL - DISABLED_ALPHA_EDGE);
    }

    @ColorInt public int defaultTextColor(Context context) {
        return resolveColor(textColor, context);
    }

    @ColorInt public int activeTextColor(Context context) {
        return resolveColor(textColor, context);
    }

    @ColorInt public int disabledTextColor(Context context) {
        return resolveColor(textColor, context);
    }

}
