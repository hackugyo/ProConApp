package jp.ne.hatena.hackugyo.procon.adapter;

import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import jp.ne.hatena.hackugyo.procon.R;
import jp.ne.hatena.hackugyo.procon.model.Memo;
import jp.ne.hatena.hackugyo.procon.ui.RecyclerClickable;
import jp.ne.hatena.hackugyo.procon.util.LogUtils;
import jp.ne.hatena.hackugyo.procon.util.StringUtils;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observables.GroupedObservable;
import rx.schedulers.Schedulers;

/**
 * Created by kwatanabe on 16/02/19.
 */
public class SummaryListAdapter  extends RecyclerView.Adapter<SummaryListAdapter.SummaryViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_SUMMARY = 1;

    private final SummaryListAdapter self = this;

    private final RecyclerClickable onClickListener;
    private final int proColor, conColor;
    private Activity context;
    private final List<Memo> pros, cons;

    public SummaryListAdapter(Activity context, RecyclerClickable onClickListener) {
        this.context = context;
        this.pros = new ArrayList<Memo>();
        this.cons = new ArrayList<Memo>();
        this.onClickListener = onClickListener;

        proColor = ContextCompat.getColor(context, R.color.orange_600);
        conColor = ContextCompat.getColor(context, R.color.blue_a400);
    }

    @Override
    public SummaryListAdapter.SummaryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SUMMARY) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_summary, parent, false);
            return new SummaryViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_summary_title, parent, false);
            return new HeaderViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(SummaryListAdapter.SummaryViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
            boolean isPro = (position == 0 && this.pros.size() != 0);
            String title = isPro ? ("賛成派" + this.pros.size() + "人") : ("反対派" + this.cons.size() + "人") ;

            holder.itemView.setBackgroundColor(isPro ? proColor : conColor);
            holder.txtMessage.setText(title);
        } else {
            int proPosition = getPositionInPro(position);
            if (proPosition >= 0) {
                Memo memo = this.pros.get(proPosition);
                holder.txtMessage.setText(memo.getCitationResource());
            } else {
                int conPosition = getPositionInCon(position);
                if (conPosition >= 0) {
                    Memo memo = this.cons.get(conPosition);
                    holder.txtMessage.setText(memo.getCitationResource());
                } else {
                    LogUtils.i("!? " + position + " / " + getItemCount());
                }
            }
        }
    }

    private int getPositionInPro(int realPosition) {
        int prosTitleCount = this.pros.size() == 0 ? 0 : 1;
        int prosCount = prosTitleCount + this.pros.size();
        if (this.pros.size() <= 0) return -1;
        if (prosCount <= realPosition) return -1;
        return realPosition - 1;
    }
    private int getPositionInCon(int realPosition) {
        int prosTitleCount = this.pros.size() == 0 ? 0 : 1;
        int prosCount = prosTitleCount + this.pros.size();
        if (realPosition < prosCount) return -1;
        int consTitleCount = this.cons.size() == 0 ? 0 : 1;
        int consCount = consTitleCount + this.cons.size();
        if (prosCount + consCount < realPosition) return -1;
        return realPosition - prosCount - 1;
    }

    @Override
    public int getItemCount() {
        return this.pros.size() + this.cons.size() + (this.pros.size() != 0 ? 1 : 0) + (this.cons.size() != 0 ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return VIEW_TYPE_HEADER;
        if (position == this.pros.size() + (this.pros.size() == 0 ? 0 : 1) && this.cons.size() != 0) return VIEW_TYPE_HEADER;
        return VIEW_TYPE_SUMMARY;
    }

    public void reloadMemos(List<Memo> memos) {
        this.pros.clear();
        this.cons.clear();
        notifyDataSetChanged();
        Observable.from(memos)
                .observeOn(Schedulers.computation())
                .groupBy(new Func1<Memo, Boolean>() {
                    @Override
                    public Boolean call(Memo memo) {
                        return memo.isPro();
                    }
                })
                .subscribe(new Action1<GroupedObservable<Boolean, Memo>>() {
                    @Override
                    public void call(GroupedObservable<Boolean, Memo> booleanMemoGroupedObservable) {
                        final boolean isPro = booleanMemoGroupedObservable.getKey();
                        booleanMemoGroupedObservable
                                .filter(new Func1<Memo, Boolean>() {
                                    @Override
                                    public Boolean call(Memo memo) {
                                        return memo != null && StringUtils.isPresent(memo.getCitationResource());
                                    }
                                })
                                .distinct(new Func1<Memo, String>() {
                                    @Override
                                    public String call(Memo memo) {
                                        return memo.getCitationResources().get(0).getName();
                                    }
                                })
                                .toSortedList(new Func2<Memo, Memo, Integer>() {
                                    @Override
                                    public Integer call(Memo memo, Memo memo2) {
                                        return memo.getCitationResource().compareTo(memo2.getCitationResource());
                                    }
                                })
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                        new Action1<List<Memo>>() {
                                            @Override
                                            public void call(List<Memo> memos) {
                                                if (isPro) {
                                                    self.pros.addAll(memos);
                                                } else {
                                                    self.cons.addAll(memos);
                                                }
                                                notifyDataSetChanged();
                                            }
                                        }
                                );
                    }
                });

    }

    public static class SummaryViewHolder extends RecyclerView.ViewHolder {

        public TextView txtMessage;
        public SummaryViewHolder(View itemView) {
            super(itemView);
            txtMessage = (TextView) itemView.findViewById(R.id.txtMessage);
        }
    }


    public static class HeaderViewHolder extends SummaryViewHolder {

        public HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }
}
