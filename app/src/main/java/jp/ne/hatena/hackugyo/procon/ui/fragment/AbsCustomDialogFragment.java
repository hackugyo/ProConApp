package jp.ne.hatena.hackugyo.procon.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import jp.ne.hatena.hackugyo.procon.R;
import jp.ne.hatena.hackugyo.procon.util.LogUtils;

/**
 * Created by kwatanabe on 15/12/15.
 */
abstract public class AbsCustomDialogFragment extends AppCompatDialogFragment{
    public static final String ICON = "icon";
    public static final String TITLE = "title";
    public static final String MESSAGE = "message";
    public static final String POSITIVE_TEXT = "positive_text";
    public static final String NEGATIVE_TEXT = "negative_text";
    public static final String ALERTDIALOG_VIEW = "alert_dialog_view";
    public static final String IS_MESSAGE_CENTERED = "is_message_centered";
    @SuppressWarnings("unused")
    private final AbsCustomDialogFragment self = this;

    /** コールバック. */
    private Callbacks mCallbacks;
    /**
     * キャンセル可否．falseにした場合，{@link #NEGATIVE_TEXT}は表示されません． <br>
     * 設定しない場合（デフォルトの場合）キャンセル不可．
     * */
    public static final String IS_CANCELABLE = "IS_CANCELABLE";

    /***********************************************
     * Life Cycle *
     ***********************************************/
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();

