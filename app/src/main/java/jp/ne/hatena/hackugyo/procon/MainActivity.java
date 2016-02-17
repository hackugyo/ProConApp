package jp.ne.hatena.hackugyo.procon;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import jp.ne.hatena.hackugyo.procon.adapter.ChatLikeListAdapter;
import jp.ne.hatena.hackugyo.procon.io.ImprovedTextCrawler;
import jp.ne.hatena.hackugyo.procon.model.ChatTheme;
import jp.ne.hatena.hackugyo.procon.model.ChatThemeRepository;
import jp.ne.hatena.hackugyo.procon.model.CitationResource;
import jp.ne.hatena.hackugyo.procon.model.CitationResourceRepository;
import jp.ne.hatena.hackugyo.procon.model.Memo;
import jp.ne.hatena.hackugyo.procon.model.MemoRepository;
import jp.ne.hatena.hackugyo.procon.ui.AbsBaseActivity;
import jp.ne.hatena.hackugyo.procon.ui.RecyclerClickable;
import jp.ne.hatena.hackugyo.procon.ui.fragment.AbsCustomDialogFragment;
import jp.ne.hatena.hackugyo.procon.ui.fragment.InputDialogFragment;
import jp.ne.hatena.hackugyo.procon.util.LogUtils;
import jp.ne.hatena.hackugyo.procon.util.StringUtils;
import jp.ne.hatena.hackugyo.procon.util.UrlUtils;
import rx.Observable;
import rx.functions.Func1;

public class MainActivity extends AbsBaseActivity implements RecyclerClickable, AbsCustomDialogFragment.Callbacks {

    public static final String TAG_INPUT_NEW_THEME = "MainActivity.TAG_INPUT_NEW_THEME";

    final ArrayList<Memo> mMemos = new ArrayList<Memo>();
    RecyclerView mListView;
    ChatLikeListAdapter mChatLikeListAdapter;
    private MemoRepository memoRepository;
    Snackbar snackbar;
    private EditText mContentEditText;
    private EditText mPagesEditText;
    private AutoCompleteTextView mResourceEditText;
    private MainActivityHelper mainActivityHelper;
    private ChatTheme chatTheme;
    private ArrayAdapter<String> mCitationResourceSuggestionAdapter;
    private final List<ChatTheme> chatThemeList = new ArrayList<>();
    private final List<String> citationResouces = new ArrayList<>();
    private ChatThemeRepository chatThemeRepository;
    private CitationResourceRepository citationResourceRepository;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private NavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        chatThemeRepository = new ChatThemeRepository(this);
        memoRepository = new MemoRepository(this);
        citationResourceRepository = new CitationResourceRepository(this);

