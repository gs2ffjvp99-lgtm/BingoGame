package com.example.bingogame;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bingogame.data.BingoDbHelper;
import com.example.bingogame.data.BingoRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * RecordActivity lists every Bingo saved in the SQLite database:
 * username, round number, the five Bingo numbers and the system time.
 *
 * <p>It reads the data with {@link BingoDbHelper#getAllRecords()} and shows
 * it in a {@link ListView}. A button lets the user delete all records.</p>
 */
public class RecordActivity extends Activity {

    /** Access to the SQLite database. */
    private BingoDbHelper dbHelper;

    /** Turns each text line into a row of the ListView. */
    private ArrayAdapter<String> adapter;

    /** Shows how many records are saved. */
    private TextView countText;

    /** Clears the table; disabled when there is nothing to delete. */
    private Button clearButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        dbHelper = new BingoDbHelper(this);

        // Find views from res/layout/activity_record.xml.
        countText = findViewById(R.id.text_record_count);
        ListView listView = findViewById(R.id.list_records);
        clearButton = findViewById(R.id.button_clear_records);

        // ListView shows "emptyView" automatically when the adapter has no rows.
        listView.setEmptyView(findViewById(R.id.text_empty));

        // simple_list_item_1 is a ready-made one-TextView row from Android.
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(adapter);

        clearButton.setOnClickListener(view -> confirmClearRecords());
    }

    /**
     * Reload every time the screen becomes visible, so new wins appear
     * even when the user comes back to an already open records screen.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadRecords();
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }

    /** Reads the table (SELECT) and refreshes the list. */
    private void loadRecords() {
        List<BingoRecord> records = dbHelper.getAllRecords();

        adapter.clear();
        for (BingoRecord record : records) {
            adapter.add(formatRecord(record));
        }
        adapter.notifyDataSetChanged();

        countText.setText(getString(R.string.label_record_count, records.size()));
        clearButton.setEnabled(!records.isEmpty());
    }

    /** Builds the text of one list row from one database row. */
    private String formatRecord(BingoRecord record) {
        return getString(R.string.record_item,
                record.getId(),
                record.getUsername(),
                record.getRoundNumber(),
                record.getBingoNumbersText(),
                record.getSystemTime());
    }

    /** Asks for confirmation before deleting, because it cannot be undone. */
    private void confirmClearRecords() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_clear_title)
                .setMessage(R.string.dialog_clear_message)
                .setPositiveButton(R.string.button_delete, (dialog, which) -> {
                    int deleted = dbHelper.deleteAllRecords();
                    Toast.makeText(this, getString(R.string.toast_records_cleared, deleted),
                            Toast.LENGTH_SHORT).show();
                    loadRecords();
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }
}
