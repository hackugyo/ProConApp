package jp.ne.hatena.hackugyo.procon.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 選択肢から1つを選ぶダイアログ。
 */
public class ChoiceDialogFragment extends AbsCustomDialogFragment {

    private static final String CHOICES = "ChoiceDialogFragment.CHOICES";
    public static ChoiceDialogFragment newInstance(Context context, Bundle args, String title, String msg, List<String> choices) {
        ChoiceDialogFragment f = new ChoiceDialogFragment();
        if (args == null) args = new Bundle();
        if (!args.containsKey(POSITIVE_TEXT)) args.putString(POSITIVE_TEXT, null); // 特記なきかぎり、OKボタン非表示
        if (!args.containsKey(NEGATIVE_TEXT)) args.putString(NEGATIVE_TEXT, "閉じる"); // 特記なきかぎり、キャンセルボタン名称変更
        args = AbsCustomDialogFragment.initializeSettings(context, args, null, null, null);
        if (!TextUtils.isEmpty(title)) args.putString(TITLE, title);
        if (!TextUtils.isEmpty(msg)) args.putString(MESSAGE, msg);

        args.putStringArrayList(CHOICES, new ArrayList<String>(choices));
        f.setArguments(args);

        return f;
    }

    @Override
    public AlertDialog.Builder customizeBuilder(AlertDialog.Builder builder, final Bundle args) {
        ArrayList<String> items = args.getStringArrayList(CHOICES);
        String[] names = new String[items.size()];
        items.toArray(names);

        builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                getCallbacks().onAlertDialogClicked(getTag(), getArguments(), which);
            }
        });
        return builder;
    }

    @Override
    public Dialog customizeDialog(Dialog dialog, Bundle args) {
        return dialog;
    }
}
