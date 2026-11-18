package com.example.bingogame;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bingogame.data.BingoDbHelper;
import com.example.bingogame.game.BingoBoard;
import com.example.bingogame.game.BingoGame;

import java.io.Serializable;

/**
 * GameActivity shows the player's 5 x 5 Bingo card and runs the match.
 *
 * <p>Responsibilities (the rules themselves live in {@link BingoGame}):</p>
 * <ul>
 *   <li>Create the 25 number cells and listen for taps.</li>
 *   <li>Pass each tap to {@link BingoGame#playRound(int)}.</li>
 *   <li>Redraw the card, round counter and status text after every move.</li>
 *   <li>When the player gets Bingo, save username, round number, the five
 *       numbers and the system time with {@link BingoDbHelper}.</li>
 *   <li>Keep the game when the screen rotates (onSaveInstanceState).</li>
 * </ul>
 */
public class GameActivity extends Activity {

    /** Intent extra key for the username sent by MainActivity. */
    public static final String EXTRA_USERNAME = "com.example.bingogame.EXTRA_USERNAME";

    /** Bundle key used to keep the game alive across screen rotation. */
    private static final String STATE_GAME = "state_game";

    /** Text size of the numbers on the card, in sp. */
    private static final float CELL_TEXT_SIZE_SP = 22f;

    /** Current match (rules + state). */
    private BingoGame game;

    /** Access to the SQLite database. */
    private BingoDbHelper dbHelper;

    /** Name entered on the start screen. */
    private String username;

    /** The 25 cells of the card; cellViews[i] shows the number in cell i. */
    private final TextView[] cellViews = new TextView[BingoBoard.CELL_COUNT];

    private TextView roundText;
    private TextView computerText;
    private TextView statusText;

    /** Kept so the dialog can be closed if the Activity is destroyed. */
    private AlertDialog gameOverDialog;

    // ------------------------------------------------------------------
    // Activity lifecycle
    // ------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // 1. Read the username sent from MainActivity (fallback if missing).
        username = getIntent().getStringExtra(EXTRA_USERNAME);
        if (username == null || username.trim().isEmpty()) {
            username = getString(R.string.default_username);
        }

        // 2. Open the database helper (the database file is created on first use).
        dbHelper = new BingoDbHelper(this);

        // 3. Find the views from res/layout/activity_game.xml.
        TextView playerText = findViewById(R.id.text_player);
        roundText = findViewById(R.id.text_round);
        computerText = findViewById(R.id.text_computer);
        statusText = findViewById(R.id.text_status);
        playerText.setText(getString(R.string.label_player, username));

        findViewById(R.id.button_new_game).setOnClickListener(view -> startNewGame());
        findViewById(R.id.button_records).setOnClickListener(view -> openRecords());

        // 4. Restore the running game after a rotation, or start a new one.
        if (savedInstanceState != null) {
            game = readSavedGame(savedInstanceState);
        }
        if (game == null) {
            game = new BingoGame();
        }

