package org.libsdl.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Window;
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
        String[] labels  = { "▶  MATCH", "⚽  TRAINING", "⚙  SETTINGS", "ℹ  CREDITS", "✕  EXIT" };
        View.OnClickListener[] actions = {
            v -> startActivity(new Intent(this, TeamSelectActivity.class)),
            v -> showTrainingDialog(),
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

    private void showTrainingDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        int dp12 = BlurzTheme.dpToPx(this, 12);
        int dp16 = BlurzTheme.dpToPx(this, 16);
        int dp24 = BlurzTheme.dpToPx(this, 24);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(BlurzTheme.CARD_BG);
        cardBg.setCornerRadius(BlurzTheme.CORNER_RADIUS_CARD);
        cardBg.setStroke(2, Color.parseColor("#2A3A4E"));
        card.setBackground(cardBg);
        card.setPadding(dp24, dp24, dp24, dp24);

        TextView title = new TextView(this);
        title.setText("⚽  TRAINING GROUND");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(BlurzTheme.FONT_BOLD);
        title.setTextColor(BlurzTheme.ACCENT_CYAN);
        title.setLetterSpacing(0.08f);
        card.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Select a practice drill to hone your football skills:");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        subtitle.setTypeface(BlurzTheme.FONT_REGULAR);
        subtitle.setTextColor(BlurzTheme.TEXT_SECONDARY);
        subtitle.setPadding(0, dp12 / 2, 0, dp16);
        card.addView(subtitle);

        // Options: Free Play (1), Free Kick (2), Corner (3), Penalty (4)
        String[][] drills = {
            { "🟢  FREE PLAY", "Open field practice: passing, shooting, and dribbling", "1" },
            { "🎯  FREE KICKS", "Direct set pieces with 3-man defensive wall & goalkeeper", "2" },
            { "🚩  CORNER KICKS", "Cross deliveries and attacking headers from both corners", "3" },
            { "🥅  PENALTY SHOOTOUT", "1v1 penalty spot kicks against the goalkeeper", "4" }
        };

        for (String[] drill : drills) {
            final int drillType = Integer.parseInt(drill[2]);
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setClickable(true);
            item.setFocusable(true);
            item.setPadding(dp16, dp12, dp16, dp12);

            GradientDrawable itemBg = new GradientDrawable();
            itemBg.setColor(Color.parseColor("#121926"));
            itemBg.setCornerRadius(BlurzTheme.dpToPx(this, 12));
            itemBg.setStroke(1, Color.parseColor("#223044"));
            item.setBackground(itemBg);

            TextView itemTitle = new TextView(this);
            itemTitle.setText(drill[0]);
            itemTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            itemTitle.setTypeface(BlurzTheme.FONT_BOLD);
            itemTitle.setTextColor(BlurzTheme.TEXT_PRIMARY);
            item.addView(itemTitle);

            TextView itemDesc = new TextView(this);
            itemDesc.setText(drill[1]);
            itemDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            itemDesc.setTypeface(BlurzTheme.FONT_REGULAR);
            itemDesc.setTextColor(BlurzTheme.TEXT_SECONDARY);
            item.addView(itemDesc);

            item.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        itemBg.setColor(Color.parseColor("#1B283D"));
                        itemBg.setStroke(1, BlurzTheme.ACCENT_CYAN);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        itemBg.setColor(Color.parseColor("#121926"));
                        itemBg.setStroke(1, Color.parseColor("#223044"));
                        break;
                }
                return false;
            });

            item.setOnClickListener(v -> {
                dialog.dismiss();
                launchTraining(drillType);
            });

            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                BlurzTheme.dpToPx(this, 360), ViewGroup.LayoutParams.WRAP_CONTENT
            );
            itemParams.bottomMargin = dp12;
            card.addView(item, itemParams);
        }

        // Cancel / Back Button
        TextView cancelBtn = new TextView(this);
        cancelBtn.setText("✕  CANCEL");
        cancelBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        cancelBtn.setTypeface(BlurzTheme.FONT_BOLD);
        cancelBtn.setTextColor(BlurzTheme.TEXT_SECONDARY);
        cancelBtn.setGravity(Gravity.CENTER);
        cancelBtn.setPadding(0, dp12, 0, dp12 / 2);
        cancelBtn.setClickable(true);
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        card.addView(cancelBtn);

        dialog.setContentView(card);
        dialog.show();
    }

    private void launchTraining(int type) {
        Intent intent = new Intent(this, org.libsdl.app.SDLActivity.class);
        intent.putExtra("startQuickMatch", true);
        intent.putExtra("isTrainingMode", true);
        intent.putExtra("trainingType", String.valueOf(type));
        intent.putExtra("homeTeamId", "1");
        intent.putExtra("awayTeamId", "2");
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
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
