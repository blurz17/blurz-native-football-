package org.libsdl.app.ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Credits screen for Blurz Football.
 * Replaces the old C++ CreditsPage.
 * Auto-scrolling credits text.
 */
public class CreditsActivity extends Activity {

    private ScrollView scrollView;
    private Handler scrollHandler = new Handler();
    private boolean isScrolling = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BlurzTheme.applyImmersive(this);

        int dp8  = BlurzTheme.dpToPx(this, 8);
        int dp16 = BlurzTheme.dpToPx(this, 16);
        int dp24 = BlurzTheme.dpToPx(this, 24);
        int dp32 = BlurzTheme.dpToPx(this, 32);
        int dp48 = BlurzTheme.dpToPx(this, 48);
        int dp64 = BlurzTheme.dpToPx(this, 64);

        FrameLayout root = new FrameLayout(this);
        root.setBackground(BlurzTheme.createBackground());

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
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
        title.setText("CREDITS");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(BlurzTheme.FONT_BOLD);
        title.setTextColor(BlurzTheme.ACCENT_CYAN);
        title.setLetterSpacing(0.1f);
        topBar.addView(title);
        main.addView(topBar);

        // ── Scrolling Credits ──
        scrollView = new ScrollView(this);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout creditsContainer = new LinearLayout(this);
        creditsContainer.setOrientation(LinearLayout.VERTICAL);
        creditsContainer.setPadding(dp48, dp48, dp48, dp64 * 3);

        // Build credits content
        addCreditSection(creditsContainer, "BLURZ FOOTBALL", "");
        addCreditSection(creditsContainer, "", "A modern football experience");
        addCreditSection(creditsContainer, "", "");
        addCreditSection(creditsContainer, "ORIGINAL ENGINE", "");
        addCreditSection(creditsContainer, "Gameplay Football", "by Bastiaan Konings Schuiling");
        addCreditSection(creditsContainer, "", "");
        addCreditSection(creditsContainer, "ANDROID PORT & UI", "");
        addCreditSection(creditsContainer, "Native Android UI", "Blurz Team");
        addCreditSection(creditsContainer, "SDL2 Integration", "SDL Development Team");
        addCreditSection(creditsContainer, "", "");
        addCreditSection(creditsContainer, "TEAM EMBLEMS", "");
        addCreditSection(creditsContainer, "", "TureckiRumun");
        addCreditSection(creditsContainer, "", "broxopios");
        addCreditSection(creditsContainer, "", "balder");
        addCreditSection(creditsContainer, "", "NLP");
        addCreditSection(creditsContainer, "", "");
        addCreditSection(creditsContainer, "LIBRARIES", "");
        addCreditSection(creditsContainer, "", "SDL2 2.28.5");
        addCreditSection(creditsContainer, "", "Boost C++ Libraries");
        addCreditSection(creditsContainer, "", "SQLite");
        addCreditSection(creditsContainer, "", "SDL2_ttf / SDL2_image");
        addCreditSection(creditsContainer, "", "");
        addCreditSection(creditsContainer, "SPECIAL THANKS", "");
        addCreditSection(creditsContainer, "", "The open-source football game community");
        addCreditSection(creditsContainer, "", "All contributors and testers");
        addCreditSection(creditsContainer, "", "");
        addCreditSection(creditsContainer, "", "");
        addCreditSection(creditsContainer, "Thank you for playing!", "");

        scrollView.addView(creditsContainer);

        // Tap to stop/resume scrolling
        scrollView.setOnTouchListener((v, event) -> {
            isScrolling = !isScrolling;
            if (isScrolling) startAutoScroll();
            return false;
        });

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        );
        scrollParams.topMargin = dp16;
        main.addView(scrollView, scrollParams);

        root.addView(main, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);

        // Start auto-scrolling after a brief delay
        scrollHandler.postDelayed(this::startAutoScroll, 1000);
    }

    private void addCreditSection(LinearLayout container, String heading, String body) {
        int dp4 = BlurzTheme.dpToPx(this, 4);

        if (!heading.isEmpty()) {
            TextView h = new TextView(this);
            h.setText(heading);
            h.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            h.setTypeface(BlurzTheme.FONT_BOLD);
            h.setTextColor(BlurzTheme.ACCENT_CYAN);
            h.setGravity(Gravity.CENTER);
            h.setPadding(0, dp4, 0, dp4);
            container.addView(h);
        }

        if (!body.isEmpty()) {
            TextView b = new TextView(this);
            b.setText(body);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            b.setTypeface(BlurzTheme.FONT_REGULAR);
            b.setTextColor(BlurzTheme.TEXT_SECONDARY);
            b.setGravity(Gravity.CENTER);
            b.setPadding(0, 0, 0, dp4);
            container.addView(b);
        }

        if (heading.isEmpty() && body.isEmpty()) {
            // Spacer
            TextView spacer = new TextView(this);
            spacer.setText("");
            spacer.setPadding(0, BlurzTheme.dpToPx(this, 16), 0, 0);
            container.addView(spacer);
        }
    }

    private void startAutoScroll() {
        scrollHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isScrolling && scrollView != null) {
                    scrollView.smoothScrollBy(0, 2);
                    scrollHandler.postDelayed(this, 50);
                }
            }
        }, 50);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isScrolling = false;
        scrollHandler.removeCallbacksAndMessages(null);
    }
}
