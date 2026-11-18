package com.example.bingogame;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

/**
 * MainActivity is the start screen (launcher activity).
 *
 * <p>The player types a username, which is passed to {@link GameActivity}
 * and later saved in the database together with each Bingo.</p>
 *
 * <p>The app uses the built-in {@link Activity} class instead of
 * AppCompatActivity, so it needs no extra libraries and can be copied into
 * any Android project.</p>
 */
public class MainActivity extends Activity {

    /** Text box where the player enters their name. */
    private EditText usernameInput;

    /**
     * Called when the screen is created: load the layout and connect the
     * buttons to their actions.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the views defined in res/layout/activity_main.xml.
        usernameInput = findViewById(R.id.edit_username);
        Button startButton = findViewById(R.id.button_start);
        Button recordsButton = findViewById(R.id.button_view_records);

        // Button actions (lambda = short way to write an OnClickListener).
        startButton.setOnClickListener(view -> startGame());
        recordsButton.setOnClickListener(view ->
                startActivity(new Intent(this, RecordActivity.class)));
    }

    /**
     * Checks the username and opens the game screen.
     * An empty name is not allowed, because every record must have an owner.
     */
    private void startGame() {
        // trim() removes spaces at the start and end, so "   " counts as empty.
        String username = usernameInput.getText().toString().trim();

        if (username.isEmpty()) {
            usernameInput.setError(getString(R.string.error_username_empty));
            usernameInput.requestFocus();
            return;
        }

        // An Intent "extra" carries the username to the next screen.
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_USERNAME, username);
        startActivity(intent);
    }
}
