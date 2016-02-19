package jp.ne.hatena.hackugyo.procon.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.widget.EditText;

/**
 * Created by kwatanabe on 16/02/17.
 */
public class InputDialogFragment extends AbsCustomDialogFragment {
    public static final String RESULT = "InputDialogFragment.RESULT";


    public static InputDialogFragment newInstance(Context context, Bundle args, String title, String msg) {
        InputDialogFragment f = new InputDialogFragment();

        args = AbsCustomDialogFragment.initializeSettings(context, args, null, null, null);
        if (!TextUtils.isEmpty(title)) args.putString(TITLE, title);
        if (!TextUtils.isEmpty(msg)) args.putString(MESSAGE, msg);
        f.setArguments(args);

        return f;
    }


    EditText userInput;
    @Override
    public AlertDialog.Builder customizeBuilder(AlertDialog.Builder builder, Bundle args) {

        // LayoutInflater inflater = LayoutInflater.from(getContext());
        userInput = new EditText(getActivity());
        builder.setView(userInput);

        // OK/キャンセルボタンセット．ただし，ボタン表示名に明示的にnullを指定されていた場合，そのボタンは表示しない
        if (args.containsKey(POSITIVE_TEXT) && args.getString(POSITIVE_TEXT) != null) {
            builder.setPositiveButton(args.getString(POSITIVE_TEXT), new DialogInterface.OnClickListener() {
                // ダイアログのボタンを押された時のリスナを定義する.
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Bundle arguments = getArguments();
                    arguments.putString(RESULT, userInput.getText().toString());
                    getCallbacks().onAlertDialogClicked(getTag(), arguments, which);
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
