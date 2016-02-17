package jp.ne.hatena.hackugyo.procon.ui.fragment;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.KeyEvent;

import jp.ne.hatena.hackugyo.procon.util.LogUtils;

public class ProgressDialogFragment extends AppCompatDialogFragment {
    public static final String TITLE = "title";
    public static final String MESSAGE = "message";
    public static final String IS_CANCELABLE = "is_cancelable";
    public static final String NEGATIVE_TEXT = "negative_text";
    public static final String TARGET_LOADER_ID = "target_loader_id";
    private final ProgressDialogFragment self = this;
    private ProgressCallbacks mCallbacks;

    /**
     * ファクトリーメソッド
     *
     * @param args
     * @param title
     * @param message
     */
    public static ProgressDialogFragment createProgressDialog(Bundle args, String title, String message) {
        ProgressDialogFragment dialog = new ProgressDialogFragment();
        if (args == null) args = new Bundle();
        args.putString(ProgressDialogFragment.TITLE, title);
        args.putString(ProgressDialogFragment.MESSAGE, message);
        dialog.setArguments(args);
        return dialog;
    }

    public static ProgressDialogFragment createUncancelableProgressDialog(Bundle args, String title, String message) {
        ProgressDialogFragment dialog = new ProgressDialogFragment();
        if (args == null) args = new Bundle();
        args.putString(ProgressDialogFragment.TITLE, title);
        args.putString(ProgressDialogFragment.MESSAGE, message);
        args.putBoolean(IS_CANCELABLE, false);
        dialog.setArguments(args);
        return dialog;
    }

    /***********************************************
     * Life Cycle *
     ***********************************************/
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        Bundle args = getArguments();
        if (args.containsKey(TITLE) && args.getString(TITLE) != null) dialog.setTitle(args.getString(TITLE));
        if (args.containsKey(MESSAGE) && args.getString(MESSAGE) != null) dialog.setMessage(args.getString(MESSAGE));
        boolean isCancelable = args.getBoolean(IS_CANCELABLE, true);
        dialog.setCancelable(isCancelable);
        if (!isCancelable) { // キャンセル不可の場合，KEYCODE_SEARCHも無効にしておく
            dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_BACK) return true; // ignore
                    return false;
                }
            });
        }
        if (args.containsKey(NEGATIVE_TEXT) && args.getString(NEGATIVE_TEXT) != null) {
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, args.getString(NEGATIVE_TEXT), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    onCancel(dialogInterface);
                }
            });
        }
        // ダイアログの外側をタップした場合、キャンセルしないように対応
        // Android4.0だとキャンセルされるのが標準なので明示的に指定する
        dialog.setCanceledOnTouchOutside(false);
        mCallbacks = setCallbacks();
        return dialog;
    }

    /**
     * {@link AppCompatDialogFragment#onCancel(DialogInterface)}
     */
    @Override
    public void onCancel(DialogInterface dialog) {
        getCallbacks().onProgressCancelled(self.getTag(), getArguments());
        super.onCancel(dialog);
    }

    /**
     * このフラグメントを呼び出したActivityまたはFragmentにCallbacksが実装されていれば，セットします．
     *
     */
    protected ProgressCallbacks setCallbacks() {
        // TargetFragment - Activityの順でコールバックが実装されているかどうかをチェックし, 使用する. されていないならば何もしない.
        // （負担になるので, コールバック実装の強制はしない事にする）
        if (getTargetFragment() != null && getTargetFragment() instanceof ProgressCallbacks) {
            mCallbacks = (ProgressCallbacks) getTargetFragment();
        } else if (getActivity() instanceof ProgressCallbacks) {
            mCallbacks = (ProgressCallbacks) getActivity();
        } else {
            LogUtils.v("No callbacks.");
            mCallbacks = new ProgressCallbacks() {

                @Override
                public void onProgressCancelled(String tag, Bundle args) {
                    // nothing to do.
                }
            };
        }
        return mCallbacks;
    }

    public ProgressCallbacks getCallbacks() {
        if (mCallbacks == null) mCallbacks = setCallbacks();
        return mCallbacks;
    }

    /***********************************************
     * Callbacks interface *
     ***********************************************/
    /**
     * プログレスダイアログに処理が行われた際のコールバックインタフェース.
     */
    public static interface ProgressCallbacks {
        /**
         * プログレスダイアログがキャンセルされた際のコールバック処理
         */
        void onProgressCancelled(String tag, Bundle args);
    }
}
