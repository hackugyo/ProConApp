package jp.ne.hatena.hackugyo.procon.adapter;

import android.app.Activity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.leocardz.link.preview.library.SourceContent;
import com.squareup.picasso.Picasso;

import java.util.List;

import jp.ne.hatena.hackugyo.procon.R;
import jp.ne.hatena.hackugyo.procon.model.Memo;
import jp.ne.hatena.hackugyo.procon.ui.RecyclerClickable;
import jp.ne.hatena.hackugyo.procon.util.ArrayUtils;
import jp.ne.hatena.hackugyo.procon.util.StringUtils;
import jp.ne.hatena.hackugyo.procon.util.UrlUtils;

/**
 * Created by kwatanabe on 15/08/27.
 */
public class ChatLikeListAdapter extends RecyclerView.Adapter<ChatLikeListAdapter.ChatLikeViewHolder> {

    private static final int VIEW_TYPE_NORMAL_BUBBLE = 0;
    private static final int VIEW_TYPE_URL_PREVIEW = 1;
    private final List<Memo> mMemos;
    private final RecyclerClickable mOnClickListener;
    private Activity context;

    public ChatLikeListAdapter(Activity context, List<Memo> memos, RecyclerClickable onClickListener) {
        this.context = context;
        this.mMemos = memos;
        mOnClickListener = onClickListener;
    }


    @Override
    public ChatLikeListAdapter.ChatLikeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_URL_PREVIEW) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_chat_url, parent, false);
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
                holder.pagesMark.setText(memo.hasManyPages() ? "pp." : "p.");
            }
        }
        // holder.itemViewにリスナーをつけてしまうと，右寄せにしたとき左側の空欄もタップに反応してしまう
        holder.content.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOnClickListener != null) {
                    mOnClickListener.onRecyclerClicked(view, position);
                }
            }
        });
        holder.content.setOnLongClickListener(new View.OnLongClickListener() {
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
        boolean toRight = !isMe;

        holder.contentWithBG.setBackgroundResource(toRight ? R.drawable.in_message_bg : R.drawable.out_message_bg);
        setGravityInLinearLayout(holder.contentWithBG, toRight);
        setGravityInLinearLayout(holder.txtMessage, toRight);
        setGravityInLinearLayout(holder.txtInfo, toRight);
        setGravityInRelativeLayout(holder.content, toRight);
    }

    private boolean setGravityInLinearLayout(View ll, boolean toRight) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) ll.getLayoutParams();
        if (layoutParams == null) return false;
        layoutParams.gravity = toRight ? Gravity.RIGHT : Gravity.LEFT;
        ll.setLayoutParams(layoutParams);
        return true;
    }


    private boolean setGravityInRelativeLayout(View rl, boolean toRight) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)  rl.getLayoutParams();
        if (layoutParams == null) return false;
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, toRight ? 0 : RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, toRight ? RelativeLayout.TRUE : 0);
        rl.setLayoutParams(layoutParams);
        return true;
    }

    public static class ChatLikeViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout citationResourceContainer;
        public TextView txtMessage;
        public TextView txtInfo;
        public LinearLayout content;
        public LinearLayout contentWithBG;
        public AppCompatTextView citationResource;
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
            citationResource = (AppCompatTextView) v.findViewById(R.id.message_source);
            pagesMark = (TextView) v.findViewById(R.id.message_source_pages_mark);
            pages = (TextView) v.findViewById(R.id.message_source_pages);
            citationResourceContainer = (LinearLayout) v.findViewById(R.id.citation_resource_container);
        }

        public void setMemoText(Memo memo) {
            txtMessage.setText(memo.getMemo());
        }
    }


    public class UrlPreviewViewHolder extends ChatLikeViewHolder {

        private final LinearLayout citationResourceContainerSub01;
        private final LinearLayout citationResourceContainerSub02;
        private final LinearLayout citationResourceContainerSub03;
        private final LinearLayout citationResourceContainerSub04;
        private final LinearLayout[] citationResourceContainerSubs;
        private final ImageView imageView;

        public UrlPreviewViewHolder(View v) {
            super(v);
            imageView = (ImageView) v.findViewById(R.id.image);
            citationResourceContainerSub01 = (LinearLayout) v.findViewById(R.id.citation_resource_container_sub_01);
            citationResourceContainerSub02 = (LinearLayout) v.findViewById(R.id.citation_resource_container_sub_02);
            citationResourceContainerSub03 = (LinearLayout) v.findViewById(R.id.citation_resource_container_sub_03);
            citationResourceContainerSub04 = (LinearLayout) v.findViewById(R.id.citation_resource_container_sub_04);
            citationResourceContainerSubs = new LinearLayout[] {
                    citationResourceContainerSub01,
                    citationResourceContainerSub02,
                    citationResourceContainerSub03,
                    citationResourceContainerSub04
            };
            for(LinearLayout ll : citationResourceContainerSubs) {
                ll.setVisibility(View.GONE);
            }
            imageView.setVisibility(View.GONE);

        }

        @Override
        public void setMemoText(Memo memo) {
            String memoText = memo.getMemo();
            if (StringUtils.isEmpty(memoText)) {
                txtMessage.setText(memo.isLoaded() ? "読込失敗" : "読込中");
            } else {
                txtMessage.setText(memoText);
            }

            SourceContent sourceContent = memo.getSourceContent();
            if (sourceContent != null && ArrayUtils.any(sourceContent.getImages()) && UrlUtils.isTwitterUrl(sourceContent.getFinalUrl())) {
                Picasso
                        .with(context)
                        .load(memo.getSourceContent().getImages().get(0))
                        .fit()
                        .centerInside()
                        .into(imageView);
                imageView.setVisibility(View.VISIBLE);
            } else {
                imageView.setVisibility(View.GONE);
            }
        }
    }

}
