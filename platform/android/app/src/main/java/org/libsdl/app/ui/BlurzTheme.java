package org.libsdl.app.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Shared design system for Blurz Football.
 * FC Mobile-inspired dark theme with neon accents.
 */
public final class BlurzTheme {

    // ── Background Gradient ──
    public static final int BG_DARK    = Color.parseColor("#0A0E1A");
    public static final int BG_MID     = Color.parseColor("#111B2E");

    // ── Card / Surface ──
    public static final int CARD_BG    = Color.parseColor("#1A2332");
    public static final int CARD_BG_LIGHT = Color.parseColor("#243044");

    // ── Accent Colors ──
    public static final int ACCENT_CYAN   = Color.parseColor("#00E5FF");
    public static final int ACCENT_GREEN  = Color.parseColor("#00E676");
    public static final int ACCENT_RED    = Color.parseColor("#FF1744");
    public static final int ACCENT_GOLD   = Color.parseColor("#FFD740");

    // ── Text Colors ──
    public static final int TEXT_PRIMARY   = Color.parseColor("#F0F4FF");
    public static final int TEXT_SECONDARY = Color.parseColor("#8899AA");
    public static final int TEXT_DIM       = Color.parseColor("#556677");

    // ── Dimensions (in dp-like values for programmatic use) ──
    public static final int CORNER_RADIUS_CARD = 24;
    public static final int CORNER_RADIUS_BTN  = 50;
    public static final int PADDING_SCREEN     = 32;
    public static final int PADDING_CARD       = 20;

    // ── Typography ──
    public static Typeface FONT_BOLD;
    public static Typeface FONT_MEDIUM;
    public static Typeface FONT_REGULAR;

    static {
        FONT_BOLD    = Typeface.create("sans-serif", Typeface.BOLD);
        FONT_MEDIUM  = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        FONT_REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
    }

    private BlurzTheme() {} // No instantiation

    /** Apply full-screen immersive mode to an Activity. */
    public static void applyImmersive(Activity activity) {
        activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
        activity.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        if (Build.VERSION.SDK_INT >= 19) {
            activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }

    /** Create the standard dark gradient background. */
    public static GradientDrawable createBackground() {
        return new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{ BG_DARK, BG_MID }
        );
    }

    /** Create a glassmorphic card background. */
    public static GradientDrawable createCardBackground() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(CARD_BG);
        d.setCornerRadius(CORNER_RADIUS_CARD);
        d.setStroke(1, Color.parseColor("#2A3A4E"));
        return d;
    }

    /** Create an accent button background. */
    public static GradientDrawable createButtonBackground(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(CORNER_RADIUS_BTN);
        return d;
    }

    /** Create a subtle outlined button background. */
    public static GradientDrawable createOutlinedButtonBackground() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.TRANSPARENT);
        d.setCornerRadius(CORNER_RADIUS_BTN);
        d.setStroke(2, ACCENT_CYAN);
        return d;
    }

    /** Convert dp to pixels. */
    public static int dpToPx(Activity activity, int dp) {
        float density = activity.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
