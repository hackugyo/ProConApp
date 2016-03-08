package jp.ne.hatena.hackugyo.procon.ui.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import jp.ne.hatena.hackugyo.procon.R;
import jp.ne.hatena.hackugyo.procon.util.LogUtils;

/**
 * 画像を画面一杯に表示するダイアログ
 */
public class ImageDialogFragment extends AbsCustomDialogFragment {


    private static final String IMAGE_URI = "_IMAGE_URI_";
    private final ImageDialogFragment self = this;
    private Picasso picasso;

    public static  ImageDialogFragment newInstance(Context context, Bundle args, String imageUriString) {
        ImageDialogFragment f = new ImageDialogFragment();

        args = AbsCustomDialogFragment.initializeSettings(context, args, null, null, null);
        args.putString(IMAGE_URI, imageUriString);
        f.setArguments(args);
        f.picasso = Picasso.with(context);

        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (picasso == null) picasso = Picasso.with(activity);
        setImage(getArguments().getString(IMAGE_URI));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (picasso == null) picasso = Picasso.with(getActivity());
        setImage(getArguments().getString(IMAGE_URI));
    }

    @Override
    public AlertDialog.Builder customizeBuilder(AlertDialog.Builder builder, Bundle args) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_image, null, false);
        builder.setView(view);
        return builder;
    }

    @Override
    public Dialog customizeDialog(Dialog dialog, Bundle args) {
        dialog = new Dialog(getActivity());
        // DialogFragmentをタイトル無しにします
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.fragment_image);
        // 背景を透明にします
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        // dim部分
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        dialog.findViewById(R.id.fragment_image_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                self.dismiss();
            }
        });

        setImage(getArguments().getString(IMAGE_URI));
        return dialog;
    }

    private boolean setImage(String uriString) {
        if (getDialog() == null) return false;
        ImageView imageView = (ImageView) getDialog().findViewById(R.id.fragment_image);
        if (picasso == null) return false;
        picasso.cancelRequest(imageView);
        picasso.load(uriString).into(imageView, new Callback() {
            @Override
            public void onSuccess() {
                if (getDialog() == null) return;
                ProgressBar progress = (ProgressBar) getDialog().findViewById(R.id.fragment_image_progress);
                progress.setVisibility(View.GONE);
            }

            @Override
            public void onError() {
                if (getDialog() == null) return;
                LogUtils.w("Cannot load any image from : " + getArguments().getString(IMAGE_URI));
                ProgressBar progress = (ProgressBar) getDialog().findViewById(R.id.fragment_image_progress);
                progress.setVisibility(View.GONE);
                self.dismiss();
                FragmentActivity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity, R.string.error_image_not_found, Toast.LENGTH_SHORT).show();
                }

            }
        });
        return true;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        ImageView imageView = (ImageView)  ((Dialog)dialog).findViewById(R.id.fragment_image);
        if (imageView != null && picasso != null) {
            picasso.cancelRequest(imageView);
        }
        picasso = null;
    }
}
