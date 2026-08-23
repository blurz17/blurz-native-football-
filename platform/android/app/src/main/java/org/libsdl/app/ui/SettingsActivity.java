package org.libsdl.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Settings screen for Blurz Football.
 * Replaces the old C++ SettingsPage.
 * Only Gameplay and Audio — no Graphics or Controller pages (mobile).
 */
public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BlurzTheme.applyImmersive(this);

        int dp8  = BlurzTheme.dpToPx(this, 8);
        int dp12 = BlurzTheme.dpToPx(this, 12);
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
        backBtn.setOnClickListener(v -> finish());
        topBar.addView(backBtn);

        TextView title = new TextView(this);
        title.setText("SETTINGS");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        title.setTypeface(BlurzTheme.FONT_BOLD);
        title.setTextColor(BlurzTheme.ACCENT_CYAN);
        title.setLetterSpacing(0.1f);
        topBar.addView(title);

        main.addView(topBar);

        // ── Buttons ──
        LinearLayout buttonContainer = new LinearLayout(this);
        buttonContainer.setOrientation(LinearLayout.VERTICAL);
        buttonContainer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        );
        containerParams.topMargin = dp32;

        String[] labels = { "⚙  GAMEPLAY", "🔊  AUDIO" };
        Class<?>[] targets = { GameplaySettingsActivity.class, AudioSettingsActivity.class };

        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView btn = new TextView(this);
            btn.setText(labels[i]);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            btn.setTypeface(BlurzTheme.FONT_BOLD);
            btn.setTextColor(BlurzTheme.TEXT_PRIMARY);
            btn.setGravity(Gravity.CENTER_VERTICAL);
            btn.setPadding(dp24, 0, dp24, 0);
            btn.setBackground(BlurzTheme.createCardBackground());

            btn.setOnClickListener(v -> startActivity(new Intent(this, targets[idx])));
            btn.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                        break;
                }
                return false;
            });

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                BlurzTheme.dpToPx(this, 280), BlurzTheme.dpToPx(this, 56)
            );
            btnParams.gravity = Gravity.CENTER_HORIZONTAL;
            btnParams.bottomMargin = dp12;
            buttonContainer.addView(btn, btnParams);
        }

        main.addView(buttonContainer, containerParams);

        root.addView(main, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);
    }
}
