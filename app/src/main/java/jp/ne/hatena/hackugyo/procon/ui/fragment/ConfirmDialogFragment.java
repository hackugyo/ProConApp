package jp.ne.hatena.hackugyo.procon.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.KeyEvent;

/**
 * Created by kwatanabe on 15/12/15.
 */
public class ConfirmDialogFragment extends AbsCustomDialogFragment {

    public static ConfirmDialogFragment newInstance(Context context, Bundle args, String title, String msg) {
        return newInstance(context, args, title, msg, null, null);
    }

    public static ConfirmDialogFragment newInstance(Context context, Bundle args, String title, String msg, String positive, String negative) {
        ConfirmDialogFragment f = new ConfirmDialogFragment();

        args = AbsCustomDialogFragment.initializeSettings(context, args, null, null, null);
        if (!TextUtils.isEmpty(title)) args.putString(TITLE, title);
        if (!TextUtils.isEmpty(msg)) args.putString(MESSAGE, msg);
        if (!TextUtils.isEmpty(positive)) args.putString(POSITIVE_TEXT, positive);
        if (!TextUtils.isEmpty(negative)) args.putString(NEGATIVE_TEXT, negative);
        f.setArguments(args);

        return f;
    }

    @Override
    public AlertDialog.Builder customizeBuilder(AlertDialog.Builder builder, Bundle args) {
        if (!args.getBoolean(IS_CANCELABLE, false)) {
            builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    return (keyCode == KeyEvent.KEYCODE_BACK ||
                            keyCode == KeyEvent.KEYCODE_MENU ||
                            keyCode == KeyEvent.KEYCODE_SEARCH);
                }
            });
        }
        return builder;
    }

    @Override
    public Dialog customizeDialog(Dialog dialog, Bundle args) {
        // ダイアログの外側をタッチされても、勝手に閉じないようにした
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }


}
