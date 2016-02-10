package jp.ne.hatena.hackugyo.procon.util;

import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import java.util.List;

/**
 * {@link Fragment}関連で誤実装しがちな箇所を切り出したユーティリティ
 */
public class FragmentUtils {
    /**
     * すでに同じダイアログが表示されているかどうかを返します．
     *
     * @param fragmentManager
     * @param fragmentTag
     * @return true 表示されている false 表示されていない
     */
    public static boolean isShowingSameDialogFragment(FragmentManager fragmentManager, String fragmentTag) {

        Fragment prev = fragmentManager.findFragmentByTag(fragmentTag);
        if (prev == null) return false;

        if (prev instanceof DialogFragment) {
            final Dialog dialog = ((DialogFragment) prev).getDialog();
            if (dialog != null && dialog.isShowing()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isShowingSameDialogFragment(android.app.FragmentManager fragmentManager, String fragmentTag) {
        android.app.Fragment prev = fragmentManager.findFragmentByTag(fragmentTag);
        if (prev == null) return false;

        if (prev instanceof android.app.DialogFragment) {
            final Dialog dialog = ((android.app.DialogFragment) prev).getDialog();
            if (dialog != null && dialog.isShowing()) {
                return true;
            }
        }
        return false;
    }

    /**
     * #onActi
     * vityResult(int, int, Intent) において，requestCodeが指定のものと同じかどうか判定します．
     * FragmentからstartActivityForResult()した場合， <br>
     * support packageを使う際は，requestCodeの下位16bitを除いて比較する必要があるため，このメソッドを使う必要があります．
     *
     * @see <a
     *      href="http://y-anz-m.blogspot.jp/2012/05/support-package-fragment.html">http://y-anz-m.blogspot.jp/2012/05/support-package-fragment.html</a>
     *
     * @param requestCode
     * @param targetCode
     */
    public static boolean isSameRequestCode(int requestCode, int targetCode) {
        int requestCodeFromFragment = requestCode & 0xffff;
        return (requestCode == targetCode || requestCodeFromFragment == targetCode);
    }

    /**
     * 現フラグメントを取得します
     *
     * @param fragmentManager
     * @return 現在出ているフラグメント or null
     */
    public static Fragment getCurrentFragmentFrom(FragmentManager fragmentManager) {
        if (fragmentManager != null) {
            List<Fragment> fragments = fragmentManager.getFragments();
            for (int i = fragments.size() - 1; i >= 0; i--) {
                Fragment fragment = fragments.get(i);
                if (fragment != null) {
                    return fragment;
                }
            }
        }
        return null;
    }

}
