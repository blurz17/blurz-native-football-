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
 * Camera Settings screen for Blurz Football.
 */
public class CameraSettingsActivity extends Activity {

    private static final String PREFS = "blurz_settings";

    private SeekBar[] sliders = new SeekBar[3];
    private TextView[] valueLabels = new TextView[3];

    private static final String[] SLIDER_KEYS = {
        "camera_height", "camera_zoom", "camera_angle"
    };

    private static final String[] SLIDER_LABELS = {
        "CAMERA HEIGHT", "CAMERA ZOOM", "CAMERA ANGLE"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BlurzTheme.applyImmersive(this);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        int dp8  = BlurzTheme.dpToPx(this, 8);
        int dp12 = BlurzTheme.dpToPx(this, 12);
        int dp16 = BlurzTheme.dpToPx(this, 16);
        int dp24 = BlurzTheme.dpToPx(this, 24);
        int dp32 = BlurzTheme.dpToPx(this, 32);

        FrameLayout root = new FrameLayout(this);
        root.setBackground(BlurzTheme.createBackground());

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp32, dp16, dp32, dp16);

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
        title.setText("CAMERA SETTINGS");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(BlurzTheme.FONT_BOLD);
        title.setTextColor(BlurzTheme.ACCENT_CYAN);
        title.setLetterSpacing(0.1f);
        topBar.addView(title);
        outer.addView(topBar);

        // ── Scrollable sliders ──
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(BlurzTheme.createCardBackground());
        card.setPadding(dp24, dp24, dp24, dp24);

        for (int i = 0; i < SLIDER_KEYS.length; i++) {
            final int idx = i;
            int saved = prefs.getInt(SLIDER_KEYS[i], 50);

            TextView label = new TextView(this);
            label.setText(SLIDER_LABELS[i]);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            label.setTypeface(BlurzTheme.FONT_BOLD);
            label.setTextColor(BlurzTheme.TEXT_SECONDARY);
            label.setLetterSpacing(0.1f);
            if (i > 0) label.setPadding(0, dp16, 0, 0);
            card.addView(label);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp8, 0, 0);

            SeekBar bar = new SeekBar(this);
            bar.setMax(100);
            bar.setProgress(saved);
            bar.getProgressDrawable().setColorFilter(BlurzTheme.ACCENT_CYAN, android.graphics.PorterDuff.Mode.SRC_IN);
            bar.getThumb().setColorFilter(BlurzTheme.ACCENT_CYAN, android.graphics.PorterDuff.Mode.SRC_IN);
            sliders[i] = bar;

            TextView val = new TextView(this);
            val.setText(saved + "%");
            val.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            val.setTypeface(BlurzTheme.FONT_MEDIUM);
            val.setTextColor(BlurzTheme.TEXT_PRIMARY);
            val.setPadding(dp12, 0, 0, 0);
            val.setMinWidth(BlurzTheme.dpToPx(this, 48));
            valueLabels[i] = val;

            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    valueLabels[idx].setText(progress + "%");
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            row.addView(bar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(val);
            card.addView(row);
        }

        scroll.addView(card);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        );
        scrollParams.topMargin = dp12;
        outer.addView(scroll, scrollParams);

        root.addView(outer, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);
    }

    private void saveAndFinish(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < SLIDER_KEYS.length; i++) {
            editor.putInt(SLIDER_KEYS[i], sliders[i].getProgress());
        }
        editor.apply();

        try {
            int h = prefs.getInt("camera_height", 50);
            int z = prefs.getInt("camera_zoom", 50);
            int a = prefs.getInt("camera_angle", 50);
            float camHeight = 0.1f + (h / 100f) * 1.4f;
            float camZoom = 0.2f + (z / 100f) * 1.3f;
            float camAngle = a / 100f;
            org.libsdl.app.SDLActivity.setCameraSettings(camZoom, camHeight, 0.5f, camAngle);
        } catch (Throwable ignored) {}

        finish();
    }

    @Override
    public void onBackPressed() {
        saveAndFinish(getSharedPreferences(PREFS, MODE_PRIVATE));
    }
}
