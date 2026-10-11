package org.libsdl.app.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Audio Settings screen for Blurz Football.
 * Replaces the old C++ AudioPage.
 */
public class AudioSettingsActivity extends Activity {

    private static final String PREFS = "blurz_settings";
    private SeekBar volumeBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BlurzTheme.applyImmersive(this);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int savedVolume = prefs.getInt("audio_volume", 80);

        int dp8  = BlurzTheme.dpToPx(this, 8);
        int dp12 = BlurzTheme.dpToPx(this, 12);
        int dp16 = BlurzTheme.dpToPx(this, 16);
        int dp24 = BlurzTheme.dpToPx(this, 24);
        int dp32 = BlurzTheme.dpToPx(this, 32);
        int dp64 = BlurzTheme.dpToPx(this, 64);

        FrameLayout root = new FrameLayout(this);
        root.setBackground(BlurzTheme.createBackground());

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp64, dp32, dp64, dp32);

        // ── Top Bar ──
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView backBtn = new TextView(this);
        backBtn.setText("←");
        backBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        backBtn.setTextColor(BlurzTheme.TEXT_PRIMARY);
        backBtn.setPadding(dp8, dp8, dp24, dp8);
        backBtn.setOnClickListener(v -> saveAndFinish(prefs));
        topBar.addView(backBtn);

        TextView title = new TextView(this);
        title.setText("AUDIO");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(BlurzTheme.FONT_BOLD);
        title.setTextColor(BlurzTheme.ACCENT_CYAN);
        title.setLetterSpacing(0.1f);
        topBar.addView(title);
        main.addView(topBar);

        // ── Volume Card ──
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(BlurzTheme.createCardBackground());
        card.setPadding(dp32, dp32, dp32, dp32);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dp32;

        TextView volLabel = new TextView(this);
        volLabel.setText("MASTER VOLUME");
        volLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        volLabel.setTypeface(BlurzTheme.FONT_BOLD);
        volLabel.setTextColor(BlurzTheme.TEXT_SECONDARY);
        volLabel.setLetterSpacing(0.15f);
        card.addView(volLabel);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp16, 0, 0);

        // Speaker icon
        TextView speakerIcon = new TextView(this);
        speakerIcon.setText("🔊");
        speakerIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        speakerIcon.setPadding(0, 0, dp16, 0);
        row.addView(speakerIcon);

        volumeBar = new SeekBar(this);
        volumeBar.setMax(100);
        volumeBar.setProgress(savedVolume);
        volumeBar.getProgressDrawable().setColorFilter(BlurzTheme.ACCENT_CYAN, android.graphics.PorterDuff.Mode.SRC_IN);
        volumeBar.getThumb().setColorFilter(BlurzTheme.ACCENT_CYAN, android.graphics.PorterDuff.Mode.SRC_IN);

        final TextView volValue = new TextView(this);
        volValue.setText(savedVolume + "%");
        volValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        volValue.setTypeface(BlurzTheme.FONT_MEDIUM);
        volValue.setTextColor(BlurzTheme.TEXT_PRIMARY);
        volValue.setPadding(dp16, 0, 0, 0);
        volValue.setMinWidth(BlurzTheme.dpToPx(this, 52));

        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volValue.setText(progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        row.addView(volumeBar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(volValue);
        card.addView(row);

        main.addView(card, cardParams);

        root.addView(main, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);
    }

    private void saveAndFinish(SharedPreferences prefs) {
        int vol = volumeBar.getProgress();
        prefs.edit().putInt("audio_volume", vol).apply();
        try {
            org.libsdl.app.SDLActivity.setAudioVolume(vol / 100f);
        } catch (Throwable ignored) {}
        finish();
    }

    @Override
    public void onBackPressed() {
        saveAndFinish(getSharedPreferences(PREFS, MODE_PRIVATE));
    }
}