        AlertDialog.Builder builder = getBuilder();
        setBuilderDefaultSettings(builder, args);
        builder = customizeBuilder(builder, args);// この位置で，builderに追加設定
        if (builder == null) throw new NullPointerException("AbsCustomAlertDialogFragment#customizeBuilder(builder, args) is not implemented,");// org.apache.commons.lang.NotImplementedException();
        Dialog dialog = builder.create();
        dialog = customizeDialog(dialog, args); // この位置で，Windowに追加設定
        if (dialog == null) throw new NullPointerException("AbsCustomAlertDialogFragment#cutomizeDialog(dialog, args) is not implemented,");// org.apache.commons.lang.NotImplementedException();

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getArguments() != null && getArguments().getBoolean(IS_MESSAGE_CENTERED, false)) {
            TextView messageView = (TextView) getDialog().findViewById(android.R.id.message);
            if (messageView != null) messageView.setGravity(Gravity.CENTER_HORIZONTAL);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mCallbacks.onAlertDialogCancelled(getTag(), getArguments());
    }

    /***********************************************
     * Builder *
     ***********************************************/
    /**
     * ビルダークラスを取得します．
     *
     * @return {@link android.app.AlertDialog.Builder}またはその子クラス
     */
    protected AlertDialog.Builder getBuilder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        return builder;
    }

    /**
     * デフォルトで利用する情報をargsから取得してビルドします．
     *
     * @param args
     */
    protected AlertDialog.Builder setBuilderDefaultSettings(AlertDialog.Builder builder, Bundle args) {
        if (args.containsKey(ICON)) builder.setIcon(args.getInt(ICON));
        if (args.containsKey(TITLE)) builder.setTitle(args.getString(TITLE));
        if (args.containsKey(MESSAGE)) builder.setMessage(args.getString(MESSAGE));
        mCallbacks = setCallbacks();

        // OK/キャンセルボタンセット．ただし，ボタン表示名に明示的にnullを指定されていた場合，そのボタンは表示しない
        if (args.containsKey(POSITIVE_TEXT) && args.getString(POSITIVE_TEXT) != null) {
            builder.setPositiveButton(args.getString(POSITIVE_TEXT), new DialogInterface.OnClickListener() {
                // ダイアログのボタンを押された時のリスナを定義する.
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mCallbacks.onAlertDialogClicked(getTag(), getArguments(), which);
                }
            });
        }
        if (args.containsKey(NEGATIVE_TEXT) && args.getString(NEGATIVE_TEXT) != null) {
            if (!args.getBoolean(IS_CANCELABLE, true)) {
                LogUtils.w("negative button may be not match with not isCancelable.");
            }
            builder.setNegativeButton(args.getString(NEGATIVE_TEXT), new DialogInterface.OnClickListener() {
                // ダイアログのボタンを押された時のリスナを定義する.
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // onCancelでCallbacksも呼んでいる．
                    onCancel(dialog);
                }
            });
        }
        if (!args.getBoolean(IS_CANCELABLE, true)) {
            builder.setCancelable(false); // キャンセルできなくする
            builder.setOnKeyListener(new DialogInterface.OnKeyListener() { // KEYCODE_SEARCHも無効にする
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_BACK) return true; // ignore
                    return false;
                }
            });
        }

        if (args.containsKey(ALERTDIALOG_VIEW)) {
            View layout = getActivity().getLayoutInflater().inflate(args.getInt(ALERTDIALOG_VIEW), null, false);
            builder.setView(layout);
        }
        return builder;
    }

    /**
     *
     */
    protected Callbacks setCallbacks() {
        // TargetFragment - Activityの順でコールバックが実装されているかどうかをチェックし, 使用する. されていないならば何もしない.
        // （負担になるので, コールバック実装の強制はしない事にする）
        if (getTargetFragment() != null && getTargetFragment() instanceof Callbacks) {
            // mCallbacks = (Callbacks) getTargetFragment();
            // ラップする
            mCallbacks = getFragmentWrappedCallbacks();
        } else if (getActivity() instanceof Callbacks) {
            mCallbacks = getActivityWrappedCallbacks();
        } else {
            mCallbacks = new AbsCustomDialogFragment.Callbacks() {
                @Override
                public void onAlertDialogClicked(String tag, Bundle args, int which) {
                    onAlertDialogClickedInner(tag, args, which);
                }

                @Override
                public void onAlertDialogCancelled(String tag, Bundle args) {
                    onAlertDialogCanceledInner(tag, args);
                }
            };
        }
        return mCallbacks;
    }

    /**
     * 基本的な設定を受け取り，argsにまとめて返します．
     *
     * @param args
     * @param title
     * @param message
     * @param layoutId
     */
    protected static Bundle initializeSettings(Context context, Bundle args, String title, String message, Integer layoutId) {
        if (args == null) args = new Bundle();

        if (title != null) args.putString(TITLE, title);
        if (message != null) args.putString(MESSAGE, message);
        if (layoutId != null) args.putInt(ALERTDIALOG_VIEW, layoutId);
        if (!args.containsKey(POSITIVE_TEXT)) args.putString(POSITIVE_TEXT, context.getResources().getString(R.string.Dialog_Button_Label_Positive));
        if (!args.containsKey(NEGATIVE_TEXT)) args.putString(NEGATIVE_TEXT, context.getResources().getString(R.string.Dialog_Button_Label_Negative));
        return args;
    }

    protected Callbacks getCallbacks() {
        return mCallbacks;
    }

    /**
     * コールバックを明示的に指定します．
     * {@link #customizeBuilder(AlertDialog.Builder, Bundle)}
     * 内で呼んでください．
     *
     * @param callbacksInstance
     */
    public void setCallbacks(Callbacks callbacksInstance) {
        mCallbacks = callbacksInstance;
    }

    abstract public AlertDialog.Builder customizeBuilder(AlertDialog.Builder builder, Bundle args);

    abstract public Dialog customizeDialog(Dialog dialog, Bundle args);

    /***********************************************
     * Callbacks interface *
     ***********************************************/
    /**
     * ダイアログの各ボタンを押下した際のコールバックインタフェース.
     */
    public static interface Callbacks {

        /**
         * ダイアログのボタン及びリストを押下した際のイベント処理.
         *
         * @param tag
         *            Fragmentにつけたタグ
         * @param args
         *            setParamsで渡されたパラメータ
         * @param which
         *            DialogInterfaceのID
         */
        void onAlertDialogClicked(String tag, Bundle args, int which);

        /**
         * ダイアログがキャンセルされた際のイベント処理.
         *
         * @param tag
         *            Fragmentのタグ
         * @param args
         *            setParamsで渡されたパラメータ
         */
        void onAlertDialogCancelled(String tag, Bundle args);
    }

    /***********************************************
     * Wrapping Callbacks *
     **********************************************/
    protected Callbacks getFragmentWrappedCallbacks() {
        return new Callbacks() {

            @Override
            public void onAlertDialogClicked(String tag, Bundle args, int which) {
                onAlertDialogClickedInner(tag, args, which);
                ((Callbacks) getTargetFragment()).onAlertDialogClicked(tag, args, which);
            }

            @Override
            public void onAlertDialogCancelled(String tag, Bundle args) {
                onAlertDialogCanceledInner(tag, args);
                ((Callbacks) getTargetFragment()).onAlertDialogCancelled(tag, args);
            }
        };
    }

    protected Callbacks getActivityWrappedCallbacks() {
        return new Callbacks() {

            @Override
            public void onAlertDialogClicked(String tag, Bundle args, int which) {
                onAlertDialogClickedInner(tag, args, which);
                ((Callbacks) getActivity()).onAlertDialogClicked(tag, args, which);
            }

            @Override
            public void onAlertDialogCancelled(String tag, Bundle args) {
                onAlertDialogCanceledInner(tag, args);
                ((Callbacks) getActivity()).onAlertDialogCancelled(tag, args);
            }
        };
    }

    /**
     * OKがタップされた際の動きをラップしています．必要に応じてoverrideしてください．
     *
     * @param tag
     * @param args
     * @param which
     */
    protected void onAlertDialogClickedInner(String tag, Bundle args, int which) {
        // nothing to do.
    }

    /**
     * キャンセルがタップされた際の動きをラップしています．必要に応じてoverrideしてください．
     *
     * @param tag
     * @param args
     */
    protected void onAlertDialogCanceledInner(String tag, Bundle args) {
        // nothing to do.
    }

}
