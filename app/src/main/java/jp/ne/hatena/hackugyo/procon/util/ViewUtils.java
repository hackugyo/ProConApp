package jp.ne.hatena.hackugyo.procon.util;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;

import java.lang.reflect.Method;

public class ViewUtils {
    /**
     * {@link View#setBackgroundDrawable(Drawable)}
     * がdeprecatedになったので，SDK_INTによって処理を変えます．
     *
     * @param view
     * @param drawable
     * @return 第1引数のView
     */
    @SuppressWarnings({ "deprecation", "javadoc" })
    @SuppressLint("NewApi")
    public static View setBackgroundDrawable(View view, Drawable drawable) {
        if (view == null) return null;
        if (drawable == null) {
            view.setBackgroundResource(0);
            return view;
        }
        if (Build.VERSION.SDK_INT >= 16) {
            view.setBackground(drawable);
        } else {
            view.setBackgroundDrawable(drawable);
        }
        return view;
    }

    public static View setBackgroundBitmap(View view, Bitmap bitmap) {
        if (view == null) return null;
        BitmapDrawable drawable = new BitmapDrawable(view.getResources(), bitmap);
        return setBackgroundDrawable(view, drawable);
    }

    /**
     * 指定したビュー階層内の{@link Drawable}をクリアします． {@link Bitmap#recycle}
     *
     * @param view
     */
    public static final void cleanupView(View view) {
        if (view == null) return;
        cleanupViewWithImage(view);
        cleanUpOnClickListeners(view);
        cleanUpAdapter(view);
        // 再帰的に処理
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            int size = vg.getChildCount();
            for (int i = 0; i < size; i++) {
                cleanupView(vg.getChildAt(i));
            }
        }
        // TODO 20160118 おそらく、RecyclerViewの解放に失敗ししている
    }

    /**
     * Viewをきれいにする．（Image・Backgroundなど）<br>
     * {@link Drawable}のrecycleはしていません．
     *
     * @param view
     * @return 引数のviewを返す
     */
    public static View cleanupViewWithImage(View view) {
        if (view == null) return null;
        if (view instanceof ImageButton) {
            ImageButton ib = (ImageButton) view;
            ib.setImageDrawable(null);
        } else if (view instanceof ImageView) {
            ImageView iv = (ImageView) view;
            iv.setImageDrawable(null);
        } else if (view instanceof SeekBar) {
            SeekBar sb = (SeekBar) view;
            sb.setProgressDrawable(null);
            sb.setThumb(null);
            // } else if(view instanceof( xxxx )) {  -- 他にもDrawableを使用するUIコンポーネントがあれば追加
        }
        return setBackgroundDrawable(view, null);
    }

    /**
     * OnClickListenerを解放する．
     *
     * @param view
     * @see <a
     *      href="http://htomiyama.blogspot.jp/2012/08/androidoutofmemoryerror.html">参考ページ</a>
     */
    @SuppressWarnings({ "rawtypes" })
    private static void cleanUpOnClickListeners(View view) {
        if (view == null) return;
        if (view instanceof AdapterView) {
            // AdapterView（ListViewのような）に対しては，setOnClickListener()を呼んではいけない．
            ((AdapterView) view).setOnItemClickListener(null);
            ((AdapterView) view).setOnItemLongClickListener(null);
            ((AdapterView) view).setOnItemSelectedListener(null);
        } else {
            view.setOnClickListener(null);
            view.setOnLongClickListener(null);
            view.setOnTouchListener(null);
        }
    }

    /**
     * Adapterを解放する．
     *
     * @param view
     * @see <a
     *      href="http://htomiyama.blogspot.jp/2012/08/androidoutofmemoryerror.html">参考ページ</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void cleanUpAdapter(View view) {
        if (view == null) return;
        if (view instanceof AdapterView) {
            try {
                ((AdapterView) view).setAdapter(null);
            } catch (IllegalArgumentException e) {
                LogUtils.v(e.toString()); // CustomなAdapterの場合など，nullをセットすると失敗する場合があるので
            } catch (NullPointerException e) {
                LogUtils.v(e.toString()); // CustomなAdapterの場合など，nullをセットすると失敗する場合があるので
            }
        }
    }

    /**
     * {@link Drawable}をrecycleします．<br>
     * これを使ったら，利用しているViewに対して直後に{@link #cleanupViewWithImage(View)}を呼んでください．<br>
     * 同じbitmapを使っているdrawableが複数存在する場合があるので，注意して呼び出してください．<br>
     *
     * @param drawable
     */
    public static void recycleDrawable(Drawable drawable) {
        if (drawable == null) return;
        if (!(drawable instanceof BitmapDrawable)) return;
        BitmapDrawable bd = (BitmapDrawable) drawable;
        Bitmap bitmap = bd.getBitmap();
        if (bitmap != null) bitmap.recycle();
    }

    public static void setActivated(View view, boolean activated) {
        view.setActivated(activated);
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setActivated(((ViewGroup) view).getChildAt(i), activated);
            }
        }
    }

    /**
     * Call this view's and this view's child's OnClickListener, <br>
     * if it is defined. <br>
     * Performs all normal actions associated with clicking: <br>
     * reporting accessibility event, playing a sound, etc.
     *
     * @param view
     * @return True there was an assigned OnClickListener that was called, false
     *         otherwise is returned.
     */
    public static boolean performClick(View view) {
        if (view == null) return false;
        boolean result = view.performClick();
        if (!result && (view instanceof ViewGroup)) {
            int count = ((ViewGroup) view).getChildCount();
            for (int i = 0; i < count; i++) {
                result = ((ViewGroup) view).getChildAt(i).performClick();
                if (result) break;
            }
        }
        return result;
    }


    /**
     * {@link ViewTreeObserver#removeGlobalOnLayoutListener(android.view.ViewTreeObserver.OnGlobalLayoutListener)}
     * がdeprecatedになったので，SDK_INTによって処理を変えます．
     *
     * @param obs
     * @param listener
     */
    @SuppressLint("NewApi")
    @SuppressWarnings({ "deprecation", "javadoc" })
    public static void removeGlobalOnLayoutListener(ViewTreeObserver obs, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (obs == null) return;
        if (Build.VERSION.SDK_INT < 16) {
            obs.removeGlobalOnLayoutListener(listener);
        } else {
            obs.removeOnGlobalLayoutListener(listener);
        }
    }

    /**
     * {@link Display#getWidth()} がdeprecatedになったので，SDK_INTによって処理を変えます．
     *
     * @param display
     * @return displayの幅（displayがnullのとき0)
     */
    @SuppressWarnings({ "deprecation", "javadoc" })
    public static int getDisplayWidth(Display display) {
        if (display == null) return 0;
        Point outSize = new Point();
        try {
            // test for new method to trigger exception
            @SuppressWarnings("rawtypes")
            Class pointClass = Class.forName("android.graphics.Point");
            Method newGetSize = Display.class.getMethod("getSize", new Class[] { pointClass });

            // no exception, so new method is available, just use it
            newGetSize.invoke(display, outSize);
        } catch (Exception ex) {
            // new method is not available, use the old ones
            outSize.x = display.getWidth();
        }
        return outSize.x;
    }

    /**
     * {@link Display#getHeight()} がdeprecatedになったので，SDK_INTによって処理を変えます．
     *
     * @param display
     * @return displayの幅（displayがnullのとき0)
     */
    @SuppressWarnings({ "deprecation", "javadoc" })
    public static int getDisplayHeight(Display display) {
        if (display == null) return 0;
        Point outSize = new Point();
        try {
            // test for new method to trigger exception
            @SuppressWarnings("rawtypes")
            Class pointClass = Class.forName("android.graphics.Point");
            Method newGetSize = Display.class.getMethod("getSize", new Class[] { pointClass });

            // no exception, so new method is available, just use it
            newGetSize.invoke(display, outSize);
        } catch (Exception ex) {
            // new method is not available, use the old ones
            outSize.y = display.getHeight();
        }
        return outSize.y;
    }

    public static <T extends View> T setLayoutHeight(T view, int height) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = height;
        view.setLayoutParams(layoutParams);
        return view;
    }

    public static <T extends View> T setLayoutWidth(T view, int width) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = width;
        view.setLayoutParams(layoutParams);
        return view;
    }

    public static int getPxFromDp(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
