package jp.ne.hatena.hackugyo.procon.util;

import android.support.v7.app.ActionBar;

public class ActionBarUtils {
    private ActionBarUtils() {

    }

    public static boolean isTitleShown(ActionBar supportActionBar) {
        if (supportActionBar == null) return false;
        return (supportActionBar.getDisplayOptions() & ActionBar.DISPLAY_SHOW_TITLE) != 0;
    }
}
