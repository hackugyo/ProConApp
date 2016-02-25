package jp.ne.hatena.hackugyo.procon.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import jp.ne.hatena.hackugyo.procon.R;

/**
 * Created by kwatanabe on 16/02/17.
 */
public class InputDialogFragment extends AbsCustomDialogFragment {
    public static final String DEFAULT_STRING = "InputDialogFragment.DEFAULT_STRING";
    public static final String SUGGESTION_STRINGS = "InputDialogFragment.SUGGESTION_STRINGS";
    public static final String RESULT = "InputDialogFragment.RESULT";


    public static InputDialogFragment newInstance(Context context, Bundle args, String title, String msg) {
        InputDialogFragment f = new InputDialogFragment();

        args = AbsCustomDialogFragment.initializeSettings(context, args, null, null, null);
        if (!TextUtils.isEmpty(title)) args.putString(TITLE, title);
        if (!TextUtils.isEmpty(msg)) args.putString(MESSAGE, msg);
        f.setArguments(args);

        return f;
    }

    AutoCompleteTextView userInput;

    @Override
    public AlertDialog.Builder customizeBuilder(AlertDialog.Builder builder, Bundle args) {

        // LayoutInflater inflater = LayoutInflater.from(getContext());
        userInput = new AutoCompleteTextView(getActivity());
        userInput.setText(args.getString(DEFAULT_STRING, ""));
        if (args.containsKey(SUGGESTION_STRINGS)) {
            ArrayAdapter<String> suggestionAdapter = new ArrayAdapter<String>(
                    getContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    args.getStringArrayList(SUGGESTION_STRINGS)
            );
            suggestionAdapter.setNotifyOnChange(true);
            userInput.setAdapter(suggestionAdapter);
            userInput.setThreshold(1);
            userInput.setCompletionHint(getContext().getString(R.string.suggestion_hint_from));
            userInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus && userInput.getAdapter().getCount() > 0)  userInput.showDropDown();
                }
            });
        }
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
