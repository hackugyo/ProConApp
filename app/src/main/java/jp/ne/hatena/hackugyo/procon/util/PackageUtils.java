package jp.ne.hatena.hackugyo.procon.util;


import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import java.util.List;

import jp.ne.hatena.hackugyo.procon.AppApplication;
import jp.ne.hatena.hackugyo.procon.R;

/**
 * 他アプリのパッケージ名と実体（インテント）とを変換するためのユーティリティメソッド.
 * Android 4.0以降，ブラウザアプリがすべて無効になっている端末が存在するため，その対応として，ブラウザアプリの存在確認が必須となっている．
 */
public class PackageUtils {

    public static boolean isSystemApp(ResolveInfo info) {
        return ((info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM);
    }

    /**
     * パッケージ名からアイコンを取得します．
     *
     * @param context
     *            （nullでも可）
     * @param packageName
     * @return アイコン
     */
    public static Drawable getAppIcon(Context context, String packageName) {

        if (context == null) context = AppApplication.getContext();
        if (packageName == null) return DrawableUtils.getDrawable(context, R.drawable.ic_launcher_default);
        PackageManager pm = context.getPackageManager();
        Drawable icon = null;
        try {
            icon = pm.getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            LogUtils.w(new StringBuilder("name not found: ").append(packageName).toString());
        }
        if (icon == null) {
            // FakeIt later, 見つからなかった場合に出すデフォルトのアイコンを準備
            icon = DrawableUtils.getDrawable(context, R.drawable.ic_launcher_default);
        }
        return icon;
    }

    /**
     * パッケージ名から起動インテントを取得します．
     *
     * @param context
     *            （nullでも可）
     * @param packageName
     * @return アイコン
     */
    public static Intent getIntent(Context context, String packageName) {
        if (context == null) context = AppApplication.getContext();
        PackageManager pm = context.getPackageManager();
        Intent intent = null;
        if (isBrowserIntent(packageName)) {
            // URLスキームで起動します．
            intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(packageName));
            // このアプリとは違うタスクで起動する
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else if (isInstalled(context, packageName)) {
            intent = pm.getLaunchIntentForPackage(packageName);
        } else {
            intent = getMarketIntent(packageName);
        }

        // このアプリとは違うタスクで起動する
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static boolean isBrowserIntent(String packageName) {
        if (packageName == null) return false;
        if (packageName.startsWith("https://") || packageName.startsWith("http://")) return true;
        return false;
    }

    /**
     * Google Playを起動し，パッケージ名で検索するインテントを作ります．
     *
     * @param packageName
     * @return Google Playインテント
     */
    public static Intent getMarketIntent(String packageName) {
        Uri uri = Uri.parse("market://details?id=" + packageName);
        return new Intent(Intent.ACTION_VIEW, uri);
    }

    /**
     * パッケージ名から、インストールされているかどうかを返します．
     *
     * @param context
     *            （nullでも可）
     * @param packageName
     * @return true: インストールされている
     */
    public static boolean isInstalled(Context context, String packageName) {
        if (context == null) context = AppApplication.getContext();
        PackageManager pm = context.getPackageManager();
        if (isBrowserIntent(packageName)) return true;

        if (pm == null) pm = context.getPackageManager();
        try {
            // intent = pm.getLaunchIntentForPackage(packageName);
            // なぜかthrowされないことになっている？ PackageManager.NameNotFoundException e
            // @see http://code.google.com/p/android/issues/detail?id=32328
            // かわりに，下記のメソッドを使うとよい．
            // @see http://qiita.com/items/8db3cabba86453469399
            ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            if (applicationInfo != null) return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return false;
    }

    /**
     * 暗黙的インテントを扱えるアプリが存在するかどうかチェックします．
     *
     * @param context
     *            （null可）
     * @param intent
     * @return 扱える：true
     * @see <a
     *      href="http://9ensan.com/blog/smartphone/android/implicit-intent-startactivity-activitynotfoundexception/">参考ページ</a>
     */
    public static boolean canHandleThisImplicitIntent(Context context, Intent intent) {
        if (context == null) context = AppApplication.getContext();
        PackageManager pm = context.getPackageManager();
        if (pm == null) pm = context.getPackageManager();
        List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);
        if (apps.size() > 0) return true;
        return false;
    }
}