        // 5. Build the 25 cells and draw everything.
        createBoardCells();
        refreshUi();
    }

    /** Saves the game before Android destroys the Activity (e.g. on rotation). */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_GAME, game);
    }

    /** Releases resources when the screen is closed. */
    @Override
    protected void onDestroy() {
        if (gameOverDialog != null && gameOverDialog.isShowing()) {
            gameOverDialog.dismiss(); // avoids a "window leaked" error on rotation
        }
        dbHelper.close();
        super.onDestroy();
    }

    // ------------------------------------------------------------------
    // Building and drawing the card
    // ------------------------------------------------------------------

    /**
     * Creates 25 TextViews in code (instead of 25 copies in XML) and puts
     * them into the 5 x 5 grid. Each cell gets weight 1 in its row and
     * column, so all cells share the space equally.
     */
    private void createBoardCells() {
        GridLayout grid = findViewById(R.id.grid_board);
        grid.removeAllViews();
        grid.setRowCount(BingoBoard.SIZE);
        grid.setColumnCount(BingoBoard.SIZE);

        int margin = getResources().getDimensionPixelSize(R.dimen.cell_margin);

        for (int i = 0; i < BingoBoard.CELL_COUNT; i++) {
            TextView cell = new TextView(this);
            cell.setGravity(Gravity.CENTER);
            cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, CELL_TEXT_SIZE_SP);
            cell.setTypeface(Typeface.DEFAULT_BOLD);
            cell.setClickable(true);
            cell.setFocusable(true);

            // row = i / 5, column = i % 5; width/height 0 + weight 1 = equal size.
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(i / BingoBoard.SIZE, 1f),
                    GridLayout.spec(i % BingoBoard.SIZE, 1f));
            params.width = 0;
            params.height = 0;
            params.setMargins(margin, margin, margin, margin);

            // A lambda can only use "effectively final" variables, so copy i.
            final int index = i;
            cell.setOnClickListener(view -> onCellClicked(index));

            grid.addView(cell, params);
            cellViews[i] = cell;
        }
    }

    /**
     * Redraws the whole screen from the current game state.
     * Colours: white = not called, blue = called by you,
     * magenta = called by the computer, yellow = your Bingo line.
     */
    private void refreshUi() {
        boolean playerWon = game.getWinner() == BingoGame.Winner.PLAYER;

        for (int i = 0; i < BingoBoard.CELL_COUNT; i++) {
            TextView cell = cellViews[i];
            int number = game.getPlayerNumberAt(i);
            BingoGame.Caller caller = game.getCaller(number);

            cell.setText(String.valueOf(number));

            if (playerWon && game.isBingoNumber(number)) {
                styleCell(cell, R.color.cell_bingo, R.color.text_primary);
            } else if (caller == BingoGame.Caller.PLAYER) {
                styleCell(cell, R.color.cell_player, R.color.text_on_color);
            } else if (caller == BingoGame.Caller.COMPUTER) {
                styleCell(cell, R.color.cell_computer, R.color.text_on_color);
            } else {
                styleCell(cell, R.color.cell_empty, R.color.text_primary);
            }

            // Only uncalled numbers can be tapped, and only while the game runs.
            cell.setEnabled(caller == null && !game.isOver());
        }

        roundText.setText(getString(R.string.label_round, game.getRoundNumber()));
        computerText.setText(getString(R.string.label_computer_progress,
                game.getComputerBestLineProgress(), BingoBoard.SIZE));
        statusText.setText(buildStatusMessage());
    }

    /**
     * Gives a cell a rounded background in the requested colours.
     * GradientDrawable is a shape drawn in code, so no image files are needed.
     */
    private void styleCell(TextView cell, int fillColorRes, int textColorRes) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(getResources().getDimension(R.dimen.cell_corner_radius));
        background.setColor(getColor(fillColorRes));
        background.setStroke(getResources().getDimensionPixelSize(R.dimen.cell_stroke),
                getColor(R.color.cell_border));

        cell.setBackground(background);
        cell.setTextColor(getColor(textColorRes));
    }

    /** @return the sentence shown under the card for the current game state. */
    private String buildStatusMessage() {
        String numbers = BingoGame.formatNumbers(game.getBingoNumbers());

        switch (game.getWinner()) {
            case PLAYER:
                return getString(R.string.status_player_won, game.getRoundNumber(), numbers);
            case COMPUTER:
                return getString(R.string.status_computer_won, game.getRoundNumber(), numbers);
            default:
                if (game.getLastComputerNumber() == 0) {
                    return getString(R.string.status_start, username); // first move
                }
                return getString(R.string.status_computer_called, game.getLastComputerNumber());
        }
    }

    // ------------------------------------------------------------------
    // User actions
    // ------------------------------------------------------------------

    /**
     * Runs when the player taps a cell.
     *
     * @param index which cell was tapped (0..24)
     */
    private void onCellClicked(int index) {
        int number = game.getPlayerNumberAt(index);

        // playRound() returns false if the tap is not allowed (e.g. double tap).
        if (!game.playRound(number)) {
            return;
        }

        // Save to SQLite only when the PLAYER gets Bingo.
        if (game.getWinner() == BingoGame.Winner.PLAYER) {
            saveBingoRecord();
        }

        refreshUi();

        if (game.isOver()) {
            showGameOverDialog();
        }
    }

    /**
     * Inserts the four required attributes into the database:
     * (i) username, (ii) round number, (iii) the five Bingo numbers and
     * (iv) the system time (added inside BingoDbHelper).
     */
    private void saveBingoRecord() {
        long rowId = dbHelper.insertRecord(username, game.getRoundNumber(), game.getBingoNumbers());

        // SQLiteDatabase.insert() returns -1 when the insert fails.
        int message = (rowId == -1) ? R.string.toast_record_failed : R.string.toast_record_saved;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /** Pops up the result with buttons to play again or see the records. */
    private void showGameOverDialog() {
        boolean playerWon = game.getWinner() == BingoGame.Winner.PLAYER;

        gameOverDialog = new AlertDialog.Builder(this)
                .setTitle(playerWon ? R.string.dialog_title_win : R.string.dialog_title_lose)
                .setMessage(buildStatusMessage())
                .setPositiveButton(R.string.button_play_again, (dialog, which) -> startNewGame())
                .setNeutralButton(R.string.button_view_records, (dialog, which) -> openRecords())
                .setNegativeButton(R.string.button_close, null) // null = just close the dialog
                .show();
    }

    /** Throws away the current match and deals two new cards. */
    private void startNewGame() {
        game = new BingoGame();
        refreshUi();
    }

    /** Opens the records screen. */
    private void openRecords() {
        startActivity(new Intent(this, RecordActivity.class));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Reads the saved game from a Bundle. Android 13 (API 33) added a new,
     * type-safe getSerializable(); older versions use the original method.
     */
    @SuppressWarnings("deprecation")
    private static BingoGame readSavedGame(Bundle state) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return state.getSerializable(STATE_GAME, BingoGame.class);
        }
        Serializable saved = state.getSerializable(STATE_GAME);
        return (saved instanceof BingoGame) ? (BingoGame) saved : null;
    }
}
