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
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewAfterTextChangeEvent;
import com.jakewharton.rxbinding.widget.TextViewEditorActionEvent;

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
import jp.ne.hatena.hackugyo.procon.ui.fragment.ConfirmDialogFragment;
import jp.ne.hatena.hackugyo.procon.ui.fragment.InputDialogFragment;
import jp.ne.hatena.hackugyo.procon.ui.widget.RecyclerViewEmptySupport;
import jp.ne.hatena.hackugyo.procon.util.LogUtils;
import jp.ne.hatena.hackugyo.procon.util.StringUtils;
import jp.ne.hatena.hackugyo.procon.util.UrlUtils;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public class MainActivity extends AbsBaseActivity implements AbsCustomDialogFragment.Callbacks {

    public static final String TAG_INPUT_NEW_THEME = "MainActivity.TAG_INPUT_NEW_THEME";
    private static final String TAG_CONFIRM_DELETE_THEME = "MainActivity.TAG_CONFIRM_DELETE_THEME";

    private EditText contentEditText;
    private EditText pagesEditText;
    private AutoCompleteTextView citationResourceEditText;
    private ArrayAdapter<String> citationResourceSuggestionAdapter;
    private NavigationView drawerLeft;
    private DrawerLayout drawerManager;
    private NavigationView drawerRight;
    private EditText themeEditText;
    private AppCompatButton themeDeleteButton;

    RecyclerViewEmptySupport mainRecyclerView, summaryRecyclerView;
    ChatLikeListAdapter mainListAdapter;
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
    private Button addAsProButton, addAsConButon;


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
                    getDrawerManager().openDrawer(Gravity.LEFT);
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
                mainListAdapter,
                memos);
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

        contentEditText = (EditText) findViewById(R.id.editText);
        citationResourceEditText = (AutoCompleteTextView) findViewById(R.id.editText_from);
        citationResourceEditText.setAdapter(getCitationResourceSuggestionAdapter());
        pagesEditText = (EditText) findViewById(R.id.editText_pages);
        addAsProButton = setupAddAsProButton();
        addAsConButon = setupAddAsConButton();

        Observable.combineLatest(
                RxTextView
                        .afterTextChangeEvents(contentEditText)
                        .map(
                                new Func1<TextViewAfterTextChangeEvent, Boolean>() {
                                    @Override
                                    public Boolean call(TextViewAfterTextChangeEvent textViewAfterTextChangeEvent) {
                                        return StringUtils.isPresent(textViewAfterTextChangeEvent.editable().toString());
                                    }
                                }),
                RxTextView
                        .afterTextChangeEvents(citationResourceEditText)
                        .map(
                                new Func1<TextViewAfterTextChangeEvent, Boolean>() {
                                    @Override
                                    public Boolean call(TextViewAfterTextChangeEvent textViewAfterTextChangeEvent) {
                                        return UrlUtils.isValidUrl(textViewAfterTextChangeEvent.editable().toString());
                                    }
                                }),
                new Func2<Boolean, Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean isContentPresent, Boolean isCitationResourceUrl) {
                        return isContentPresent || isCitationResourceUrl;
                    }
                })
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean isValid) {
                        addAsProButton.setEnabled(isValid);
                        addAsConButon.setEnabled(isValid);
                    }
                });

        provideRightDrawer();
        provideRightDrawerTitle();
        provideThemeDeleteButton();
        provideRightDrawerRecyclerView();

        getDrawerManager().setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {
                //Keyboard の消去
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(drawerView.getWindowToken(), 0);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                //Keyboard の消去
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(drawerView.getWindowToken(), 0);
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

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
            themeEditText = (EditText) provideRightDrawer().getHeaderView(0).findViewById(R.id.navigation_header_right_title);
            Observable<TextViewEditorActionEvent> textViewEditorActionEventObservable = RxTextView.editorActionEvents(themeEditText);
            textViewEditorActionEventObservable.subscribe(
                    new Action1<TextViewEditorActionEvent>() {
                        @Override
                        public void call(TextViewEditorActionEvent event) {
                            if (
                                    event.actionId() == EditorInfo.IME_ACTION_DONE ||
                                            (event.keyEvent().getKeyCode() == KeyEvent.KEYCODE_ENTER && event.keyEvent().getAction() == KeyEvent.ACTION_DOWN)
                                    ) {
                                //Keyboard の消去
                                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                inputMethodManager.hideSoftInputFromWindow(event.view().getWindowToken(), 0);

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
            themeDeleteButton = (AppCompatButton) provideRightDrawer().getHeaderView(0).findViewById(R.id.button_delete_this_theme);
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

    private RecyclerView provideRightDrawerRecyclerView() {
        if (summaryRecyclerView == null) {

            View container = LayoutInflater.from(this).inflate(R.layout.layout_navidation_content_right, null, false);
            provideRightDrawer().addHeaderView(container);
            summaryRecyclerView = (RecyclerViewEmptySupport) container.findViewById(R.id.listView_summary);
            mainListAdapter = new ChatLikeListAdapter(this, memos, getMainOnClickRecyclerListener());
            summaryRecyclerView.setAdapter(mainListAdapter);
            LinearLayoutManager llm = new LinearLayoutManager(this);
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            summaryRecyclerView.setLayoutManager(llm);
            summaryRecyclerView.setEmptyView(container.findViewById(R.id.summary_empty));

        }
        return summaryRecyclerView;
    }

    private void saveMemoAndUpdate(View v, boolean asPro) {

        String content = contentEditText.getText().toString();
        String citationResource =  citationResourceEditText.getText().toString();
        if (StringUtils.isPresent(content) || UrlUtils.isValidUrl(citationResource)) {
            Calendar cal = Calendar.getInstance();
            //保存処置
            Memo newMemo = insertMemo(content, cal, citationResource, pagesEditText.getText().toString(), asPro);
            //ListView に設置
            mainListAdapter.notifyDataSetChanged();
            //Keyboard の消去
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
            //EditText 内のデータ消去
            contentEditText.getEditableText().clear();
            citationResourceEditText.getEditableText().clear();
            pagesEditText.getEditableText().clear();

            mainRecyclerView.smoothScrollToPosition(mainListAdapter.getItemCount() - 1);

            mainActivityHelper.loadPreview(newMemo);

        }
    }

    private DrawerLayout getDrawerManager() {
        if (drawerManager == null) drawerManager = (DrawerLayout) findViewById(R.id.drawer_layout);
        return drawerManager;
    }

    /****************************************
     * DB Access
     ****************************************/

    private void loadMemo(ChatTheme chatTheme) {
        //database からすべてを呼び出し、メモに追加する
        List<Memo> memos = memoRepository.loadFromChatTheme(chatTheme);
        if (memos == null) return;
        this.memos.clear();
        this.memos.addAll(memos);
        mainListAdapter.notifyDataSetChanged();
        if (mainListAdapter.getItemCount() > 0) {
            mainRecyclerView.smoothScrollToPosition(mainListAdapter.getItemCount() - 1);
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
        }
        return memo;
    }

    private void deleteMemo(int position) {
        //memo を消去する
        Memo memo = memos.get(position);
        if (memoRepository.delete(memo) == 1) {
            memos.remove(memo);
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
        citationResources.clear();

        Observable<String> memoObservable = Observable
                .from(memos)
                .map(new Func1<Memo, String>() {

                    @Override
                    public String call(Memo memo) {
                        return memo.getCitationResource();
                    }
                });
        // 気を利かせて、メモが1つ未満のときはすべての議題を検索して候補を提示する
        if (memos.size() <= 1) {
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
        citationResources.addAll(
                memoObservable
                        .filter(new Func1<String, Boolean>() {
                            @Override
                            public Boolean call(String s) {
                                return !StringUtils.isEmpty(s) && !UrlUtils.isValidUrl(s.replaceAll("\\s+$", ""));
                            }
                        })
                        .distinct()
                        .toList().toBlocking().single());

        // notifiyDataSetChangedだと正しく更新されない
        getCitationResourceSuggestionAdapter().clear();
        getCitationResourceSuggestionAdapter().addAll(citationResources);
        getCitationResourceSuggestionAdapter().notifyDataSetChanged();
        citationResourceEditText.setAdapter(getCitationResourceSuggestionAdapter());
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

            chatTheme = new ChatTheme(newTheme);
            chatThemeRepository.save(chatTheme);// ここでIDがセットされる
            chatThemeList.add(chatTheme);
            reloadChatTheme(chatTheme);

            getDrawerManager().closeDrawers();
        } else if (StringUtils.isSame(tag, TAG_CONFIRM_DELETE_THEME)) {
            deleteCurrentChatTheme();
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

    private void reloadChatTheme(ChatTheme ct) {
        reloadChatThemeList();
        reloadChatThemeMenu();
        loadMemo(ct);
        renewCitationResources();
        mainActivityHelper.loadPreview();
    }

    private void deleteCurrentChatTheme() {
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
                                if (first != null) {
                                    chatTheme = first;
                                } else {
                                    chatTheme = new ChatTheme("最初の議題");
                                    chatThemeRepository.save(chatTheme);// ここでIDがセットされる
                                    chatThemeList.add(chatTheme);
                                }
                                reloadChatTheme(chatTheme);
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
