package org.libsdl.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ScrollView;

/**
 * Match Options screen for Blurz Football.
 * Replaces the old C++ MatchOptionsPage.
 * Difficulty slider + match duration slider.
 */
public class MatchOptionsActivity extends Activity {

    private static final String PREFS = "blurz_settings";

    private SeekBar difficultyBar;
    private SeekBar durationBar;
    private TextView difficultyValue;
    private TextView durationValue;

    private TextView startBtn;
    private android.widget.ProgressBar loader;
    private LinearLayout main;
    private boolean isLaunching = false;

    private int homeTeamId;
    private int awayTeamId;

    @Override
    protected void onResume() {
        super.onResume();
        if (isLaunching) {
            isLaunching = false;
            if (loader != null && main != null) {
                main.removeView(loader);
            }
            if (startBtn != null && main != null) {
                if (startBtn.getParent() == null) {
                    LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    startParams.gravity = Gravity.CENTER_HORIZONTAL;
                    startParams.topMargin = BlurzTheme.dpToPx(this, 32);
                    main.addView(startBtn, startParams);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BlurzTheme.applyImmersive(this);

        homeTeamId = getIntent().getIntExtra("homeTeamId", 3);
        awayTeamId = getIntent().getIntExtra("awayTeamId", 8);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int savedDifficulty = prefs.getInt("match_difficulty", 50); // 0-100, maps to 0.0-1.0
        int savedDuration = prefs.getInt("match_duration", 50);

        int dp8  = BlurzTheme.dpToPx(this, 8);
        int dp12 = BlurzTheme.dpToPx(this, 12);
        int dp16 = BlurzTheme.dpToPx(this, 16);
        int dp24 = BlurzTheme.dpToPx(this, 24);
        int dp32 = BlurzTheme.dpToPx(this, 32);
        int dp48 = BlurzTheme.dpToPx(this, 48);
        int dp64 = BlurzTheme.dpToPx(this, 64);

        // ── Root ──
        FrameLayout root = new FrameLayout(this);
        root.setBackground(BlurzTheme.createBackground());

        ScrollView mainScroll = new ScrollView(this);
        mainScroll.setFillViewport(true);

        main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setGravity(Gravity.CENTER);
        main.setPadding(dp32, dp16, dp32, dp16);

        // ── Top Bar ──
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView backBtn = new TextView(this);
        backBtn.setText("←");
        backBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        backBtn.setTextColor(BlurzTheme.TEXT_PRIMARY);
        backBtn.setPadding(dp8, dp8, dp24, dp8);
        backBtn.setOnClickListener(v -> finish());
        topBar.addView(backBtn);

        TextView title = new TextView(this);
        title.setText("MATCH OPTIONS");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        title.setTypeface(BlurzTheme.FONT_BOLD);
        title.setTextColor(BlurzTheme.ACCENT_CYAN);
        title.setLetterSpacing(0.1f);
        topBar.addView(title);

        main.addView(topBar);

        // ── Options Card ──
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(BlurzTheme.createCardBackground());
        card.setPadding(dp32, dp32, dp32, dp32);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dp24;

        // ── Difficulty Slider ──
        TextView diffLabel = new TextView(this);
        diffLabel.setText("DIFFICULTY (vs CPU)");
        diffLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        diffLabel.setTypeface(BlurzTheme.FONT_BOLD);
        diffLabel.setTextColor(BlurzTheme.TEXT_SECONDARY);
        diffLabel.setLetterSpacing(0.15f);
        card.addView(diffLabel);

        LinearLayout diffRow = new LinearLayout(this);
        diffRow.setOrientation(LinearLayout.HORIZONTAL);
        diffRow.setGravity(Gravity.CENTER_VERTICAL);
        diffRow.setPadding(0, dp8, 0, dp24);

        difficultyBar = new SeekBar(this);
        difficultyBar.setMax(100);
        difficultyBar.setProgress(savedDifficulty);
        difficultyBar.getProgressDrawable().setColorFilter(BlurzTheme.ACCENT_CYAN, android.graphics.PorterDuff.Mode.SRC_IN);
        difficultyBar.getThumb().setColorFilter(BlurzTheme.ACCENT_CYAN, android.graphics.PorterDuff.Mode.SRC_IN);

        difficultyValue = new TextView(this);
        difficultyValue.setText(getDifficultyLabel(savedDifficulty));
        difficultyValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        difficultyValue.setTypeface(BlurzTheme.FONT_MEDIUM);
        difficultyValue.setTextColor(BlurzTheme.TEXT_PRIMARY);
        difficultyValue.setPadding(dp16, 0, 0, 0);

        difficultyBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                difficultyValue.setText(getDifficultyLabel(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        diffRow.addView(difficultyBar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        diffRow.addView(difficultyValue);
        card.addView(diffRow);

        // ── Duration Slider ──
        TextView durLabel = new TextView(this);
        durLabel.setText("MATCH DURATION");
        durLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        durLabel.setTypeface(BlurzTheme.FONT_BOLD);
        durLabel.setTextColor(BlurzTheme.TEXT_SECONDARY);
        durLabel.setLetterSpacing(0.15f);
        card.addView(durLabel);

        LinearLayout durRow = new LinearLayout(this);
        durRow.setOrientation(LinearLayout.HORIZONTAL);
        durRow.setGravity(Gravity.CENTER_VERTICAL);
        durRow.setPadding(0, dp8, 0, dp24);

        durationBar = new SeekBar(this);
        durationBar.setMax(100);
        durationBar.setProgress(savedDuration);
        durationBar.getProgressDrawable().setColorFilter(BlurzTheme.ACCENT_CYAN, android.graphics.PorterDuff.Mode.SRC_IN);
        durationBar.getThumb().setColorFilter(BlurzTheme.ACCENT_CYAN, android.graphics.PorterDuff.Mode.SRC_IN);

        durationValue = new TextView(this);
        durationValue.setText(getDurationLabel(savedDuration));
        durationValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        durationValue.setTypeface(BlurzTheme.FONT_MEDIUM);
        durationValue.setTextColor(BlurzTheme.TEXT_PRIMARY);
        durationValue.setPadding(dp16, 0, 0, 0);

        durationBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                durationValue.setText(getDurationLabel(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        durRow.addView(durationBar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        durRow.addView(durationValue);
        card.addView(durRow);

        main.addView(card, cardParams);

        // ── START MATCH Button ──
        startBtn = new TextView(this);
        startBtn.setText("START MATCH  ▶");
        startBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        startBtn.setTypeface(BlurzTheme.FONT_BOLD);
        startBtn.setTextColor(Color.WHITE);
        startBtn.setGravity(Gravity.CENTER);
        startBtn.setBackground(BlurzTheme.createButtonBackground(BlurzTheme.ACCENT_GREEN));
        startBtn.setShadowLayer(16, 0, 4, Color.parseColor("#6600E676"));
        startBtn.setPadding(dp32, dp12, dp32, dp12);
        startBtn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    break;
            }
            return false;
        });

        startBtn.setOnClickListener(v -> {
            isLaunching = true;
            // Save settings
            float difficulty = difficultyBar.getProgress() / 100f;
            float duration = durationBar.getProgress() / 100f;
            prefs.edit()
                .putInt("match_difficulty", difficultyBar.getProgress())
                .putInt("match_duration", durationBar.getProgress())
                .apply();

            // Hide start button and show loader
            startBtn.setVisibility(View.GONE);
            if (loader == null) {
                loader = new android.widget.ProgressBar(this);
                loader.getIndeterminateDrawable().setColorFilter(BlurzTheme.ACCENT_GREEN, android.graphics.PorterDuff.Mode.SRC_IN);
                LinearLayout.LayoutParams loaderParams = new LinearLayout.LayoutParams(dp48, dp48);
                loaderParams.gravity = Gravity.CENTER_HORIZONTAL;
                loaderParams.topMargin = dp32;
                main.addView(loader, loaderParams);
            }
            loader.setVisibility(View.VISIBLE);

            // Launch SDLActivity without animation
            Intent intent = new Intent(this, org.libsdl.app.SDLActivity.class);
            intent.putExtra("startQuickMatch", true);
            intent.putExtra("homeTeamId", String.valueOf(homeTeamId));
            intent.putExtra("awayTeamId", String.valueOf(awayTeamId));
            intent.putExtra("matchDifficulty", String.valueOf(difficulty));
            intent.putExtra("matchDuration", String.valueOf(duration));
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            finish();
        });

        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        startParams.gravity = Gravity.CENTER_HORIZONTAL;
        startParams.topMargin = dp32;
        main.addView(startBtn, startParams);

        mainScroll.addView(main);
        root.addView(mainScroll, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);
    }

    private String getDifficultyLabel(int progress) {
        if (progress < 20) return "Very Easy";
        if (progress < 40) return "Easy";
        if (progress < 60) return "Normal";
        if (progress < 80) return "Hard";
        return "Legendary";
    }

    private String getDurationLabel(int progress) {
        // Maps 0-100 to 5-25 minutes
        int minutes = 5 + (int)(progress / 100f * 20);
        return minutes + " min";
    }
}
