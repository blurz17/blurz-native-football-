package org.libsdl.app.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * GamePlan / Squad Management screen for Blurz Football.
 */
public class GamePlanActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BlurzTheme.applyImmersive(this);

        int dp8  = BlurzTheme.dpToPx(this, 8);
        int dp16 = BlurzTheme.dpToPx(this, 16);
        int dp24 = BlurzTheme.dpToPx(this, 24);
        int dp32 = BlurzTheme.dpToPx(this, 32);
        int dp64 = BlurzTheme.dpToPx(this, 64);

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
        backBtn.setOnClickListener(v -> finish());
        topBar.addView(backBtn);

        TextView title = new TextView(this);
        title.setText("SQUAD MANAGEMENT");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(BlurzTheme.FONT_BOLD);
        title.setTextColor(BlurzTheme.ACCENT_CYAN);
        title.setLetterSpacing(0.1f);
        topBar.addView(title);
        outer.addView(topBar);

        // ── Center Content ──
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(BlurzTheme.createCardBackground());
        card.setPadding(dp64, dp64, dp64, dp64);
        card.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText("NATIVE SQUAD MANAGEMENT IS UNDER CONSTRUCTION\n\nFor now, squad management is disabled in this native UI update.");
        info.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        info.setTypeface(BlurzTheme.FONT_MEDIUM);
        info.setTextColor(BlurzTheme.TEXT_PRIMARY);
        info.setGravity(Gravity.CENTER);
        
        card.addView(info);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        );
        cardParams.topMargin = dp32;
        cardParams.bottomMargin = dp32;
        outer.addView(card, cardParams);

        root.addView(outer, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);
    }
}
