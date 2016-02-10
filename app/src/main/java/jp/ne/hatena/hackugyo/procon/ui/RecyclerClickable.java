package jp.ne.hatena.hackugyo.procon.ui;

import android.view.View;

/***
 * RecyclerViewにはアイテムのクリックイベントなどに対応したリスナがないため、自作しています。
 */
public interface RecyclerClickable {
    void onRecyclerClicked(View v, int position);
    void onRecyclerButtonClicked(View v, int position);
    boolean onRecyclerLongClicked(View v, int position);
}
