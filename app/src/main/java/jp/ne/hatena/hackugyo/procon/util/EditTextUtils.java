package jp.ne.hatena.hackugyo.procon.util;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/***
 * {@link EditText }関連で誤実装しがちな箇所を切り出したユーティリティ
 */
public class EditTextUtils {

    private EditTextUtils() {

    }

    /**
     * キーボードを閉じます
     *
     * @see <a href="http://stackoverflow.com/a/17789187/2338047">参考ページ</a>
     * @param context
     * @param view
     */
    public static void closeKeyboard(Context context, View view) {

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            return;
        }

        if (context instanceof Activity) {
            // ((Activity) context).getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            View v = ((Activity) context).getCurrentFocus();
            if (v == null) {
                v = new View(((Activity) context));
            }
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
        // imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    /**
     * {@link EditText}の入力制限をかけて数値のみにします．<br>
     * {@link EditText#setInputType(int)}では，最初に表示するキーボードの種類しか制限できません．<br>
     *
     * @return {@link EditText#setFilters(InputFilter[])}に渡してください．
     */
    public static InputFilter[] createNumericInputFilter() {
        InputFilter inputFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, //
                                       int start, int end, Spanned dest, int dstart, int dend) {
                if (source.toString().matches("^[0-9]+$")) {
                    return source;
                } else {
                    return "";
                }
            }
        };
        return new InputFilter[] { inputFilter };
    }

    /**
     * {@link EditText}の入力制限をかけて英数字のみにします．<br>
     * {@link EditText#setInputType(int)}では，最初に表示するキーボードの種類しか制限できません．<br>
     *
     * @return {@link EditText#setFilters(InputFilter[])}に渡してください．
     */
    public static InputFilter[] createAlphaNumericInputFilter() {
        InputFilter inputFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, //
                                       int start, int end, Spanned dest, int dstart, int dend) {
                if (source.toString().matches("^[0-9a-zA-Z]+$")) {
                    return source;
                } else {
                    return "";
                }
            }
        };
        return new InputFilter[] { inputFilter };
    }

    public static InputFilter[] createRegularExpressionInputFilter(final String regularExpression) {
        InputFilter inputFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, //
                                       int start, int end, Spanned dest, int dstart, int dend) {
                if (source.toString().matches(regularExpression)) {
                    return source;
                } else {
                    return "";
                }
            }
        };
        return new InputFilter[] { inputFilter };
    }

    /**
     * キーボード入力可能な最大行数を指定する。ただし、EditTextが狭すぎて複数行になってしまう場合にも制限されてしまう。
     * @param editText
     * @param linesCountLimit
     * @return
     * @see <a href="http://stackoverflow.com/a/30703723">参考リンク</a>
     */
    public static EditText addNoEnterKeyListener(final EditText editText, final int linesCountLimit) {
        editText.addTextChangedListener(new TextWatcher() {
            int lastSpecialRequestsCursorPosition = 0;
            String specialRequests = "";
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                lastSpecialRequestsCursorPosition = editText.getSelectionStart();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                editText.removeTextChangedListener(this);

                if (editText.getLineCount() > linesCountLimit) {
                    editText.setText(specialRequests);
                    editText.setSelection(lastSpecialRequestsCursorPosition);
                }
                else {
                    specialRequests = editText.getText().toString();
                }

                editText.addTextChangedListener(this);
            }
        });
        return editText;
    }
}
