package jp.ne.hatena.hackugyo.procon;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.util.Pair;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewEditorActionEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import jp.ne.hatena.hackugyo.procon.adapter.AutoCompleteSuggestionArrayAdapter;
import jp.ne.hatena.hackugyo.procon.adapter.ChatLikeListAdapter;
import jp.ne.hatena.hackugyo.procon.adapter.SummaryListAdapter;
import jp.ne.hatena.hackugyo.procon.event.DataDeletedEvent;
import jp.ne.hatena.hackugyo.procon.event.DataSavedEvent;
import jp.ne.hatena.hackugyo.procon.event.RequestDataDeleteEvent;
import jp.ne.hatena.hackugyo.procon.event.RequestDataSaveEvent;
import jp.ne.hatena.hackugyo.procon.event.RxBusProvider;
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
import jp.ne.hatena.hackugyo.procon.ui.fragment.ChoiceDialogFragment;
import jp.ne.hatena.hackugyo.procon.ui.fragment.ConfirmDialogFragment;
import jp.ne.hatena.hackugyo.procon.ui.fragment.ImageDialogFragment;
import jp.ne.hatena.hackugyo.procon.ui.fragment.InputDialogFragment;
import jp.ne.hatena.hackugyo.procon.ui.widget.CustomBootStrapBrand;
import jp.ne.hatena.hackugyo.procon.ui.widget.KeyboardClosingDrawerListener;
import jp.ne.hatena.hackugyo.procon.ui.widget.RecyclerViewEmptySupport;
import jp.ne.hatena.hackugyo.procon.util.ArrayUtils;
import jp.ne.hatena.hackugyo.procon.util.EditTextUtils;
import jp.ne.hatena.hackugyo.procon.util.LogUtils;
import jp.ne.hatena.hackugyo.procon.util.StringUtils;
import jp.ne.hatena.hackugyo.procon.util.UrlUtils;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AbsBaseActivity implements AbsCustomDialogFragment.Callbacks {

    public static final String TAG_INPUT_NEW_THEME = "MainActivity.TAG_INPUT_NEW_THEME";
    private static final String TAG_CONFIRM_DELETE_THEME = "MainActivity.TAG_CONFIRM_DELETE_THEME";
    private static final String TAG_CHOOSE_EDIT_MODE = "MainActivity.TAG_CHOOSE_EDIT_MODE";
    private static final String TAG_EDIT_CONTENT = "MainActivity.TAG_EDIT_CONTENT";
    private static final String TAG_EDIT_CITATION_RESOURCE = "MainActivity.TAG_EDIT_CITATION_RESOURCE";
    private static final String TAG_SHOW_IMAGE = "MainActivity.TAG_SHOW_IMAGE";

    public static final String ITEM_ID = "MainActivity.ITEM_ID";
    public static final String CHOICE_IDS = "MainActivity.CHOICE_IDS";
    private static final int REQUEST_CAMERA_CHOOSER = 1000;
    private static final int REQUEST_GALLERY_CHOOSER = 1001;

    private static final String SHARED_PREFERENCE_LAST_THEME_ID = "MainActivity.SHARED_PREFERENCE_LAST_THEME_ID";
    private static final String SHARED_PREFERENCE_SHOW_REORDERING_HINT = "MainActivity.SHARED_PREFERENCE_SHOW_REORDERING_HINT";

    /**
     * 長押しからのアイテム編集モード
     */
    public enum EditModeEnum {
        DELETE_THIS_ITEM(0, "このアイテムを削除"),
        FORCE_RELOAD(1, "Webから再読込"),
        SHARE_THIS_ITEM(2, "本文を共有"),
        OPEN_URL(3, "URLを開く"),
        EDIT_THIS_ITEM(4, "本文を編集"),
        EDIT_CITATION(5, "出典情報を編集"),;
        public final int id;
        public final String  title;
        EditModeEnum(final int id, final String title) {
            this.id = id;
            this.title = title;
        }

        static ArrayList<String> titlesFrom(List<EditModeEnum> enums) {
            return new ArrayList<String>(
                    Observable.from(enums).map(new Func1<EditModeEnum, String>() {
                        @Override
                        public String call(EditModeEnum editModeEnum) {
                            return editModeEnum.title;
                        }
                    }).toList().toBlocking().single()
            );
        }

        static ArrayList<Integer> idsFrom(List<EditModeEnum> enums) {
            return new ArrayList<Integer>(
                    Observable.from(enums).map(new Func1<EditModeEnum, Integer>() {
                        @Override
                        public Integer call(EditModeEnum editModeEnum) {
                            return editModeEnum.id;
                        }
                    }).toList().toBlocking().single()
            );
        }
    }

    private final MainActivity self = this;

    Toolbar toolbar;
    AppBarLayout appBar;
    // メイン部分のView管理
    MainActivityViewProvider viewProvider;
    private ArrayAdapter citationResourceSuggestionAdapter;
    // Drawer内部のView
    private NavigationView drawerLeft;
    private DrawerLayout drawerManager;
    private NavigationView drawerRight;
    private EditText themeEditText;
    private BootstrapButton themeDeleteButton;
    private BootstrapButton themeExportButton;
    private BootstrapButton reorderMemosButton;
    private RecyclerClickable imageOnClickRecyclerListener;
    private ImageView imageThumbnailView;
    /**
     * メインのRecylerView（{@link #mainRecyclerView}）が並べ替え可能状態かどうか
     */
    private boolean isInReorderMode = false;

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

    private CompositeSubscription compositeSubscription;
    /**
     * Intentの返却値にUriを含めて返さない端末があるため、アプリ側でいちおう持っておく。
     */
    private Uri tempUriForRequestChooser;
    private BootstrapButton photoButton;
    private BootstrapButton cameraButton;

    /**
     * 並べ替えまえの状態を退避しておく
     */
    private ArrayList<Memo> stashedMemosForReordering;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        chatThemeRepository = new ChatThemeRepository(this);
        memoRepository = new MemoRepository(this);
        citationResourceRepository = new CitationResourceRepository(this);

        //toolbar の設置
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(R.drawable.ic_home);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isInReorderMode) getDrawerManager().openDrawer(Gravity.LEFT);
                }
            });
        }
        appBar = (AppBarLayout) findViewById(R.id.main_appbar);
        setupViews();

        reloadChatThemeList();
        final long latestChatThemeId = AppApplication.getSharedPreferences().getLong(SHARED_PREFERENCE_LAST_THEME_ID, 0);
        chatTheme = Observable.from(chatThemeList).firstOrDefault(ArrayUtils.last(chatThemeList), new Func1<ChatTheme, Boolean>() {
            @Override
            public Boolean call(ChatTheme chatTheme) {
                return chatTheme.getId() == latestChatThemeId;
            }
        }).toBlocking().single();
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
                                        self.memos,
                                        memoRepository);
                                mainActivityHelper.loadPreviewAsync();
                            }

                        }
                );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //menu の表示
        MenuInflater inflater = getMenuInflater();
        if (isInReorderMode) {
            inflater.inflate(R.menu.menu_main_reorder, menu);
        } else {
            inflater.inflate(R.menu.menu_main, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        compositeSubscription = new CompositeSubscription();
        compositeSubscription.add(
                RxBusProvider.getInstance()
                        .subscribe(DataSavedEvent.class, new Action1<DataSavedEvent>() {
                            @Override
                            public void call(DataSavedEvent dataSavedEvent) {
                                onMemoSaved(dataSavedEvent);
                            }
                        }, AndroidSchedulers.mainThread())
        );
        compositeSubscription.add(
                RxBusProvider.getInstance()
                        .subscribe(DataDeletedEvent.class, new Action1<DataDeletedEvent>() {
                            @Override
                            public void call(DataDeletedEvent dataDeletedEvent) {
                                onMemoDeleted(dataDeletedEvent);
                            }
                        }, AndroidSchedulers.mainThread())
        );

        memoRepository.onResume(this);
        chatThemeRepository.onResume(this);
        citationResourceRepository.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (compositeSubscription != null) compositeSubscription.unsubscribe();
        if (memoRepository != null) memoRepository.onPause();
        if (chatThemeRepository != null) chatThemeRepository.onPause();
        if (citationResourceRepository != null) citationResourceRepository.onPause();
        AppApplication.getSharedPreferences().edit().putLong(SHARED_PREFERENCE_LAST_THEME_ID, chatTheme.getId()).apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //icon 押下時の処置
        if (id == R.id.menu_edit_this_theme) {
            NavigationView drawer = provideRightDrawer();
            if (getDrawerManager().isDrawerOpen(drawer)) {
                getDrawerManager().closeDrawer(drawer);
            } else {
                if (!isInReorderMode) getDrawerManager().openDrawer(drawer);
            }
            return true;
        } else if (id == R.id.menu_done_reorder) {
            if (Memo.setPositions(Observable.from(memos))) {
                showProgressDialog();
                updateMemoAsync(memos);
            }
            isInReorderMode = false;
            setToolbarScrollable(true);
            invalidateOptionsMenu();
        } else if (id == R.id.menu_cancel_reorder) {
            if (stashedMemosForReordering != null) {
                memos.clear();
                memos.addAll(stashedMemosForReordering);
                stashedMemosForReordering.clear();
                stashedMemosForReordering = null;
                mainListAdapter.notifyDataSetChanged();
            }
            isInReorderMode = false;
            setToolbarScrollable(true);
            invalidateOptionsMenu();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CAMERA_CHOOSER) {
            getPhotoButton().setVisibility(View.GONE);
            getCameraButton().setVisibility(View.GONE);
            if(resultCode != RESULT_OK) {
                // キャンセル時
                tempUriForRequestChooser = null;
                return ;
            }

            Uri resultUri = (data != null ? data.getData() : tempUriForRequestChooser);
            // dataからUriがとれない端末があるので退避した
            LogUtils.d("data: " + resultUri);
            tempUriForRequestChooser = null;
            if(resultUri == null) {
                // 取得失敗
                return;
            }

            // ギャラリーへスキャンを促す
            MediaScannerConnection.scanFile(
                    this,
                    new String[]{resultUri.getPath()},
                    new String[]{"image/jpeg"},
                    null
            );

            viewProvider.setImage(resultUri);
        } else if (requestCode == REQUEST_GALLERY_CHOOSER) {
            getPhotoButton().setVisibility(View.GONE);
            getCameraButton().setVisibility(View.GONE);
            if(resultCode != RESULT_OK) {
                // キャンセル時
                return ;
            }
            Uri resultUri = (data != null ? data.getData() : tempUriForRequestChooser);
            LogUtils.v("data: " + resultUri);
            if(resultUri == null) {
                // 取得失敗
                return;
            }
            viewProvider.setImage(resultUri);
        }
    }

    /****************************************
     * Views
     ****************************************/

    private void setupViews() {
        viewProvider = new MainActivityViewProvider(this, getCitationResourceSuggestionAdapter(), setupAddAsProButton(), setupAddAsConButton(), setupOthersButton());
        viewProvider.setImageView(provideImageThumbnailView());
        provideRightDrawer();
        provideRightDrawerTitle();
        setupRightDrawerButtons();
        provideRightDrawerRecyclerView();

        getDrawerManager().addDrawerListener(new KeyboardClosingDrawerListener());

        mainRecyclerView = (RecyclerViewEmptySupport) findViewById(R.id.listView);
        { // D&D用のリスナと相互参照させる
            itemTouchHelper.attachToRecyclerView(mainRecyclerView);
            mainRecyclerView.addItemDecoration(itemTouchHelper);
        }
        mainListAdapter = new ChatLikeListAdapter(this, memos, getMainOnClickRecyclerListener());
        mainListAdapter.setOnImageClickListener(getImageOnClickRecyclerListener());
        mainRecyclerView.setAdapter(mainListAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        llm.setSmoothScrollbarEnabled(true);
        mainRecyclerView.setLayoutManager(llm);
    }

    private BootstrapButton setupAddAsProButton() {
        //Button の処理
        View.OnClickListener listener = new View.
                OnClickListener() {

            @Override
            public void onClick(View v) {
                saveMemoAndUpdate(v, true);

            }
        };
        BootstrapButton viewById = (BootstrapButton) findViewById(R.id.button_save_as_pro);
        viewById.setBootstrapBrand(CustomBootStrapBrand.PRO);
        viewById.setOnClickListener(listener);
        return viewById;
    }

    private BootstrapButton setupAddAsConButton() {
        //Button の処理
        View.OnClickListener listener = new View.
                OnClickListener() {

            @Override
            public void onClick(View v) {
                saveMemoAndUpdate(v, false);
            }
        };
        BootstrapButton  viewById = (BootstrapButton) findViewById(R.id.button_save_as_con);
        viewById.setBootstrapBrand(CustomBootStrapBrand.CON);
        viewById.setOnClickListener(listener);
        return viewById;
    }

    private BootstrapButton setupOthersButton() {
        //Button の処理
        View.OnClickListener listener = new View.
                OnClickListener() {

            @Override
            public void onClick(View v) {
                getPhotoButton().setVisibility(getPhotoButton().getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
                getCameraButton().setVisibility(getCameraButton().getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
            }
        };
        BootstrapButton  viewById = (BootstrapButton) findViewById(R.id.button_save_as_other);
        if (viewById != null) {
            viewById.setBootstrapBrand(CustomBootStrapBrand.OTHER);
            viewById.setOnClickListener(listener);
        }
        return viewById;
    }

    private BootstrapButton getCameraButton() {
        if (cameraButton == null) {
            //Button の処理
            View.OnClickListener listener = new View.
                    OnClickListener() {

                @Override
                public void onClick(View v) {
                    showCamera();
                }
            };
            cameraButton = (BootstrapButton) findViewById(R.id.button_load_from_camera);
            if (cameraButton != null) {
                cameraButton.setBootstrapBrand(CustomBootStrapBrand.OTHER);
                cameraButton.setOnClickListener(listener);
            }
        }
        return cameraButton;
    }

    private BootstrapButton getPhotoButton() {
        if (photoButton == null) {
            //Button の処理
            View.OnClickListener listener = new View.
                    OnClickListener() {

                @Override
                public void onClick(View v) {
                    showGallery();
                }
            };
            photoButton = (BootstrapButton) findViewById(R.id.button_load_from_gallery);
            if (photoButton != null) {
                photoButton.setBootstrapBrand(CustomBootStrapBrand.OTHER);
                photoButton.setOnClickListener(listener);
            }
        }
        return photoButton;
    }

    private ImageView provideImageThumbnailView() {
        if (imageThumbnailView == null) {
            imageThumbnailView = (ImageView)findViewById(R.id.imageView);
            imageThumbnailView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (viewProvider.getImageUri() == null) {
                        showGallery();
                    } else {
                        showImageEnlarge(viewProvider.getImageUri().toString());
                    }
                }
            });
        }
        return imageThumbnailView;
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

    private void setupRightDrawerButtons() {
        provideThemeDeleteButton();
        provideReorderMemosButton();
        provideThemeExportButton();
    }

    private BootstrapButton provideThemeDeleteButton() {
        if (themeDeleteButton == null) {
            themeDeleteButton = (BootstrapButton) provideRightDrawer().findViewById(R.id.button_delete_this_theme);
            themeDeleteButton.setBootstrapBrand(CustomBootStrapBrand.OTHER);
            themeDeleteButton.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
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
     * 並べ替えモードに移行するボタン
     * @return
     */
    private BootstrapButton provideReorderMemosButton() {
        if (reorderMemosButton == null) {
            reorderMemosButton = (BootstrapButton) provideRightDrawer().findViewById(R.id.button_reorder_memos);
            reorderMemosButton.setBootstrapBrand(CustomBootStrapBrand.OTHER);
            reorderMemosButton.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
            reorderMemosButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setToolbarScrollable(false);
                    stashedMemosForReordering = ArrayUtils.copy(memos);
                    isInReorderMode = true;
                    invalidateOptionsMenu();
                    getDrawerManager().closeDrawers();
                }
            });
        }
        return reorderMemosButton;
    }

    /**
     * @see <a href="http://stackoverflow.com/a/33128251">参考リンク</a>
     * @param isScrollable
     */
    private void setToolbarScrollable(boolean isScrollable) {
        AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
        params.setScrollFlags(isScrollable ?
                (AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS | AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL) :
                (AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS)
        );
        if (!isScrollable) appBar.setExpanded(true);
    }

    private BootstrapButton provideThemeExportButton() {
        if (themeExportButton == null) {
            themeExportButton = (BootstrapButton) provideRightDrawer().findViewById(R.id.button_export_whole_memo);
            themeExportButton.setBootstrapBrand(CustomBootStrapBrand.OTHER);
            themeExportButton.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
            themeExportButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showProgressDialog();
                    // mainActivityHelper.
                    MainActivityHelper.createExportationObservable(
                            chatTheme.getId(),
                            Observable.from(memos)
                                    .observeOn(Schedulers.computation())
                    )
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Action1<Pair<Long, StringBuilder>>() {
                                @Override
                                public void call(Pair<Long, StringBuilder> result) {
                                    hideProgressDialog();
                                    if (result.first != chatTheme.getId()) {
                                        return;
                                    } else {
                                        sendText(result.second.toString(), chatTheme.getTitle());
                                    }
                                }
                            });
                }
            });
        }
        return themeExportButton;
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

    /**
     * メモの読み込み
     * @return
     */
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
                        smoothScrollMainRecyclerView(mainRecyclerView, mainListAdapter);
                        summaryListAdapter.reloadMemos(self.memos);
                    }
                });
        return listObservable;
    }

    private void saveMemoAndUpdate(View v, boolean asPro) {
        String content = viewProvider.contentEditText.getText().toString();
        String citationResource =  viewProvider.citationResourceEditText.getText().toString();
        if (StringUtils.isPresent(content) || UrlUtils.isValidWebUrl(citationResource)) {
            Calendar cal = Calendar.getInstance();
            //保存処置
            Memo memo = createMemo(content, cal, citationResource, viewProvider.pagesEditText.getText().toString(), asPro);
            if (viewProvider.getImageUri() != null) {
                if (!ArrayUtils.any(memo.getCitationResources())) memo.addCitationResource("画像取り込み");
                memo.addCitationResource(viewProvider.getImageUri().toString());
            }
            insertMemoAsync(memo);
            //Keyboard の消去，EditText 内のデータ消去
            viewProvider.resetInputTexts(v);

            smoothScrollMainRecyclerView(mainRecyclerView, mainListAdapter);
        }
    }

    private void smoothScrollMainRecyclerView(RecyclerView recyclerView, RecyclerView.Adapter adapter) {
        appBar.setExpanded(false, true); // AppBarLayoutを閉じてから、いちばん下までスクロールさせる
        recyclerView.smoothScrollToPosition(Math.max(0, adapter.getItemCount() - 1));
    }

    private Memo createMemo(String text, Calendar cal, String resource, String pages, boolean isPro) {
        Memo memo = new Memo(cal, text, isPro);
        memo.addCitationResource(StringUtils.stripLast(resource));
        memo.setPages(pages);
        memo.setChatTheme(chatTheme);
        return memo;
    }

    private void insertMemoAsync(Memo memo) {
        RxBusProvider.getInstance().post(new RequestDataSaveEvent(memo));
    }

    private void deleteMemoAsync(Memo memo) {
        //memo を消去する
        RxBusProvider.getInstance().post(new RequestDataDeleteEvent(memo));
    }

    private void updateMemoAsync(List<Memo> sortedMemos) {
        RxBusProvider.getInstance().post(new RequestDataSaveEvent(sortedMemos));
    }


    private void onMemoSaved(DataSavedEvent event) {
        if (event.pairs.size() != 1) {
            onMemosUpdated(event.pairs);
        } else {
            Memo savedMemo = event.pairs.get(0).first;
            Memo inList = findById(memos, savedMemo.getId());
            if (event.pairs.get(0).second) {
                if (inList == null) {
                    memos.add(savedMemo);
                    renewCitationResources();
                    //ListView に設置
                    mainListAdapter.notifyDataSetChanged();
                    summaryListAdapter.reloadMemos(self.memos);
                    smoothScrollMainRecyclerView(mainRecyclerView, mainListAdapter);
                    mainActivityHelper.loadPreviewAsync(savedMemo);
                } else {
                    mainListAdapter.notifyItemChanged(memos.indexOf(inList));
                }
            } else {
                LogUtils.w("something wrong");
                viewProvider.resumeInputTexts((inList.isForUrl() ? "" : inList.getMemo()), inList.getCitationResource(), inList.getPages());
            }
        }
    }

    private void onMemosUpdated(ArrayList<Pair<Memo, Boolean>> updatedResults) {
        LogUtils.d("並べかえ完了");
        Pair<Memo, Boolean> firstFailure = Observable.from(updatedResults)
                .firstOrDefault(null, new Func1<Pair<Memo,Boolean>, Boolean>() {
                    @Override
                    public Boolean call(Pair<Memo, Boolean> memoBooleanPair) {
                        return !memoBooleanPair.second;
                    }
                }).toBlocking().single();
        if (firstFailure != null) {
            LogUtils.w("並べかえ失敗 " + firstFailure.first);
        }
        reloadChatThemeAsync();
        hideProgressDialog();
    }

    private void onMemoDeleted(DataDeletedEvent deleted) {
        for (Pair<Long, Boolean> result : deleted.pairs) {
            Memo inList = findById(memos, result.first);
            if (result.second) {
                memos.remove(inList);
            } else {
                LogUtils.w("something wrong");
                if (inList != null) inList.setRemoved(false);
            }
        }
        renewCitationResources();
        summaryListAdapter.reloadMemos(self.memos);
        mainListAdapter.notifyDataSetChanged();
    }

    private static Memo findById(List<Memo> memo, final long memoId) {
        return Observable.from(memo).firstOrDefault(null, new Func1<Memo, Boolean>() {
            @Override
            public Boolean call(Memo memo) {
                return memo.getId() == memoId;
            }
        }).toBlocking().single();
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
                    if (isInReorderMode) return false;
                    if (memos.size() <= position) {
                        LogUtils.w("あれ？" + memos.size() + " vs. " + position);
                        return false;
                    }
                    //snackbar をクリックで消す処置
                    if (snackbar != null) snackbar.dismiss();

                    if (AppApplication.getSharedPreferences().getBoolean(SHARED_PREFERENCE_SHOW_REORDERING_HINT, true)) {
                        showSingleToast("ヒント：並べ替えは右上メニューから行えます。", Toast.LENGTH_LONG);
                        AppApplication.getSharedPreferences().edit().putBoolean(SHARED_PREFERENCE_SHOW_REORDERING_HINT, false).apply();
                    }

                    Memo memo = memos.get(position);
                    ArrayList<EditModeEnum> items = new ArrayList<>();
                    {
                        items.add(EditModeEnum.DELETE_THIS_ITEM);
                        if (memo.isForUrl()) items.add(EditModeEnum.FORCE_RELOAD);
                        items.add(EditModeEnum.SHARE_THIS_ITEM);
                        if (memo.isForUrl()) items.add(EditModeEnum.OPEN_URL);
                        if (!memo.isForUrl()) items.add(EditModeEnum.EDIT_THIS_ITEM);
                        items.add(EditModeEnum.EDIT_CITATION);
                    }
                    Bundle args = new Bundle();
                    args.putLong(ITEM_ID, memo.getId());
                    args.putIntegerArrayList(CHOICE_IDS, EditModeEnum.idsFrom(items));
                    ChoiceDialogFragment choiceDialogFragment = ChoiceDialogFragment.newInstance(self, args, "", null,EditModeEnum.titlesFrom(items));
                    showDialogFragment(choiceDialogFragment, TAG_CHOOSE_EDIT_MODE);
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


    private RecyclerClickable getImageOnClickRecyclerListener() {
        if (imageOnClickRecyclerListener == null) {
            imageOnClickRecyclerListener = new RecyclerClickable() {
                @Override
                public void onRecyclerClicked(View v, int position) {
                    //snackbar をクリックで消す処置
                    if (snackbar != null) snackbar.dismiss();
                    Memo memo = memos.get(position);
                    if (memo.isWithPhoto()) {
                        showImageEnlarge(memo.getImageUrl());
                    }

                }

                @Override
                public void onRecyclerButtonClicked(View v, int position) {
                    getMainOnClickRecyclerListener().onRecyclerButtonClicked(v, position);
                }

                @Override
                public boolean onRecyclerLongClicked(View v, int position) {
                    return getMainOnClickRecyclerListener().onRecyclerLongClicked(v, position);
                }
            };
        }
        return imageOnClickRecyclerListener;
    }

    void showImageEnlarge(String imageUrlString) {
        showDialogFragment(
                ImageDialogFragment.newInstance(self, null, imageUrlString),
                TAG_SHOW_IMAGE);
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

    /**
     * ファイルマネージャを選択させて表示します．<br>
     * @see <a href="http://stackoverflow.com/a/26651827">参考リンク</a>
     * */
    public void launchExternalFileManager(Uri dirUri) {
        Intent mainIntent = new Intent(Intent.ACTION_VIEW);
        mainIntent.setDataAndType(dirUri, "resource/folder");
        List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(mainIntent, PackageManager.MATCH_ALL);
        if (mainIntent.resolveActivityInfo(getPackageManager(), 0) == null) {// resolveInfos.size() == 0) {
            Toast.makeText(this, "対応するファイル閲覧アプリがインストールされていません。", Toast.LENGTH_LONG).show();
        } else {
            startActivity(mainIntent);
        }
    }

    private void showCamera() {
        // カメラ起動のIntent作成 */
        File pathFilesDir = new File(
                StringUtils.build(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath(), File.separator,
                        self.getPackageName(), File.separator,
                        "Images"
                ));
        pathFilesDir.mkdirs();

        String filename =  StringUtils.build(
                StringUtils.padZeros(chatTheme.getId(), 8),
                "_", StringUtils.valueOf(System.currentTimeMillis()),
                ".jpg");
        File capturedFile = new File(pathFilesDir, filename);
        tempUriForRequestChooser = Uri.fromFile(capturedFile);
        Intent intentPhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intentPhoto.putExtra(MediaStore.EXTRA_OUTPUT, tempUriForRequestChooser);
        Intent chooserIntent = Intent.createChooser(intentPhoto, "画像の選択");
        startActivityForResult(chooserIntent, REQUEST_CAMERA_CHOOSER);
    }

    private void showGallery() {
        // ギャラリー用のIntent作成
        Intent intentGallery;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            intentGallery = new Intent(Intent.ACTION_GET_CONTENT);
            intentGallery.setType("image/*");
        } else {
            intentGallery = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intentGallery.addCategory(Intent.CATEGORY_OPENABLE);
            intentGallery.setType("image/jpeg");
        }

        Intent chooserIntent = Intent.createChooser(intentGallery, "画像の選択");

        startActivityForResult(chooserIntent, REQUEST_GALLERY_CHOOSER);
    }

    private void sendText(String text, String subject) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);

        List<ResolveInfo> resolveInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_ALL);
        if (intent.resolveActivityInfo(getPackageManager(), 0) == null || resolveInfoList.size() == 0) {
            Toast.makeText(self, "対応するアプリがインストールされていません。", Toast.LENGTH_LONG).show();
        } else {
            startActivity(intent);
        }
    }

    private void shareToTwitter(String text) {
        String url = "https://twitter.com/share?text=" + StringUtils.nullToEmpty(text);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
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

    public ArrayAdapter getCitationResourceSuggestionAdapter() {
        if (citationResourceSuggestionAdapter == null) {
            citationResourceSuggestionAdapter = new AutoCompleteSuggestionArrayAdapter(
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
        viewProvider.resetCitationResourceSuggestionAdapterAsync(citationResources);
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

        if (item != null) {
            SubMenu subMenu = item.getSubMenu();
            if (subMenu != null) {
                subMenu.clear();

                if (chatThemeList.size() == 0) chatTheme = createInitialChatTheme();
                for (ChatTheme theme : chatThemeList) {
                    MenuItem add = subMenu.add(R.id.menu_group_sub_child, theme.getId().intValue(), Menu.NONE, theme.getTitle());
                    add.setChecked(theme.getId() == chatTheme.getId());
                }
            }
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
                            case R.id.menu_settings:
                                return true;
                            case R.id.menu_help: // なおここでfalseを返すと選択されなかったことになるもよう
                                shareToTwitter("@hackugyo%20%20プロコン%20");
                                return false;
                            default:
                                LogUtils.w("missing menu called. at " + item.getItemId());
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
        } else if (StringUtils.isSame(tag, TAG_CHOOSE_EDIT_MODE)) {
            processEditingResult(args.getLong(ITEM_ID), args.getIntegerArrayList(CHOICE_IDS), which);
        } else if (StringUtils.isSame(tag, TAG_EDIT_CONTENT)) {
            setContentAtAsync(args);
        } else if (StringUtils.isSame(tag, TAG_EDIT_CITATION_RESOURCE)) {
            setCitationResourceAt(args);
        } else if (StringUtils.isSame(tag, TAG_SHOW_IMAGE)) {
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
        } else if (StringUtils.isSame(tag, TAG_CHOOSE_EDIT_MODE)) {
            // nothing to do.
        } else if (StringUtils.isSame(tag, TAG_EDIT_CONTENT)) {
            // nothing to do.
        } else if (StringUtils.isSame(tag, TAG_EDIT_CITATION_RESOURCE)) {
            // nothing to do.
        } else if (StringUtils.isSame(tag, TAG_SHOW_IMAGE)) {
            // nothing to do.
        } else {
            LogUtils.w("Something wrong. " + tag);
        }

    }

    private void processEditingResult(final long itemId, List<Integer> choiceIds, int which) {
        Memo single = findById(memos, itemId);
        if (single == null) {
            LogUtils.w("something wrong. " + itemId);
            return;
        }
        int index = memos.indexOf(single);
        switch (choiceIds.get(which)) {
            case 0:
                deleteMemoWithSomeStay(single, itemId);
                break;
            case 1:
                if (single.isForUrl()) {
                    mainActivityHelper.forceReloadPreviewAsync(single);
                    mainListAdapter.notifyItemChanged(index);
                } else {
                    LogUtils.w("something wrong.");
                }
                break;
            case 2:
                shareContent(single);
                break;
            case 3:
                if (single.isForUrl()) {
                    launchExternalBrowser(single.getCitationResource());
                }
            case 4:
                if (!single.isForUrl()) {
                    Bundle bundle = new Bundle();
                    bundle.putLong(ITEM_ID, itemId);
                    bundle.putString(InputDialogFragment.DEFAULT_STRING, single.getMemo());
                    showDialogFragment(InputDialogFragment.newInstance(self, bundle, "本文編集", null), TAG_EDIT_CONTENT);
                }
                break;
            case 5:
                Bundle bundle = new Bundle();
                bundle.putLong(ITEM_ID, itemId);
                bundle.putStringArrayList(InputDialogFragment.SUGGESTION_STRINGS, new ArrayList<String>(citationResources));
                bundle.putString(InputDialogFragment.DEFAULT_STRING, single.getCitationResource());
                showDialogFragment(InputDialogFragment.newInstance(self, bundle, "出典編集", null), TAG_EDIT_CITATION_RESOURCE);
                break;
            default:
                LogUtils.w("Something wrong. " + which);
        }
    }

    /**
     * 本文の編集
     * @param args
     * @return URL用のアイテムだった場合は本文を編集しない。
     */
    private void setContentAtAsync(Bundle args) {
        final long itemId = args.getLong(ITEM_ID);
        Memo single = findById(memos, itemId);
        if (single == null || single.isForUrl() || !args.containsKey(InputDialogFragment.RESULT)) {
            LogUtils.w("Something wrong. " + single);
        } else {
            single.setMemo(args.getString(InputDialogFragment.RESULT, ""));
            RxBusProvider.getInstance().post(new RequestDataSaveEvent(single));
        }
    }

    /**
     * 出典情報の編集。URL以外からURLに変化した場合は、再読込は行わない。
     * @param args
     */
    private void setCitationResourceAt(Bundle args) {
        final long itemId = args.getLong(ITEM_ID);
        Memo single = findById(memos, itemId);
        if (single != null && args.containsKey(InputDialogFragment.RESULT)) {
            boolean isForUrlCurrently = single.isForUrl();
            List<CitationResource> citationResources = single.getCitationResources();
            if (ArrayUtils.any(citationResources)) {
                citationResources.set(0, new CitationResource(args.getString(InputDialogFragment.RESULT, "")));
            } else {
                single.addCitationResource(args.getString(InputDialogFragment.RESULT, ""));
            }
            single.setLoaded(true);
            if (memoRepository.save(single)) {
                mainListAdapter.notifyItemChanged(memos.indexOf(single));
                renewCitationResources();
                if (isForUrlCurrently) {
                    mainActivityHelper.forceReloadPreviewAsync(single);
                }
            }
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

    /**
     * 長押しから削除（Snackbarによる猶予つき）
     * @param memo
     * @param itemId
     */
    private void deleteMemoWithSomeStay(Memo memo, final long itemId) {

        memo.setRemoved(true);
        mainListAdapter.notifyItemChanged(memos.indexOf(memo));

        final LinearLayout layout = (LinearLayout) findViewById(R.id.snackbar);
        //snackbar の表示
        snackbar = Snackbar.make(layout, "削除しました", Snackbar.LENGTH_LONG)
                .setAction("戻す", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Memo single = findById(memos, itemId);
                        if (single != null) {
                            single.setRemoved(false);
                            int index = memos.indexOf(single);
                            mainListAdapter.notifyItemChanged(index);
                            mainRecyclerView.smoothScrollToPosition(index);
                        }
                    }
                })
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        Memo single = findById(memos, itemId);
                        if (single != null) {
                            if (single.isRemoved()) {
                                deleteMemoAsync(single);
                            }
                        }
                    }
                });
        snackbar.show();
    }

    private void shareContent(final Memo memo) {

        String content;
        if (memo.isForUrl() && !memo.isLoaded()) {
            content = memo.getCitationResource();
        } else {
            content = StringUtils.build(memo.getMemo(), StringUtils.getCRLF(), memo.getCitationResource());
        }
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, content);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    /**
     * RecyclerViewの移動イベントを受け取ります
     */
    private ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (isInReorderMode) {
                final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                final int swipeFlags = 0;
                return makeMovementFlags(dragFlags, swipeFlags);
            } else {
                return makeMovementFlags(0, 0);
            }
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            final int fromPos = viewHolder.getAdapterPosition();
            final int toPos = target.getAdapterPosition();
            mainListAdapter.onItemMoved(fromPos, toPos);
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                ((ChatLikeListAdapter.ChatLikeViewHolder) viewHolder).setSelected(true);
            }
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            ((ChatLikeListAdapter.ChatLikeViewHolder) viewHolder).setSelected(false);
        }
    });
}
