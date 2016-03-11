package jp.ne.hatena.hackugyo.procon;

import android.app.Activity;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewAfterTextChangeEvent;
import com.squareup.picasso.Callback;
import com.squareup.picasso.DocumentExifTransformation;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import jp.ne.hatena.hackugyo.procon.util.EditTextUtils;
import jp.ne.hatena.hackugyo.procon.util.StringUtils;
import jp.ne.hatena.hackugyo.procon.util.UrlUtils;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 * Created by kwatanabe on 16/02/19.
 */
public class MainActivityViewProvider {
    private final Activity activity;
    private final ArrayAdapter citationResourceSuggestionAdapter;
    private final MainActivityViewProvider viewProvider = this;
    private final Picasso picasso;

    EditText contentEditText;
    EditText pagesEditText;
    AutoCompleteTextView citationResourceEditText;
    BootstrapButton addAsProButton, addAsConButton, otherButton;
    private ProgressBar pbLoadingBar;
    private ViewGroup imageContainer;
    ImageView imageView;
    private Uri imageUri;

    public MainActivityViewProvider(Activity activity, ArrayAdapter citationResourceSuggestionAdapter, BootstrapButton proButton, BootstrapButton conButton, BootstrapButton otherButton) {
        this.activity = activity;
        picasso = Picasso.with(activity);
        this.citationResourceSuggestionAdapter = citationResourceSuggestionAdapter;

        contentEditText = (EditText) findViewById(R.id.editText);
        citationResourceEditText = (AutoCompleteTextView) findViewById(R.id.editText_from);
        citationResourceEditText.setAdapter(this.citationResourceSuggestionAdapter);
        citationResourceEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && citationResourceEditText.getAdapter().getCount() > 0) citationResourceEditText.showDropDown();
            }
        });
        pagesEditText = (EditText) findViewById(R.id.editText_pages);
        addAsProButton = proButton;
        addAsConButton = conButton;
        this.otherButton = otherButton;

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
                                        return UrlUtils.isValidWebUrl(textViewAfterTextChangeEvent.editable().toString());
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
                        addAsConButton.setEnabled(isValid);
                    }
                });
    }

    private View findViewById(int id) {
        return activity.findViewById(id);
    }

    public void resetInputTexts(View currentView) {
        EditTextUtils.closeKeyboard(activity, currentView);

        viewProvider.contentEditText.getEditableText().clear();
        viewProvider.citationResourceEditText.getEditableText().clear();
        viewProvider.citationResourceEditText.setEnabled(true);
        viewProvider.pagesEditText.getEditableText().clear();

        if (imageContainer == null) imageContainer = (ViewGroup) findViewById(R.id.imageViewContainer);
        imageContainer.setVisibility(View.GONE);

    }

    public void resumeInputTexts(String memo, String citationResource, String pages) {

        viewProvider.contentEditText.setText(memo);
        viewProvider.citationResourceEditText.setText(citationResource);
        viewProvider.citationResourceEditText.setEnabled(true);
        viewProvider.pagesEditText.setText(pages);
    }

    /**
     * notifyDataSetChangedだと正しく更新されない
     * @param citationResources
     */
    public void resetCitationResourceSuggestionAdapterAsync(List<String> citationResources) {
        Observable.just(new ArrayList<String>(citationResources))
                .observeOn(AndroidSchedulers.mainThread())
                .single(
                        new Func1<List<String>, Boolean>() {
                            @Override
                            public Boolean call(List<String> strings) {
                                citationResourceSuggestionAdapter.setNotifyOnChange(true);
                                citationResourceSuggestionAdapter.clear();
                                citationResourceSuggestionAdapter.addAll(strings);
                                // 不要。addAllで呼ばれている citationResourceSuggestionAdapter.notifyDataSetChanged();
                                citationResourceEditText.setAdapter(citationResourceSuggestionAdapter);
                                return true;
                            }
                        })
                .subscribe();
    }

    public void setImage(Uri resultUri) {
        if (pbLoadingBar == null) pbLoadingBar = (ProgressBar) findViewById(R.id.imageViewProgress);
        if (imageContainer == null) imageContainer = (ViewGroup) findViewById(R.id.imageViewContainer);
        imageContainer.setVisibility(View.VISIBLE);
        pbLoadingBar.setVisibility(View.VISIBLE);
        // 画像を設定

        picasso.cancelRequest(getImageView());
        imageUri = resultUri;
        picasso
                .load(resultUri)
                .noPlaceholder()
                .error(R.drawable.ic_action_reload)
                .fit().centerInside()
                .transform(new DocumentExifTransformation(activity, resultUri))
                .into(getImageView(), new Callback() {
                    @Override
                    public void onSuccess() {
                        pbLoadingBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError() {
                        pbLoadingBar.setVisibility(View.GONE);
                        imageUri = null;
                    }
                });
    }

    public void setImageView(ImageView view) {
        imageView = view;
    }

    private ImageView getImageView() {
        if (imageView == null) {
            imageView = (ImageView)findViewById(R.id.imageView);
        }
        return imageView;
    }

    public Uri getImageUri() {
        return imageUri;
    }
}
