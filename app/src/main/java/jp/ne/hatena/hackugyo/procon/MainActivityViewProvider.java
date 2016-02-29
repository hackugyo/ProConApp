package jp.ne.hatena.hackugyo.procon;

import android.app.Activity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewAfterTextChangeEvent;

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


    EditText contentEditText;
    EditText pagesEditText;
    AutoCompleteTextView citationResourceEditText;
    Button addAsProButton, addAsConButton;

    public MainActivityViewProvider(Activity activity, ArrayAdapter citationResourceSuggestionAdapter, Button proButton, Button conButton) {
        this.activity = activity;
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
        viewProvider.pagesEditText.getEditableText().clear();
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

}
