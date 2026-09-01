package org.libsdl.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.libsdl.app.R;

/**
 * Main Menu for Blurz Football.
 */
public class MainMenuActivity extends Activity {

    public static MainMenuActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        BlurzTheme.applyImmersive(this);

        int dp12 = BlurzTheme.dpToPx(this, 12);
        int dp16 = BlurzTheme.dpToPx(this, 16);
        int dp24 = BlurzTheme.dpToPx(this, 24);
        int dp32 = BlurzTheme.dpToPx(this, 32);
        int dp48 = BlurzTheme.dpToPx(this, 48);
        int dp64 = BlurzTheme.dpToPx(this, 64);

        // ── Root: FrameLayout ──
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#0A0E1A")); // Solid navy bg to match splash

        // ── Gradient overlay for depth ──
        View gradientOverlay = new View(this);
        GradientDrawable overlay = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{ BlurzTheme.BG_DARK, Color.TRANSPARENT }
        );
        gradientOverlay.setBackground(overlay);
        root.addView(gradientOverlay, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // ── ScrollView for all content to prevent cutting off ──
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);

        // ── Content Layout ──
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        // Reduced top padding so buttons fit
        content.setPadding(dp64, dp24, dp32, dp24);

        // ── Logo and Title row ──
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(0, 0, 0, dp24); // Reduced Space below title

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.blurz_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(
            BlurzTheme.dpToPx(this, 80), BlurzTheme.dpToPx(this, 80)
        );
        logoParams.rightMargin = dp16;
        titleRow.addView(logo, logoParams);

        LinearLayout titleTextCol = new LinearLayout(this);
        titleTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView titleLine1 = new TextView(this);
        titleLine1.setText("BLURZ");
        titleLine1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 46);
        titleLine1.setTypeface(BlurzTheme.FONT_BOLD);
        titleLine1.setTextColor(BlurzTheme.TEXT_PRIMARY);
        titleLine1.setLetterSpacing(0.15f);
        titleLine1.setShadowLayer(20, 0, 0, BlurzTheme.ACCENT_CYAN);
        titleTextCol.addView(titleLine1);

        TextView titleLine2 = new TextView(this);
        titleLine2.setText("FOOTBALL");
        titleLine2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        titleLine2.setTypeface(BlurzTheme.FONT_MEDIUM);
        titleLine2.setTextColor(BlurzTheme.ACCENT_CYAN);
        titleLine2.setLetterSpacing(0.3f);
        titleTextCol.addView(titleLine2);

        titleRow.addView(titleTextCol);
        content.addView(titleRow);

        // ── Menu Buttons ──
        String[] labels  = { "▶  MATCH", "⚙  SETTINGS", "ℹ  CREDITS", "✕  EXIT" };
        View.OnClickListener[] actions = {
            v -> startActivity(new Intent(this, TeamSelectActivity.class)),
            v -> startActivity(new Intent(this, SettingsActivity.class)),
            v -> startActivity(new Intent(this, CreditsActivity.class)),
            v -> finishAffinity()
        };

        for (int i = 0; i < labels.length; i++) {
            TextView btn = createMenuButton(labels[i], i == 0);
            btn.setOnClickListener(actions[i]);

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
                BlurzTheme.dpToPx(this, 260),
                BlurzTheme.dpToPx(this, 48)
            );
            btnParams.bottomMargin = BlurzTheme.dpToPx(this, 8);
            content.addView(btn, btnParams);

            // Staggered slide-in animation
            AnimationSet anim = new AnimationSet(true);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            TranslateAnimation slide = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, -0.5f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f
            );
            AlphaAnimation fade = new AlphaAnimation(0f, 1f);
            anim.addAnimation(slide);
            anim.addAnimation(fade);
            anim.setDuration(400);
            anim.setStartOffset(200 + i * 100L);
            anim.setFillBefore(true);
            btn.startAnimation(anim);
        }

        scrollView.addView(content);
        
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        root.addView(scrollView, scrollParams);

        // ── Version text bottom-right ──
        TextView versionText = new TextView(this);
        versionText.setText("v1.0  •  Blurz Football");
        versionText.setTextColor(BlurzTheme.TEXT_DIM);
        versionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        versionText.setTypeface(BlurzTheme.FONT_REGULAR);
        versionText.setPadding(0, 0, dp24, dp16);

        FrameLayout.LayoutParams versionParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM | Gravity.END
        );
        root.addView(versionText, versionParams);

        setContentView(root);
    }

    private TextView createMenuButton(String text, boolean isPrimary) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        btn.setTypeface(BlurzTheme.FONT_BOLD);
        btn.setGravity(Gravity.CENTER_VERTICAL);
        btn.setPadding(BlurzTheme.dpToPx(this, 24), 0, BlurzTheme.dpToPx(this, 24), 0);
        btn.setClickable(true);
        btn.setFocusable(true);

        if (isPrimary) {
            btn.setTextColor(Color.WHITE);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(BlurzTheme.ACCENT_CYAN);
            bg.setCornerRadius(BlurzTheme.CORNER_RADIUS_BTN);
            btn.setBackground(bg);
            btn.setShadowLayer(12, 0, 4, Color.parseColor("#6600E5FF"));
        } else {
            btn.setTextColor(BlurzTheme.TEXT_PRIMARY);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(BlurzTheme.CARD_BG);
            bg.setCornerRadius(BlurzTheme.CORNER_RADIUS_BTN);
            bg.setStroke(1, Color.parseColor("#2A3A4E"));
            btn.setBackground(bg);
        }

        return btn;
    }

    @Override
    protected void onResume() {
        super.onResume();
        BlurzTheme.applyImmersive(this);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (instance == this) instance = null;
    }
}