        //toolbar の設置
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(R.drawable.ic_launcher_default);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getDrawerLayout().openDrawer(Gravity.LEFT);
                }
            });
        }
        setupViews();

        reloadChatThemeList();
        chatTheme = chatThemeList.get(0);
        reloadChatThemeMenu();

        //memo の表示
        loadMemo(chatTheme);
        renewCitationResources();
        mainActivityHelper = new MainActivityHelper(
                new ImprovedTextCrawler(AppApplication.provideOkHttpClient(this)),
                mChatLikeListAdapter,
                mMemos);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //menu の表示
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        memoRepository.onResume(this);
        chatThemeRepository.onResume(this);
        citationResourceRepository.onResume(this);
        mainActivityHelper.loadPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (memoRepository != null) memoRepository.onPause();
        if (chatThemeRepository != null) chatThemeRepository.onPause();
        if (citationResourceRepository != null) citationResourceRepository.onPause();
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

    /****************************************
     * Views
     ****************************************/

    private void setupViews() {

        mContentEditText = (EditText) findViewById(R.id.editText);
        mResourceEditText = (AutoCompleteTextView) findViewById(R.id.editText_from);
        mResourceEditText.setAdapter(getCitationResourceSuggestionAdapter());
        mPagesEditText = (EditText) findViewById(R.id.editText_pages);
        setupAddAsProButton();
        setupAddAsConButton();

        mListView = (RecyclerView) findViewById(R.id.listView);
        mChatLikeListAdapter = new ChatLikeListAdapter(this, mMemos);
        mListView.setAdapter(mChatLikeListAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mListView.setLayoutManager(llm);
    }

    private Button setupAddAsProButton() {

        //Button の処理
        View.OnClickListener listener = new View.
                OnClickListener() {

            @Override
            public void onClick(View v) {
                saveMemoAndUpdate(v, true);

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
                saveMemoAndUpdate(v, false);
            }
        };
        Button viewById = (Button) findViewById(R.id.button_save_as_con);
        viewById.setOnClickListener(listener);
        return viewById;
    }

    private void saveMemoAndUpdate(View v, boolean asPro) {

        String content = mContentEditText.getText().toString();
        String citationResource =  mResourceEditText.getText().toString();
        if (StringUtils.isPresent(content) || UrlUtils.isValidUrl(citationResource)) {
            Calendar cal = Calendar.getInstance();
            //保存処置
            Memo newMemo = insertMemo(content, cal, citationResource, mPagesEditText.getText().toString(), asPro);
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

            mainActivityHelper.loadPreview(newMemo);

        }
    }

    private DrawerLayout getDrawerLayout() {
        if (drawerLayout == null) drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        return drawerLayout;
    }

    /****************************************
     * DB Access
     ****************************************/

    private void loadMemo(ChatTheme chatTheme) {
        //database からすべてを呼び出し、メモに追加する
        List<Memo> memos = memoRepository.loadFromChatTheme(chatTheme);
        if (memos == null) return;
        mMemos.clear();
        mMemos.addAll(memos);
        mChatLikeListAdapter.notifyDataSetChanged();
        if (mChatLikeListAdapter.getItemCount() > 0) {
            mListView.smoothScrollToPosition(mChatLikeListAdapter.getItemCount() - 1);
        }
    }

    private Memo insertMemo(String text, Calendar cal, String resource, String pages, boolean isPro) {
        //memo を追加し、セーブする
        Memo memo = new Memo(cal, text, isPro);
        memo.addCitationResource(resource.replaceAll("\\s+$", ""));
        memo.setPages(pages);
        memo.setChatTheme(chatTheme);
        if(memoRepository.save(memo)) {
            mMemos.add(memo);
            renewCitationResources();
        }
        return memo;
    }

    private void deleteMemo(int position) {
        //memo を消去する
        Memo memo = mMemos.get(position);
        if (memoRepository.delete(memo) == 1) {
            mMemos.remove(memo);
        } else {
            LogUtils.w("something wrong");
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

        if (url == null) url = "";
        Intent mainIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(mainIntent, PackageManager.MATCH_ALL);
        if (resolveInfos.size() == 0) {
            Toast.makeText(this, "ブラウザアプリがインストールされていません。", Toast.LENGTH_LONG).show();
            LogUtils.e("browser activity cannot found.");
        } else {
            startActivity(mainIntent);
        }
    }

    /***********************************************
     * データの更新 *
     ***********************************************/

    public ArrayAdapter<String> getCitationResourceSuggestionAdapter() {
        if (mCitationResourceSuggestionAdapter == null) {
            mCitationResourceSuggestionAdapter = new ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    citationResouces
            );
        }
        return mCitationResourceSuggestionAdapter;
    }

    public void renewCitationResources() {
        citationResouces.clear();

        Observable<String> memoObservable = Observable
                .from(mMemos)
                .map(new Func1<Memo, String>() {

                    @Override
                    public String call(Memo memo) {
                        return memo.getCitationResource();
                    }
                });
        // 気を利かせて、メモが1つ未満のときはすべての議題を検索して候補を提示する
        if (mMemos.size() <= 1) {
            memoObservable = Observable.merge(
                    memoObservable,
                    Observable
                            .from(citationResourceRepository.findAll())
                            .map(new Func1<CitationResource, String>() {
                                @Override
                                public String call(CitationResource citationResource) {
                                    return citationResource.getName();
                                }
                            }));
        }
        citationResouces.addAll(
                memoObservable
                        .filter(new Func1<String, Boolean>() {
                            @Override
                            public Boolean call(String s) {
                                LogUtils.i("s: " + s);
                                return !StringUtils.isEmpty(s) && !UrlUtils.isValidUrl(s.replaceAll("\\s+$", ""));
                            }
                        })
                        .distinct()
                        .toList().toBlocking().single());

        // notifiyDataSetChangedだと正しく更新されない
        getCitationResourceSuggestionAdapter().clear();
        getCitationResourceSuggestionAdapter().addAll(citationResouces);
        getCitationResourceSuggestionAdapter().notifyDataSetChanged();
        mResourceEditText.setAdapter(getCitationResourceSuggestionAdapter());
    }


    private void reloadChatThemeList() {
        chatThemeList.clear();
        chatThemeList.addAll(chatThemeRepository.findAll());
    }

    private void reloadChatThemeMenu() {
        if (navigationView == null) {
            navigationView = (NavigationView) findViewById(R.id.navigation_view);
        }
        final Menu menu = navigationView.getMenu();
        final MenuItem item = menu.findItem(R.id.menu_group_sub);

        item.getSubMenu().clear();

        if (chatThemeList.size() == 0) {
            chatTheme = new ChatTheme("最初の議題");
            chatThemeRepository.save(chatTheme);// ここでIDがセットされる
            chatThemeList.add(chatTheme);
        }
        for (ChatTheme theme : chatThemeList) {
            MenuItem add = item.getSubMenu().add(R.id.menu_group_sub_child, theme.getId().intValue(), Menu.NONE, theme.getTitle());
            add.setChecked(theme.getId() == chatTheme.getId());
        }

        getSupportActionBar().setTitle(chatTheme.getTitle());

        navigationView.setNavigationItemSelectedListener(getOnNavigationItemSelectedListener());
    }

    private NavigationView.OnNavigationItemSelectedListener getOnNavigationItemSelectedListener() {
        if (onNavigationItemSelectedListener == null ) {
            onNavigationItemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {

                @Override
                public boolean onNavigationItemSelected(MenuItem item) {
                    if (item.getGroupId() == R.id.menu_group_main) {
                        switch (item.getItemId()) {
                            default:
                                // TODO 20160216 なんとかする
                                return false;
                        }
                    } else if (item.getItemId() == R.id.menu_add_new_theme) {
                        InputDialogFragment f = InputDialogFragment.newInstance(MainActivity.this, null, "議題設定", null);
                        showDialogFragment(f, TAG_INPUT_NEW_THEME);
                        return true;

                    } else if (item.getGroupId() == R.id.menu_group_sub_child) {
                        ChatTheme byId = chatThemeRepository.findById(item.getItemId());
                        LogUtils.d("id: " + item.getItemId() +", chatTheme: " + byId);
                        if (byId != null) {
                            chatTheme = byId;
                        } else {
                            chatTheme = new ChatTheme(item.getTitle().toString());
                            chatThemeRepository.save(chatTheme);// ここでIDがセットされる
                            chatThemeList.add(chatTheme);
                        }
                        reloadChatTheme(chatTheme);

                        getDrawerLayout().closeDrawers();
                        return true;
                    } else {
                        LogUtils.w("id: " + item.getItemId());
                        return false;
                    }
                }
            };
        }
        return onNavigationItemSelectedListener;
    }


    @Override
    public void onAlertDialogClicked(String tag, Bundle args, int which) {
        if (StringUtils.isSame(tag, TAG_INPUT_NEW_THEME)) {
            String newTheme = args.getString(InputDialogFragment.RESULT, "新しい議題");

            chatTheme = new ChatTheme(newTheme);
            chatThemeRepository.save(chatTheme);// ここでIDがセットされる
            chatThemeList.add(chatTheme);
            reloadChatTheme(chatTheme);

            getDrawerLayout().closeDrawers();
        } else {
            LogUtils.w("Something wrong. " + tag);
        }
    }

    @Override
    public void onAlertDialogCancelled(String tag, Bundle args) {
        if (StringUtils.isSame(tag, TAG_INPUT_NEW_THEME)) {
            getDrawerLayout().closeDrawers();
        } else {
            LogUtils.w("Something wrong. " + tag);
        }

    }

    private void reloadChatTheme(ChatTheme ct) {
        reloadChatThemeList();
        reloadChatThemeMenu();
        loadMemo(ct);
        renewCitationResources();
        mainActivityHelper.loadPreview();
    }
}
