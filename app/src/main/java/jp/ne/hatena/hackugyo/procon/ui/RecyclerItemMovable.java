package jp.ne.hatena.hackugyo.procon.ui;

/**
 * {@link  android.support.v7.widget.RecyclerView}内のアイテムをドラッグなどで移動できるように定義するインタフェース。
 */
public interface RecyclerItemMovable {
    void onItemMoved(int fromPosition, int toPosition);
    void onItemDismiss(int position);
    void onItemInsert();
}