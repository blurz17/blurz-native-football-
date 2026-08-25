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
 * Command List screen for Blurz Football.
 */
public class CommandListActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BlurzTheme.applyImmersive(this);

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
        backBtn.setOnClickListener(v -> finish());
        topBar.addView(backBtn);

        TextView title = new TextView(this);
        title.setText("COMMAND LIST");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(BlurzTheme.FONT_BOLD);
        title.setTextColor(BlurzTheme.ACCENT_CYAN);
        title.setLetterSpacing(0.1f);
        topBar.addView(title);
        outer.addView(topBar);

        // ── Scrollable list ──
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(BlurzTheme.createCardBackground());
        card.setPadding(dp24, dp24, dp24, dp24);

        String[] commands = {
            "ATTACKING",
            "Move Player", "Left Joystick",
            "Short Pass", "Tap Pass Button",
            "Through Ball", "Swipe Up on Pass Button",
            "Shoot", "Tap Shoot Button",
            "Finesse Shot", "Swipe Down on Shoot Button",
            "Sprint", "Hold Dash Button",
            "",
            "DEFENDING",
            "Move Player", "Left Joystick",
            "Press", "Hold Press Button",
            "Sliding Tackle", "Tap Tackle Button",
            "Switch Player", "Tap Switch Button"
        };

        for (int i = 0; i < commands.length; i++) {
            String text = commands[i];
            TextView label = new TextView(this);
            label.setText(text);
            
            if (text.equals("ATTACKING") || text.equals("DEFENDING")) {
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                label.setTypeface(BlurzTheme.FONT_BOLD);
                label.setTextColor(BlurzTheme.ACCENT_GOLD);
                if (i > 0) label.setPadding(0, dp16, 0, dp8);
                else label.setPadding(0, 0, 0, dp8);
            } else if (i % 2 != 0) {
                // Command Name
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                label.setTypeface(BlurzTheme.FONT_MEDIUM);
                label.setTextColor(BlurzTheme.TEXT_PRIMARY);
                label.setPadding(0, dp8, 0, 0);
            } else {
                // Command Action
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                label.setTypeface(BlurzTheme.FONT_REGULAR);
                label.setTextColor(BlurzTheme.TEXT_SECONDARY);
                label.setPadding(0, 0, 0, dp8);
            }
            card.addView(label);
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
}
