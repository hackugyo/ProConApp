package jp.ne.hatena.hackugyo.procon;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import jp.ne.hatena.hackugyo.procon.adapter.ChatLikeListAdapter;
import jp.ne.hatena.hackugyo.procon.io.ImprovedTextCrawler;
import jp.ne.hatena.hackugyo.procon.model.ChatTheme;
import jp.ne.hatena.hackugyo.procon.model.ChatThemeRepository;
import jp.ne.hatena.hackugyo.procon.model.Memo;
import jp.ne.hatena.hackugyo.procon.model.MemoRepository;
import jp.ne.hatena.hackugyo.procon.ui.RecyclerClickable;
import jp.ne.hatena.hackugyo.procon.util.ArrayUtils;
import jp.ne.hatena.hackugyo.procon.util.FragmentUtils;
import jp.ne.hatena.hackugyo.procon.util.LogUtils;
import jp.ne.hatena.hackugyo.procon.util.StringUtils;
import jp.ne.hatena.hackugyo.procon.util.UrlUtils;

public class MainActivity extends AppCompatActivity implements RecyclerClickable {

    final ArrayList<Memo> mMemos = new ArrayList<Memo>();
    RecyclerView mListView;
    ChatLikeListAdapter mChatLikeListAdapter;
    private MemoRepository memoRepository;
    Snackbar snackbar;
    private EditText mContentEditText;
    private EditText mPagesEditText;
    private EditText mResourceEditText;
    private MainActivityHelper mainActivityHelper;
    private ChatTheme chatTheme;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //toolbar の設置
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        memoRepository = new MemoRepository(this);

        mContentEditText = (EditText) findViewById(R.id.editText);
        mResourceEditText = (EditText) findViewById(R.id.editText_from);
        mPagesEditText = (EditText) findViewById(R.id.editText_pages);
        setupAddAsProButton();
        setupAddAsConButton();

        mListView = (RecyclerView) findViewById(R.id.listView);
        mChatLikeListAdapter = new ChatLikeListAdapter(this, mMemos);
        mListView.setAdapter(mChatLikeListAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mListView.setLayoutManager(llm);

        //memo の表示
        {
            ChatThemeRepository chatThemeRepository = new ChatThemeRepository(this);
            List<ChatTheme> all = chatThemeRepository.findAll();
            if (all.size() == 0) {
                chatTheme = new ChatTheme("最初の議題");
                chatThemeRepository.save(chatTheme);
            } else {
                chatTheme = all.get(0);
            }
            chatThemeRepository.onPause();
        }
        getSupportActionBar().setTitle(chatTheme.getTitle());
        loadMemo(chatTheme);
        mainActivityHelper = new MainActivityHelper(new ImprovedTextCrawler(), mChatLikeListAdapter, mMemos);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //menu の表示
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        return super.onCreateOptionsMenu(menu);
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


    @Override
    protected void onResume() {
        super.onResume();
        memoRepository.onResume(this);
        mainActivityHelper.loadPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (memoRepository != null) memoRepository.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //icon 押下時の処置
        if (id == R.id.menu_globe) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private Button setupAddAsProButton() {

        //Button の処理
        View.OnClickListener listener = new View.
                OnClickListener() {

            @Override
            public void onClick(View v) {
                String content = mContentEditText.getText().toString();
                String citationResource =  mResourceEditText.getText().toString();
                if (StringUtils.isPresent(content) || UrlUtils.isValidUrl(citationResource)) {
                    Calendar cal = Calendar.getInstance();
                    //保存処置
                    insertMemo(content, cal, citationResource, mPagesEditText.getText().toString(), true);
                    //ListView に設置
                    mChatLikeListAdapter.notifyDataSetChanged();
                    //Keyboard の消去
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    //EditText 内のデータ消去
                    mContentEditText.getEditableText().clear();
                    mResourceEditText.getEditableText().clear();
                    mPagesEditText.getEditableText().clear();

                    mListView.smoothScrollToPosition(mChatLikeListAdapter.getItemCount() - 1);

                    mainActivityHelper.loadPreview();
                }

            }
        };

        Button viewById = (Button) findViewById(R.id.button_save_as_pro);
        viewById.setOnClickListener(listener);
        return viewById;
    }

    private Button setupAddAsConButton() {

        //Button の処理
        View.OnClickListener listener = new View.
                OnClickListener() {

            @Override
            public void onClick(View v) {
                String content = mContentEditText.getText().toString();
                String citationResource =  mResourceEditText.getText().toString();
                if (StringUtils.isPresent(content) || UrlUtils.isValidUrl(citationResource)) {
                    Calendar cal = Calendar.getInstance();
                    //保存処置
                    insertMemo(content, cal, citationResource, mPagesEditText.getText().toString(),  false);
                    //ListView に設置
                    mChatLikeListAdapter.notifyDataSetChanged();
                    //Keyboard の消去
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    //EditText 内のデータ消去
                    mContentEditText.getEditableText().clear();
                    mResourceEditText.getEditableText().clear();
                    mPagesEditText.getEditableText().clear();

                    mListView.smoothScrollToPosition(mChatLikeListAdapter.getItemCount() - 1);

                    mainActivityHelper.loadPreview();

                }

            }
        };
        Button viewById = (Button) findViewById(R.id.button_save_as_con);
        viewById.setOnClickListener(listener);
        return viewById;
    }


    private void loadMemo(ChatTheme chatTheme) {
        //database からすべてを呼び出し、メモに追加する
        List<Memo> memos = memoRepository.loadFromChatTheme(chatTheme);
        if (!ArrayUtils.any(memos)) return;
        mMemos.addAll(memos);
        mChatLikeListAdapter.notifyDataSetChanged();
        mListView.smoothScrollToPosition(mChatLikeListAdapter.getItemCount() - 1);
    }

    private void insertMemo(String text, Calendar cal, String resource, String pages, boolean isPro) {
        //memo を追加し、セーブする
        Memo memo = new Memo(cal, text, isPro);
        memo.addCitationResource(resource);
        memo.setPages(pages);
        memo.setChatTheme(chatTheme);
        if(memoRepository.save(memo)) {
            mMemos.add(memo);
        }
    }

    private void deleteMemo(int position) {
        //memo を消去する
        Memo memo = mMemos.get(position);
        if (memoRepository.delete(memo) == 1) {
            mMemos.remove(memo);
        } else {
            LogUtils.i("something wrong");
        }
    }

    /****************************************
     * {@link RecyclerClickable}
     ****************************************/

    @Override
    public void onRecyclerClicked(View v, int position) {
        //snackbar をクリックで消す処置
        if (snackbar != null) snackbar.dismiss();
        Memo memo = mMemos.get(position);
        if (memo.isForUrl()) {
            launchExternalBrowser(memo.getCitationResource());
        }
    }

    @Override
    public void onRecyclerButtonClicked(View v, int position) {
    }

    @Override
    public boolean onRecyclerLongClicked(View v, final int position) {
        // TODO 20160210 copy to clipboard
        final LinearLayout layout = (LinearLayout) findViewById(R.id.snackbar);
        //snackbar の表示
        snackbar = Snackbar.make(layout, "削除しますか", Snackbar.LENGTH_LONG)
                .setAction("削除", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // something
                        deleteMemo(position);
                        mChatLikeListAdapter.notifyDataSetChanged();
                    }
                });

        snackbar.show();
        return true;
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
            Toast.makeText(this, "ブラウザアプリがインストールされていません。", Toast.LENGTH_LONG).show();
            LogUtils.e("browser activity cannot found.");
        }
    }

}
