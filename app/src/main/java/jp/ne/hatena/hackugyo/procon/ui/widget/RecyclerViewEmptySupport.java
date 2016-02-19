package jp.ne.hatena.hackugyo.procon.ui.widget;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

/**
 * @see <a href="http://stackoverflow.com/a/30415582">参考リンク</a>
 */
public class RecyclerViewEmptySupport extends RecyclerView {
    private View emptyView;

    private AdapterDataObserver emptyObserver = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            checkIfEmpty();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            super.onItemRangeInserted(positionStart, itemCount);
            checkIfEmpty();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            super.onItemRangeRemoved(positionStart, itemCount);
            checkIfEmpty();
        }
    };

    public RecyclerViewEmptySupport(Context context) {
        super(context);
    }

    public RecyclerViewEmptySupport(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclerViewEmptySupport(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        final Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(emptyObserver);
        }
        super.setAdapter(adapter);

        if(adapter != null) {
            adapter.registerAdapterDataObserver(emptyObserver);
        }
        checkIfEmpty();
    }

    /**
     * 空のとき表示するView {@link android.support.v7.widget.RecyclerView.LayoutManager}をセットする際にいっしょにセットするとよい
     * @param emptyView
     */
    public void setEmptyView(View emptyView) {
        this.emptyView = emptyView;
        checkIfEmpty();
    }

    private void checkIfEmpty() {
        Adapter<?> adapter =  getAdapter();
        if(adapter != null && emptyView != null) {
            if(adapter.getItemCount() == 0) {
                emptyView.setVisibility(View.VISIBLE);
                RecyclerViewEmptySupport.this.setVisibility(View.GONE);
            }
            else {
                emptyView.setVisibility(View.GONE);
                RecyclerViewEmptySupport.this.setVisibility(View.VISIBLE);
            }
        }
    }
}