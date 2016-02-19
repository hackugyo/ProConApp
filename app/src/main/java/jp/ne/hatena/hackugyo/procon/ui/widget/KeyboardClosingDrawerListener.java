package jp.ne.hatena.hackugyo.procon.ui.widget;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * 開閉するときキーボードを閉じる
 */
public class KeyboardClosingDrawerListener implements DrawerLayout.DrawerListener {
    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {

    }

    @Override
    public void onDrawerOpened(View drawerView) {
        //Keyboard の消去
        InputMethodManager inputMethodManager = (InputMethodManager) drawerView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(drawerView.getWindowToken(), 0);
    }

    @Override
    public void onDrawerClosed(View drawerView) {
        //Keyboard の消去
        InputMethodManager inputMethodManager = (InputMethodManager) drawerView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(drawerView.getWindowToken(), 0);
    }

    @Override
    public void onDrawerStateChanged(int newState) {

    }
}
