package jp.ne.hatena.hackugyo.procon.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.leocardz.link.preview.library.TextCrawler;

import java.util.List;

import jp.ne.hatena.hackugyo.procon.R;
import jp.ne.hatena.hackugyo.procon.model.Memo;
import jp.ne.hatena.hackugyo.procon.ui.RecyclerClickable;
import jp.ne.hatena.hackugyo.procon.util.StringUtils;

/**
 * Created by kwatanabe on 15/08/27.
 */
public class ChatLikeListAdapter extends RecyclerView.Adapter<ChatLikeListAdapter.ChatLikeViewHolder> {

    private static final int VIEW_TYPE_NORMAL_BUBBLE = 0;
    private static final int VIEW_TYPE_URL_PREVIEW = 1;
    private final List<Memo> mMemos;
    private final RecyclerClickable mOnClickListener;
    private final TextCrawler textCrawler;
    private Activity context;

    public ChatLikeListAdapter(Activity context, List<Memo> memos) {
        this.context = context;
        this.mMemos = memos;
        mOnClickListener = ((context instanceof RecyclerClickable) ? (RecyclerClickable) context : null);
        textCrawler = new TextCrawler();
    }


    @Override
    public ChatLikeListAdapter.ChatLikeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_URL_PREVIEW) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_chat_message, parent, false);
            return new UrlPreviewViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_chat_message, parent, false);
            return new ChatLikeViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(final ChatLikeListAdapter.ChatLikeViewHolder holder, final int position) {
        Memo memo = mMemos.get(position);
        boolean isPro = memo.isPro();
        //to simulate whether it me or other sender
        setAlignment(holder, isPro);
        holder.numberOfTheMessage.setText("#" + memo.getId()); // TODO 20160212 getIdInTheme
        holder.setMemoText(memo);
        holder.txtInfo.setText(memo.getDate());
        if (StringUtils.isEmpty(memo.getCitationResource())) {
            holder.citationResourceContainer.setVisibility(View.GONE);
        } else {
            holder.citationResourceContainer.setVisibility(View.VISIBLE);
            holder.citationResource.setText(memo.getCitationResource());
            holder.pages.setText(memo.getPages());
            if (StringUtils.isEmpty(memo.getPages())) {
                holder.pages.setVisibility(View.GONE);
                holder.pagesMark.setVisibility(View.GONE);
            } else {
                holder.pages.setVisibility(View.VISIBLE);
                holder.pagesMark.setVisibility(View.VISIBLE);
                holder.pagesMark.setText(memo.isPages() ? "pp." : "p.");
            }
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOnClickListener != null) {
                    mOnClickListener.onRecyclerClicked(view, position);
                }
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mOnClickListener != null) {
                    return mOnClickListener.onRecyclerLongClicked(view, position);
                }
                return false;
            }
        });

    }

    @Override
    public int getItemViewType(int position) {
        Memo memo = mMemos.get(position);
        if (memo.isForUrl()) return VIEW_TYPE_URL_PREVIEW;
        return VIEW_TYPE_NORMAL_BUBBLE;
    }

    @Override
    public long getItemId(int position) {
        return mMemos == null ? -1 : mMemos.get(position).getId(); // TODO 20160210 localId
    }

    @Override
    public int getItemCount() {
        if (mMemos != null) {
            return mMemos.size();
        } else {
            return 0;
        }
    }

    private void setAlignment(ChatLikeViewHolder holder, boolean isMe) {
        if (!isMe) {
            holder.contentWithBG.setBackgroundResource(R.drawable.in_message_bg);

            LinearLayout.LayoutParams layoutParams =
                    (LinearLayout.LayoutParams) holder.contentWithBG.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            holder.contentWithBG.setLayoutParams(layoutParams);

            RelativeLayout.LayoutParams lp =
                    (RelativeLayout.LayoutParams) holder.content.getLayoutParams();
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            holder.content.setLayoutParams(lp);
            layoutParams = (LinearLayout.LayoutParams) holder.txtMessage.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            holder.txtMessage.setLayoutParams(layoutParams);

            layoutParams = (LinearLayout.LayoutParams) holder.txtInfo.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            holder.txtInfo.setLayoutParams(layoutParams);
        } else {
            holder.contentWithBG.setBackgroundResource(R.drawable.out_message_bg);

            LinearLayout.LayoutParams layoutParams =
                    (LinearLayout.LayoutParams) holder.contentWithBG.getLayoutParams();
            layoutParams.gravity = Gravity.LEFT;
            holder.contentWithBG.setLayoutParams(layoutParams);

            RelativeLayout.LayoutParams lp =
                    (RelativeLayout.LayoutParams) holder.content.getLayoutParams();
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            holder.content.setLayoutParams(lp);
            layoutParams = (LinearLayout.LayoutParams) holder.txtMessage.getLayoutParams();
            layoutParams.gravity = Gravity.LEFT;
            holder.txtMessage.setLayoutParams(layoutParams);

            layoutParams = (LinearLayout.LayoutParams) holder.txtInfo.getLayoutParams();
            layoutParams.gravity = Gravity.LEFT;
            holder.txtInfo.setLayoutParams(layoutParams);
        }
    }


    public static class ChatLikeViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout citationResourceContainer;
        public TextView txtMessage;
        public TextView txtInfo;
        public LinearLayout content;
        public LinearLayout contentWithBG;
        public TextView citationResource;
        public TextView pages;
        public TextView pagesMark;
        public TextView numberOfTheMessage;

        public ChatLikeViewHolder(View v) {
            super(v);
            numberOfTheMessage = (TextView) v.findViewById(R.id.txtInfoNumber);
            txtMessage = (TextView) v.findViewById(R.id.txtMessage);
            content = (LinearLayout) v.findViewById(R.id.content);
            contentWithBG = (LinearLayout) v.findViewById(R.id.contentWithBackground);
            txtInfo = (TextView) v.findViewById(R.id.txtInfo);
            citationResource = (TextView) v.findViewById(R.id.message_source);
            pagesMark = (TextView) v.findViewById(R.id.message_source_pages_mark);
            pages = (TextView) v.findViewById(R.id.message_source_pages);
            citationResourceContainer = (LinearLayout) v.findViewById(R.id.citation_resource_container);
        }

        public void setMemoText(Memo memo) {
            txtMessage.setText(memo.getMemo());
        }
    }


    public class UrlPreviewViewHolder extends ChatLikeViewHolder {

        public UrlPreviewViewHolder(View v) {
            super(v);
        }

        @Override
        public void setMemoText(Memo memo) {
            String memoText = memo.getMemo();
            if (StringUtils.isEmpty(memoText)) {
                txtMessage.setText(memo.isLoaded() ? "読込失敗" : "読込中");
            } else {
                txtMessage.setText(memoText);
            }
        }
    }

}
