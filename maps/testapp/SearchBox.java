package com.yandex.maps.testapp;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/// Generic search input widget
public class SearchBox extends LinearLayout {

    public interface SearchBoxListener {
        void onTextChanged(String text);

        void onSubmit(String text);

        void onOptionsClick();
    }

    private EditText searchBox;
    private ArrayList<SuggestResult> suggest = new ArrayList<SuggestResult>();
    private ArrayAdapter<SuggestResult> adapter;
    private SearchBoxListener listener;
    private Context context;
    private Button optionsButton;

    public SearchBox(Context context, AttributeSet attributes) {
        super(context, attributes);
        this.context = context;

        LayoutInflater inflater = (LayoutInflater)context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.search_box, this, true);
        setOrientation(LinearLayout.VERTICAL);

        setupSearchBox();
        setupSuggest();

        optionsButton = (Button)findViewById(R.id.search_options);
        optionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onOptionsClick();
                }
            }
        });

        Button clearButton = (Button)findViewById(R.id.search_box_clear);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchBox.setText("");
            }
        });
    }

    public String getText() {
        return ((TextView)searchBox).getText().toString();
    }

    public void setOptionsEnabled(boolean enabled) {
        optionsButton.setEnabled(enabled);
    }
    public void hideOptions() {
        optionsButton.setVisibility(View.GONE);
    }

    public void setSuggestVisibility(int visibility) {
        findViewById(R.id.search_suggest).setVisibility(visibility);
    }

    public void setText(String text) {
        ((TextView)searchBox).setText(text);
    }

    public void setSuggest(List<SuggestResult> suggest) {
        this.suggest.clear();
        this.suggest.addAll(suggest);
        adapter.notifyDataSetChanged();
    }

    public void setProgress(boolean visible) {
        findViewById(R.id.search_in_progress).setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    public void setListener(SearchBoxListener searchBoxListener) {
        listener = searchBoxListener;
    }

    public void hideSortButton() {
        findViewById(R.id.search_options).setVisibility(View.GONE);
    }

    public void hideKeyboard() {
        ((InputMethodManager) getContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(
                    searchBox.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void setupSearchBox() {
        searchBox = (EditText)findViewById(R.id.search_box);
        searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {

                if (listener != null) {
                    listener.onSubmit(searchBox.getText().toString());
                }

                hideKeyboard();

                return true;
            }
        });

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (listener != null) {
                    listener.onTextChanged(s.toString());
                }
            }
        });
    }

    private void setupSuggest() {
        ListView results = (ListView)findViewById(R.id.search_suggest);
        adapter = new ArrayAdapter<SuggestResult>(
                context,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                suggest) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
              View view = super.getView(position, convertView, parent);
              TextView text1 = (TextView) view.findViewById(android.R.id.text1);
              TextView text2 = (TextView) view.findViewById(android.R.id.text2);

              SuggestResult item = suggest.get(position);
              text1.setText(item.getTitle());
              text2.setText(TextUtils.concat(
                      item.getSubtitle(),
                      "\ntype: " + item.getType() +
                          ", tags: " + item.getTags() +
                          ", action: " + item.getAction() +
                      "\ndistance: " + item.getDistance()));
              return view;
            }
        };
        results.setAdapter(adapter);
        results.setClickable(true);
        results.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                new AlertDialog.Builder(context)
                    .setTitle("More information")
                    .setMessage("Action: " + suggest.get(position).getAction())
                    .show();
                setText(suggest.get(position).getDisplayText());
            }
        });
    }
}
