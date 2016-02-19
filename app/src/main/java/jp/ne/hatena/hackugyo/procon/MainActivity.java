package jp.ne.hatena.hackugyo.procon;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewEditorActionEvent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import jp.ne.hatena.hackugyo.procon.adapter.ChatLikeListAdapter;
import jp.ne.hatena.hackugyo.procon.adapter.SummaryListAdapter;
import jp.ne.hatena.hackugyo.procon.io.ImprovedTextCrawler;
import jp.ne.hatena.hackugyo.procon.model.ChatTheme;
import jp.ne.hatena.hackugyo.procon.model.ChatThemeRepository;
import jp.ne.hatena.hackugyo.procon.model.CitationResourceRepository;
import jp.ne.hatena.hackugyo.procon.model.Memo;
import jp.ne.hatena.hackugyo.procon.model.MemoRepository;
import jp.ne.hatena.hackugyo.procon.ui.AbsBaseActivity;
import jp.ne.hatena.hackugyo.procon.ui.RecyclerClickable;
import jp.ne.hatena.hackugyo.procon.ui.fragment.AbsCustomDialogFragment;
import jp.ne.hatena.hackugyo.procon.ui.fragment.ConfirmDialogFragment;
import jp.ne.hatena.hackugyo.procon.ui.fragment.InputDialogFragment;
import jp.ne.hatena.hackugyo.procon.ui.widget.KeyboardClosingDrawerListener;
import jp.ne.hatena.hackugyo.procon.ui.widget.RecyclerViewEmptySupport;
import jp.ne.hatena.hackugyo.procon.util.EditTextUtils;
import jp.ne.hatena.hackugyo.procon.util.LogUtils;
import jp.ne.hatena.hackugyo.procon.util.StringUtils;
import jp.ne.hatena.hackugyo.procon.util.UrlUtils;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends AbsBaseActivity implements AbsCustomDialogFragment.Callbacks {

    public static final String TAG_INPUT_NEW_THEME = "MainActivity.TAG_INPUT_NEW_THEME";
    private static final String TAG_CONFIRM_DELETE_THEME = "MainActivity.TAG_CONFIRM_DELETE_THEME";

    private final MainActivity self = this;

    // メイン部分のView管理
    MainActivityViewProvider viewProvider;
    private ArrayAdapter<String> citationResourceSuggestionAdapter;
    // Drawer内部のView
    private NavigationView drawerLeft;
    private DrawerLayout drawerManager;
    private NavigationView drawerRight;
    private EditText themeEditText;
    private AppCompatButton themeDeleteButton;

    RecyclerViewEmptySupport mainRecyclerView, summaryRecyclerView;
    ChatLikeListAdapter mainListAdapter;
    SummaryListAdapter summaryListAdapter;
    Snackbar snackbar;

    private ChatTheme chatTheme;
    final ArrayList<Memo> memos = new ArrayList<Memo>();
    private final List<ChatTheme> chatThemeList = new ArrayList<>();
    private final List<String> citationResources = new ArrayList<>();

    private MemoRepository memoRepository;
    private ChatThemeRepository chatThemeRepository;
    private CitationResourceRepository citationResourceRepository;

    private MainActivityHelper mainActivityHelper;

    private NavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener;
    private RecyclerClickable mainOnClickRecyclerListener, summaryOnClickRecyclerListener;


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
            toolbar.setNavigationIcon(R.drawable.ic_home);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getDrawerManager().openDrawer(Gravity.LEFT);
                }
            });
        }
        setupViews();

        reloadChatThemeList();
        chatTheme = chatThemeList.get(0);
        reloadChatThemeMenu();

        //memo の表示
        getLoadMemoObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action1<List<Memo>>() {
                            @Override
                            public void call(List<Memo> memos) {
                                renewCitationResources();
                                mainActivityHelper = new MainActivityHelper(
                                        new ImprovedTextCrawler(AppApplication.provideOkHttpClient(self)),
                                        mainListAdapter,
                                        self.memos);
                                mainActivityHelper.loadPreviewAsync();
                            }

                        }
                );
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
            NavigationView drawer = provideRightDrawer();
            if (getDrawerManager().isDrawerOpen(drawer)) {
                getDrawerManager().closeDrawer(drawer);
            } else {
                getDrawerManager().openDrawer(drawer);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /****************************************
     * Views
     ****************************************/

    private void setupViews() {

        viewProvider = new MainActivityViewProvider(this, getCitationResourceSuggestionAdapter(), setupAddAsProButton(), setupAddAsConButton());
        provideRightDrawer();
        provideRightDrawerTitle();
        provideThemeDeleteButton();
        provideRightDrawerRecyclerView();

        getDrawerManager().setDrawerListener(new KeyboardClosingDrawerListener());

        mainRecyclerView = (RecyclerViewEmptySupport) findViewById(R.id.listView);
        mainListAdapter = new ChatLikeListAdapter(this, memos, getMainOnClickRecyclerListener());
        mainRecyclerView.setAdapter(mainListAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mainRecyclerView.setLayoutManager(llm);
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

    private NavigationView provideRightDrawer() {
        if (drawerRight == null) {
            drawerRight = (NavigationView) findViewById(R.id.drawer_right);
        }
        return drawerRight;
    }

    private TextView provideRightDrawerTitle() {
        if (themeEditText == null) {
            themeEditText = (EditText) provideRightDrawer().findViewById(R.id.navigation_header_right_title);
            Observable<TextViewEditorActionEvent> textViewEditorActionEventObservable = RxTextView.editorActionEvents(themeEditText);
            textViewEditorActionEventObservable.subscribe(
                    new Action1<TextViewEditorActionEvent>() {
                        @Override
                        public void call(TextViewEditorActionEvent event) {
                            if (EditTextUtils.isDone(event.actionId(), event.keyEvent())) {
                                EditTextUtils.closeKeyboard(MainActivity.this, event.view());
                                if (editChatTheme(event.view().getText().toString(), false)) {
                                    showSingleToast(R.string.toast_theme_name_edited, Toast.LENGTH_SHORT);
                                }
                            }
                        }
                    }
            );
            Observable<Boolean> booleanObservable = RxView.focusChanges(themeEditText);
            booleanObservable.subscribe(new Action1<Boolean>() {
                @Override
                public void call(Boolean hasFocus) {
                    if (!hasFocus) {
                        editChatTheme(themeEditText.getText().toString());
                    }
                }
            });
        }
        return themeEditText;
    }

    private AppCompatButton provideThemeDeleteButton() {
        if (themeDeleteButton == null) {
            themeDeleteButton = (AppCompatButton) provideRightDrawer().findViewById(R.id.button_delete_this_theme);
            themeDeleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ConfirmDialogFragment confirmDialogFragment = ConfirmDialogFragment.newInstance(MainActivity.this, null, "", "削除しますか？　この議題のメモはすべて失われ、元に戻すことはできません。");
                    showDialogFragment(confirmDialogFragment, TAG_CONFIRM_DELETE_THEME);
                }
            });
        }
        return themeDeleteButton;
    }

    /**
     * @see <a href="http://stackoverflow.com/a/33291107">参考リンク</a>
     * @return
     */
    private RecyclerView provideRightDrawerRecyclerView() {
        if (summaryRecyclerView == null) {
            View container = provideRightDrawer();
            summaryRecyclerView = (RecyclerViewEmptySupport) container.findViewById(R.id.listView_summary);
            summaryListAdapter = new SummaryListAdapter(this, getSummaryOnClickRecyclerListener());
            summaryRecyclerView.setAdapter(summaryListAdapter);
            LinearLayoutManager llm = new LinearLayoutManager(this);
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            summaryRecyclerView.setLayoutManager(llm);
            summaryRecyclerView.setEmptyView(container.findViewById(R.id.summary_empty));

        }
        return summaryRecyclerView;
    }

    private DrawerLayout getDrawerManager() {
        if (drawerManager == null) drawerManager = (DrawerLayout) findViewById(R.id.drawer_layout);
        return drawerManager;
    }

    /****************************************
     * DB Access
     ****************************************/

    private Observable<List<Memo>> getLoadMemoObservable() {
        Observable<List<Memo>> listObservable = Observable.create(
                new Observable.OnSubscribe<List<Memo>>() {
                    @Override
                    public void call(Subscriber<? super List<Memo>> subscriber) {
                        //database からすべてを呼び出し、メモに追加する
                        List<Memo> memos = memoRepository.loadFromChatTheme(MainActivity.this.chatTheme);
                        if (memos == null) {
                            subscriber.onNext(new ArrayList<Memo>());
                        } else {
                            subscriber.onNext(memos);
                        }
                        subscriber.onCompleted();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Action1<List<Memo>>() {
                    @Override
                    public void call(List<Memo> memos) {
                        self.memos.clear();
                        self.memos.addAll(memos);
                        mainListAdapter.notifyDataSetChanged();
                        if (mainListAdapter.getItemCount() > 0) {
                            mainRecyclerView.smoothScrollToPosition(mainListAdapter.getItemCount() - 1);
                        }
                        summaryListAdapter.reloadMemos(self.memos);
                    }
                });
        return listObservable;
    }

    private void saveMemoAndUpdate(View v, boolean asPro) {
        String content = viewProvider.contentEditText.getText().toString();
        String citationResource =  viewProvider.citationResourceEditText.getText().toString();
        if (StringUtils.isPresent(content) || UrlUtils.isValidUrl(citationResource)) {
            Calendar cal = Calendar.getInstance();
            //保存処置
            Memo newMemo = insertMemo(content, cal, citationResource, viewProvider.pagesEditText.getText().toString(), asPro);
            //ListView に設置
            mainListAdapter.notifyDataSetChanged();
            //Keyboard の消去，EditText 内のデータ消去
            viewProvider.resetInputTexts(v);

            mainRecyclerView.smoothScrollToPosition(mainListAdapter.getItemCount() - 1);

            mainActivityHelper.loadPreviewAsync(newMemo);
        }
    }

    private Memo insertMemo(String text, Calendar cal, String resource, String pages, boolean isPro) {
        //memo を追加し、セーブする
        Memo memo = new Memo(cal, text, isPro);
        memo.addCitationResource(resource.replaceAll("\\s+$", ""));
        memo.setPages(pages);
        memo.setChatTheme(chatTheme);
        if(memoRepository.save(memo)) {
            memos.add(memo);
            renewCitationResources();
            summaryListAdapter.reloadMemos(self.memos);
        }
        return memo;
    }

    private void deleteMemo(int position) {
        //memo を消去する
        Memo memo = memos.get(position);
        if (memoRepository.delete(memo) == 1) {
            memos.remove(memo);
            summaryListAdapter.reloadMemos(self.memos);
        } else {
            LogUtils.w("something wrong");
        }
    }


    /****************************************
     * {@link RecyclerClickable}
     ****************************************/

    private RecyclerClickable getMainOnClickRecyclerListener() {
        if (mainOnClickRecyclerListener == null) {
            mainOnClickRecyclerListener = new RecyclerClickable() {

                @Override
                public void onRecyclerClicked(View v, int position) {
                    //snackbar をクリックで消す処置
                    if (snackbar != null) snackbar.dismiss();
                    Memo memo = memos.get(position);
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
                                    mainListAdapter.notifyDataSetChanged();
                                }
                            });

                    snackbar.show();
                    return true;
                }
            };
        }
        return mainOnClickRecyclerListener;
    }


    private RecyclerClickable getSummaryOnClickRecyclerListener() {
        if (summaryOnClickRecyclerListener == null) {
            summaryOnClickRecyclerListener = new RecyclerClickable() {
                @Override
                public void onRecyclerClicked(View v, int position) {

                }

                @Override
                public void onRecyclerButtonClicked(View v, int position) {

                }

                @Override
                public boolean onRecyclerLongClicked(View v, int position) {
                    return false;
                }
            };
        }
        return summaryOnClickRecyclerListener;
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

    private ChatTheme createInitialChatTheme() {
        return createInitialChatTheme("最初の議題");
    }

    private ChatTheme createInitialChatTheme(String title) {
        ChatTheme chatTheme = new ChatTheme(title);
        chatThemeRepository.save(chatTheme);// ここでIDがセットされる
        chatThemeList.add(chatTheme);
        return chatTheme;
    }

    public ArrayAdapter<String> getCitationResourceSuggestionAdapter() {
        if (citationResourceSuggestionAdapter == null) {
            citationResourceSuggestionAdapter = new ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    citationResources
            );
        }
        return citationResourceSuggestionAdapter;
    }

    public void renewCitationResources() {
        // データの更新
        citationResources.clear();
        citationResources.addAll(MainActivityHelper.createNewCitationResources(memos, citationResourceRepository));
        // 表示の更新
        viewProvider.resetCitationResourceSuggestionAdapter(citationResources);
    }


    private void reloadChatThemeList() {
        chatThemeList.clear();
        chatThemeList.addAll(chatThemeRepository.findAll());
    }

    private void reloadChatThemeMenu() {
        if (drawerLeft == null) {
            drawerLeft = (NavigationView) findViewById(R.id.navigation_view);
        }
        final Menu menu = drawerLeft.getMenu();
        final MenuItem item = menu.findItem(R.id.menu_group_sub);

        item.getSubMenu().clear();

        if (chatThemeList.size() == 0) chatTheme = createInitialChatTheme();
        for (ChatTheme theme : chatThemeList) {
            MenuItem add = item.getSubMenu().add(R.id.menu_group_sub_child, theme.getId().intValue(), Menu.NONE, theme.getTitle());
            add.setChecked(theme.getId() == chatTheme.getId());
        }

        getSupportActionBar().setTitle(chatTheme.getTitle());
        provideRightDrawerTitle().setText(chatTheme.getTitle());

        drawerLeft.setNavigationItemSelectedListener(getOnNavigationItemSelectedListener());
    }

    private NavigationView.OnNavigationItemSelectedListener getOnNavigationItemSelectedListener() {
        if (onNavigationItemSelectedListener == null ) {
            onNavigationItemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {

                @Override
                public boolean onNavigationItemSelected(MenuItem item) {
                    if (item.getGroupId() == R.id.menu_group_main) {
                        switch (item.getItemId()) {
                            case R.id.menu_home:
                                // TODO 20160216 この画面へ
                                return true;
                            default:
                                // TODO 20160216 なんとかする
                                return true; // なおここでfalseを返すと選択されなかったことになるもよう
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
                        reloadChatThemeAsync();

                        getDrawerManager().closeDrawers();
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
            chatTheme = createInitialChatTheme(newTheme);
            reloadChatThemeAsync();

            getDrawerManager().closeDrawers();
        } else if (StringUtils.isSame(tag, TAG_CONFIRM_DELETE_THEME)) {
            deleteCurrentChatThemeAsync();
        } else {
            LogUtils.w("Something wrong. " + tag);
        }
    }

    @Override
    public void onAlertDialogCancelled(String tag, Bundle args) {
        if (StringUtils.isSame(tag, TAG_INPUT_NEW_THEME)) {
            // nothing to do.
        } else if (StringUtils.isSame(tag, TAG_CONFIRM_DELETE_THEME)) {
            // nothing to do.
        } else {
            LogUtils.w("Something wrong. " + tag);
        }

    }

    /**
     * {@link #chatTheme} の変更を反映します。
     */
    private void reloadChatThemeAsync() {
        reloadChatThemeList();
        reloadChatThemeMenu();
        getLoadMemoObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action1<List<Memo>>() {
                            @Override
                            public void call(List<Memo> memos) {
                                renewCitationResources();
                                mainActivityHelper.loadPreviewAsync();
                            }
                        }
                );
    }

    private void deleteCurrentChatThemeAsync() {
        Observable.from(memos)
                .subscribeOn(Schedulers.io()) // doOnNextを走らせる
                .doOnNext(new Action1<Memo>() {
                    @Override
                    public void call(Memo memo) {
                        memoRepository.delete(memo);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread()) // メインスレッドでUI操作
                .doOnCompleted(
                        new Action0() {
                            @Override
                            public void call() {
                                chatThemeRepository.delete(chatTheme);
                                getDrawerManager().closeDrawers();

                                ChatTheme first = chatThemeRepository.findFirst();
                                chatTheme = (first == null ? createInitialChatTheme() : first);
                                reloadChatThemeAsync();
                            }
                        })
                .subscribe();
    }



    private boolean editChatTheme(String newTitle) {
        return editChatTheme(newTitle, true);
    }

    private boolean editChatTheme(String newTitle, boolean isUndoable) {
        if (chatTheme == null) return false;
        final String currentTitle = chatTheme.getTitle();
        if (StringUtils.isSame(currentTitle, newTitle)) {
            return false;
        }
        chatTheme.setTitle(newTitle);
        chatThemeRepository.save(chatTheme);
        reloadChatThemeList();
        reloadChatThemeMenu();

        if (isUndoable) {
            LinearLayout layout = (LinearLayout) findViewById(R.id.snackbar);
            Snackbar undoSnackbar = Snackbar.make(layout, R.string.toast_theme_name_edited, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo_theme_name_edited, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            editChatTheme(currentTitle, false);
                        }
                    });
            undoSnackbar.show();
        }
        return true;
    }
}
