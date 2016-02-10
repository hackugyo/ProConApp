package jp.ne.hatena.hackugyo.procon.util;


import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextViewUtils {
    private TextViewUtils() {

    }

    /**
     * 複数行で末尾を"…"にするListenerです．<br>
     * Viewに対して，{@link View#getViewTreeObserver()}を呼び，{@link ViewTreeObserver#addOnGlobalLayoutListener(ViewTreeObserver.OnGlobalLayoutListener)}でセットしてください．
     * @param view
     * @param lines
     * @return リスナ
     */
    public static ViewTreeObserver.OnGlobalLayoutListener createMultiLineEllipsizer(final TextView view, final int lines) {
        return new ViewTreeObserver.OnGlobalLayoutListener() {
            private boolean mIsPassedOnce = false;
            private final int maxLines = lines;
            private final static String REPLACER = "…";


            /**
             * http://stackoverflow.com/a/11247803/2338047
             * 2行めの末尾文字以降を"…"に置換して，いったん入れる．
             * その状態だと，改行コードを"…"に置換してしまった場合など，3行になってしまう可能性がある．
             * そこで，onGlobalLayout()に再度入れるようにしておき，
             * 3行になっていたら再調整するようにしてある．
             * 再調整時は，末尾に"…"があることを考慮し，"…"に置換する位置を変える．
             */
            @Override
            public void onGlobalLayout() {
                if(view.getLineCount() > maxLines){
                    final int lineEndIndex = view.getLayout().getLineEnd(maxLines - 1);
                    int reservingPosition =  (mIsPassedOnce ? lineEndIndex : lineEndIndex - 1);

                    final CharSequence text = view.getText();
                    if (mIsPassedOnce) {
                        if (isTooShortToReplace(text, lineEndIndex)) {
                            reservingPosition -= 1;
                        }
                    }
                    final String result = text.subSequence(0, reservingPosition) + REPLACER;
                    view.setText(result);
                    {
                        // 無限ループしないよう，mIsPassedOnceのときはリスナーを切る
                        if (mIsPassedOnce) ViewUtils.removeGlobalOnLayoutListener(view.getViewTreeObserver(), this);
                    }
                    mIsPassedOnce = true;
                } else {
                    ViewUtils.removeGlobalOnLayoutListener(view.getViewTreeObserver(), this);
                }
            }

            private boolean isTooShortToReplace(CharSequence charSequence, int lineEndIndex) {
                String str = String.valueOf(charSequence.charAt(lineEndIndex));
                if (REPLACER.equals(str)) return true;
                return isAnASCII(str);
            }


            private boolean isAnASCII(String str){
                Pattern p = Pattern.compile("^[\\p{ASCII}]$");
                Matcher m = p.matcher(str);
                return m.find();
            }
        };
    }
}
