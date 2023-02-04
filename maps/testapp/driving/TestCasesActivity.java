package com.yandex.maps.testapp.driving;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.yandex.maps.testapp.R;

import java.util.ArrayList;

public class TestCasesActivity extends Activity {
    private ArrayAdapter<TestCase> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.driving_testcases_activity);

        ListView testCasesListView = findViewById(R.id.driving_testcases_list);
        testCasesListView.setOnItemClickListener((adapterView, view, position, id) -> selectTestCase(position));

        arrayAdapter = new ArrayAdapter<TestCase>(this, android.R.layout.simple_list_item_2, android.R.id.text1, new ArrayList<>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TestCase testCase = getItem(position);

                final TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                text1.setText(testCase.getTitle());
                text1.setTextColor(Color.WHITE);

                final TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                text2.setText(testCase.getDescription());
                text2.setTextColor(Color.WHITE);

                return view;
            }
        };

        EditText filterTestCasesEdit = findViewById(R.id.driving_testcase_filter);
        filterTestCasesEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence query, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence query, int start, int before, int count) {
                updateTestCases(query.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });

        testCasesListView.setAdapter(arrayAdapter);

        updateTestCases("");
    }

    private void selectTestCase(int position) {
        TestCase testCase = arrayAdapter.getItem(position);
        Intent intent = new Intent();
        intent.putExtra("testCase", testCase);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void updateTestCases(String query) {
        arrayAdapter.addAll(TestCasesStorage.filteredTestCases(query));
    }
}
