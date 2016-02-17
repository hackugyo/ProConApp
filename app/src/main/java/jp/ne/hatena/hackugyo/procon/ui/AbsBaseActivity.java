package jp.ne.hatena.hackugyo.procon.ui;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import jp.ne.hatena.hackugyo.procon.CustomUncaughtExceptionHandler;
import jp.ne.hatena.hackugyo.procon.R;
import jp.ne.hatena.hackugyo.procon.ui.fragment.ProgressDialogFragment;
import jp.ne.hatena.hackugyo.procon.util.AppUtils;
import jp.ne.hatena.hackugyo.procon.util.FragmentUtils;
import jp.ne.hatena.hackugyo.procon.util.LogUtils;
import jp.ne.hatena.hackugyo.procon.util.PermissionUtils;
import jp.ne.hatena.hackugyo.procon.util.ViewUtils;

/**
 * AppCompatActivity はバグを誘発する箇所が複数あるので、その対処版となるActivityクラスを用意した
 */
public abstract class AbsBaseActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_PERMISSIONS_INTERNET = 102;
    private static final String INSTANCE_STATE_IS_SHOWING_PROGRESS_DIAGLOG = "INSTANCE_STATE_IS_SHOWING_PROGRESS_DIAGLOG";

    private final AbsBaseActivity self = this;
    /**
     *
     * FragmentManagerのBackStackが空の場合に返す，特別な文字列．
     * nullを返してしまうと，BackStackにFragmentはあるがタグがnullだった場合と区別できないため．
     * */
    protected static final String CURRENT_FRAGMENT_BACKSTACK_IS_EMPTY = "AbsFragmentActivity:empty";
    protected FragmentManager mFragmentManager;
    /** 標準APIは現在のオプションメニューを取得する手段を提供していないので、自力で実装してある */
    private Menu mMenu;

    /**
     * 更新ボタンの連打対策
     */
    private boolean mIsShowingProgressDialog = false;

    /***********************************************
     * Life Cycle *
     ***********************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // UncaughtExceptionHandlerを実装したクラスをセットする。
        CustomUncaughtExceptionHandler customUncaughtExceptionHandler;
        customUncaughtExceptionHandler = new CustomUncaughtExceptionHandler(getApplicationContext());
        Thread.setDefaultUncaughtExceptionHandler(customUncaughtExceptionHandler);

        // FragmentManagerを確保
        mFragmentManager = getSupportFragmentManager();

        // OSにキャプチャされないようにする．2.3を超えたやつに対してのみ適用
        // http://y-anz-m.blogspot.jp/2012/05/android_05.html
        // if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        // ホームボタンを有効にする．
        try {
            if (getSupportActionBar().isShowing()) {
                getSupportActionBar().setHomeButtonEnabled(true);
            }
        } catch (NullPointerException e) {
            // ignore.
        }
        mIsShowingProgressDialog = savedInstanceState == null ? false : savedInstanceState.getBoolean(INSTANCE_STATE_IS_SHOWING_PROGRESS_DIAGLOG, false);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mIsShowingProgressDialog = savedInstanceState == null ? false : savedInstanceState.getBoolean(INSTANCE_STATE_IS_SHOWING_PROGRESS_DIAGLOG, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(INSTANCE_STATE_IS_SHOWING_PROGRESS_DIAGLOG, mIsShowingProgressDialog);
    }

    /**
     * メモリ解放処理を行います．<br>
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            ViewUtils.cleanupView(getContentView());
            LogUtils.v("cleaned up this view: " + self.getClass().getSimpleName());
        } catch (Exception e) { // onDestroy時に落ちてほしくないので，お守り
            LogUtils.e(e.toString());
            if (AppUtils.isDebuggable(self)) e.printStackTrace();
        }
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        try {
            return super.onCreatePanelMenu(featureId, menu);
        } catch (NullPointerException e) {
            if (AppUtils.isDebuggable(self)) e.printStackTrace();
            LogUtils.e("onCreatePanelMenu failed");
            return true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        return true;
    }

    protected Menu getCurrentOptionsMenu() {
        return mMenu;
    }

    /***********************************************
     * Fragment Control *
     **********************************************/
    /**
     * タグで指定されたフラグメントを消去します
     *
     * @param fragmentTag
     */
    protected boolean removeFragment(String fragmentTag) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        if (mFragmentManager == null) mFragmentManager = getSupportFragmentManager();
        if (mFragmentManager == null) {
            LogUtils.w("fragment manager not found.");
            return false;
        }
        Fragment prev = mFragmentManager.findFragmentByTag(fragmentTag);
        if (prev == null) {
            LogUtils.v("  not found: " + fragmentTag);
            return false;
        }
        LogUtils.v("  found: " + fragmentTag);

        if (prev instanceof DialogFragment) {
            final Dialog dialog = ((DialogFragment) prev).getDialog();

            if (dialog != null && dialog.isShowing()) {
                // 最新のソースだと，#onDismissではdialogそのものをdismissする前にフラグを見て抜けてしまうので，
                // dialog自体は別途dismiss()してやるのが確実．
                dialog.dismiss(); // http://blog.zaq.ne.jp/oboe2uran/article/876/
                // prev.dismiss()を呼んではだめ． http://memory.empressia.jp/article/44110106.html
                ((DialogFragment) prev).onDismiss(dialog); // DialogFragmentの場合は閉じる処理も追加
            }
        }

        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.remove(prev);
        // ft.commit();
        ft.commitAllowingStateLoss();
        ft = null;
        getSupportFragmentManager().executePendingTransactions();
        return true;
    }

    protected void removeFragment(Context contextForMainLooper, final String fragmentTag) {

        boolean isRemovedProgressDialog = removeFragment(fragmentTag);
        // callbackなので，プログレスダイアログは消したいのだが，
        // 戻りが早すぎてまだプログレスダイアログが出ていないうちにここに来てしまう場合がある．
        // そうなるとremoveFragment()してもだめなので，Handlerを作り，
        // UIスレッドが空いてから（＝プログレスダイアログが出てから）改めてremoveする．
        if (!isRemovedProgressDialog) {
            LogUtils.w("Something wrong. will retry to remove progress dialog after 1 second.");
            Handler handler = new Handler(contextForMainLooper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    removeFragment(fragmentTag);
                }
            }, 1 * 1000l);
        }
    }

    protected boolean isShowingSameDialogFragment(String fragmentTag) {
        return FragmentUtils.isShowingSameDialogFragment(mFragmentManager, fragmentTag);
    }

    /**
     * onLoadFinished()の中などからでも，安全にDialogFragmentをshow()します．
     *
     * @param fragment
     */
    public void showDialogFragment(DialogFragment fragment, final String tag) {
        try {
            removeFragment(tag);
            fragment.show(mFragmentManager, tag);
            return;
        } catch (IllegalStateException e) {
            // ignore. use handler.
            // IllegalStateException: Can not perform this action after onSaveInstanceState
            showDialogFragmentByHandler(fragment, tag);
        }
    }

    private void showDialogFragmentByHandler(final DialogFragment fragment, final String tag) {
        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                removeFragment(tag);
                if (mFragmentManager == null) mFragmentManager = getSupportFragmentManager();
                try {
                    fragment.show(mFragmentManager, tag);
                } catch (NullPointerException e) {
                    LogUtils.e(tag + ": cannot get SupportFragmentManager.");
                    LogUtils.e(self.getClass().getSimpleName() + e.getMessage());
                } catch (IllegalStateException e) {
                    LogUtils.e(tag + ": cannot show a dialog when this activity is in background.");
                    LogUtils.e(self.getClass().getSimpleName() + e.getMessage());
                }
            }
        });
    }

    /**
     * 現在BackStackのもっとも上にあるFragmentのタグを返します．<br>
     *
     * @return タグ<br>
     *         （BackStackが空の場合，
     *         {@link AbsBaseActivity#CURRENT_FRAGMENT_BACKSTACK_IS_EMPTY}．<br>
     *         タグがnullの場合にのみnullを返すので注意）
     */
    protected String getCurrentFragmentTag() {
        if (isCurrentBackStackEmpty()) return CURRENT_FRAGMENT_BACKSTACK_IS_EMPTY;
        FragmentManager fm = getSupportFragmentManager();
        return fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName();
    }

    protected boolean isCurrentBackStackEmpty() {
        FragmentManager fm = getSupportFragmentManager();
        int backStackEntryCount = fm.getBackStackEntryCount();
        return (backStackEntryCount == 0);
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= 11) {
            if (getFragmentManager().popBackStackImmediate()) return;
        }
        super.onBackPressed();
    }

    /***********************************************
     * intent handling *
     **********************************************/

    /**
     * 外部ブラウザを選択させて表示します．<br>
     * Andorid4.0以降，外部ブラウザが端末にインストールされていない場合があるため，<br>
     * このメソッドを利用することを推奨します．<br>
     *
     * @param url
     */
    public void launchExternalBrowser(String url) {
        selectBrowser(url);
    }

    /***********************************************
     * ブラウザ起動*
     ***********************************************/
    protected static final int REQUEST_PICK_BROWSER = 0x1111;

    /**
     * urlを処理できるアプリ（ブラウザアプリ）の一覧を表示するchooserを出します．
     * {@link #onActivityResult(int, int, Intent)}で，選択されたアプリを起動します．
     *
     * @param url
     */
    private void selectBrowser(String url) {
        selectBrowser(url, REQUEST_PICK_BROWSER);
    }

    private void selectBrowser(String url, int requestId) {
        if (url == null) url = "";
        Intent mainIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        Intent chooserIntent = Intent.createChooser(mainIntent, "アプリケーションを選択");
        try {
            startActivityForResult(chooserIntent, requestId);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(self, "ブラウザアプリがインストールされていません。", Toast.LENGTH_LONG).show();
            LogUtils.e("browser activity cannot found.");
        }
    }

    /***********************************************
     * Activity Result Handling *
     **********************************************/
    /**
     * アプリケーション（このactivityインスタンスを含んだタスク）全体をbackgroundに入れます．
     * API16（4.1JellyBean）のfinishAffinity()の代わりです． このメソッドではActivityのfinishはしません．
     */
    protected void finishApplication() {
        moveTaskToBack(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent fromToDataIntent) {
        if (FragmentUtils.isSameRequestCode(requestCode, REQUEST_PICK_BROWSER)) {
            if (fromToDataIntent == null) return;
            fromToDataIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(fromToDataIntent);
        } else {
            super.onActivityResult(requestCode, resultCode, fromToDataIntent);
        }
    }

    /***********************************************
     * View *
     **********************************************/

    /**
     * setContentView(id)したViewを取得します．
     *
     */
    public View getContentView() {
        return ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
                return onHomeIconPressed();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * ホームアイコン（ActionBar左端）押下時の動作．
     */
    public boolean onHomeIconPressed() {
        finish();
        return true;
    }

    /***********************************************
     * orientation *
     **********************************************/

    /**
     * 画面が横向きかどうか
     *
     */
    protected boolean isLandScape() {
        int currentOrientation = getResources().getConfiguration().orientation;
        switch (currentOrientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                return true;
            case Configuration.ORIENTATION_PORTRAIT:
                // case Configuration.ORIENTATION_SQUARE:
            case Configuration.ORIENTATION_UNDEFINED:
            default:
                return false;
        }
    }

    /**
     * 画面が縦向きかどうか
     *
     */
    protected boolean isPortrait() {
        int currentOrientation = getResources().getConfiguration().orientation;
        switch (currentOrientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                return true;
            case Configuration.ORIENTATION_LANDSCAPE:
                // case Configuration.ORIENTATION_SQUARE:
            case Configuration.ORIENTATION_UNDEFINED:
            default:
                return false;
        }
    }

    /***********************************************
     * Toast *
     **********************************************/
    private Toast mToast;

    /**
     * Activity内で消し忘れがないよう，単一のToastインスタンスを使い回します．
     *
     * @param text
     * @param length
     */
    protected void showSingleToast(String text, int length) {
        if (mToast != null) mToast.cancel();
        mToast = Toast.makeText(self, text, length);
        mToast.show();
    }

    /**
     * Activity内で消し忘れがないよう，単一のToastインスタンスを使い回します．
     *
     * @param resId
     * @param length
     */
    protected void showSingleToast(int resId, int length) {
        if (mToast != null) mToast.cancel();
        mToast = Toast.makeText(self, resId, length);
        mToast.show();
    }

    /**
     * 使い回している単一のToastインスタンスを破棄します．
     *
     */
    protected void removeSingleToast() {
        if (mToast != null) mToast.cancel();
        mToast = null;
    }

    /***********************************************
     * プログレスダイアログ *
     **********************************************/
    protected static final String TAG_PROGRESS_DIALOG_DEFAULT = "TAG_PROGRESS_DIALOG_DEFAULT";

    protected boolean showProgressDialog() {
        return showProgressDialog(null, getString(R.string.message_pleasewait));
    }

    protected boolean showProgressDialog(String title, String message) {
        return showProgressDialog(TAG_PROGRESS_DIALOG_DEFAULT, title, message);
    }

    protected boolean showProgressDialog(String tag, String title, String message) {
        if (mIsShowingProgressDialog) return false;
        mIsShowingProgressDialog = true;
        ProgressDialogFragment fragment = ProgressDialogFragment.createUncancelableProgressDialog(null, title, message);
        showDialogFragment(fragment, tag);
        return true;
    }

    public void hideProgressDialog() {
        mIsShowingProgressDialog = false;
        if (isFinishing()) return;
        removeFragment(TAG_PROGRESS_DIALOG_DEFAULT);
    }

    /***********************************************
     * パーミッション確認 *
     **********************************************/

    /**
     * {@link android.Manifest.permission}が期待どおり許可されていることを確認する。<br>
     *     許可されているときに必要な処理がない場合、対応は不要（このメソッドからダイアログが出てくる）。
     *     許可が必要なPermissionは、PROTECTION_NORMALでないもの。
     * @return 許可されているかどうか
     * @see <a href="http://developer.android.com/intl/ja/guide/topics/security/normal-permissions.html">Normal Permissions</a>
     */
    protected boolean checkSelfPermission() {
        return PermissionUtils.checkSelfPermission(this, REQUEST_CODE_PERMISSIONS_INTERNET,
                // 許可がほしいものを列挙する。
                ""
                // Manifest.permission.INTERNET // 不要
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != REQUEST_CODE_PERMISSIONS_INTERNET) {
            return;
        }

        if (grantResults != null && PermissionUtils.hasGranted(grantResults)) {
            // パーミッションの使用が許可された
            doInitialNetworkAccess();
        } else {
            // 使用が拒否された時の対応。アプリを落とす。
            finishAffinity();
        }
    }

    /**
     * Permission解禁時に呼び出すメソッド。
     */
    protected void doInitialNetworkAccess() {
        // override me.
    }
}
