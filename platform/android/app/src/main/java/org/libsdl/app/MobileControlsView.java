package org.libsdl.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern In-Game Match UI & Touch Overlay.
 * Implements the "Apex Pitch HUD" design specification:
 * - Floating glassmorphic Scoreboard Capsule with live match telemetry
 * - Futuristic Virtual Joystick with 8-direction cardinal ticks and glowing nub
 * - Ergonomic 4-node Action Button Diamond with contextual Attack/Defense states
 * - Sleek circular Pause Button and Glassmorphic Pause Modal
 * - Full-Time Match Result Screen with possession bars and shot statistics
 */
public class MobileControlsView extends View {

    // Paints
    private Paint bgPaint;
    private Paint handlePaint;
    private Paint btnBgPaint;
    private Paint btnBorderPaint;
    private Paint textPaint;
    private Paint subTextPaint;
    private Paint cardPaint;
    private Paint borderPaint;
    private Paint accentPaint;

    // Safe Area Insets
    private int safeInsetLeft = 0;
    private int safeInsetRight = 0;
    private int safeInsetTop = 0;
    private int safeInsetBottom = 0;

    // State
    private boolean isMatchMode = false;
    private boolean isPaused = false;
    private boolean isGameOver = false;
    public boolean isAttacking = true;

    // Pause subviews & stats
    public enum PauseSubView {
        OVERVIEW,
        SETTINGS,
        CONFIRM_QUIT
    }
    private PauseSubView pauseSubView = PauseSubView.OVERVIEW;

    public static class LiveMatchStats {
        public int score1 = 0, score2 = 0;
        public int poss1 = 55, poss2 = 45;
        public int shots1 = 0, shots2 = 0;
        public int sot1 = 0, sot2 = 0;
        public int fouls1 = 0, fouls2 = 0;
        public int offsides1 = 0, offsides2 = 0;
        public int corners1 = 0, corners2 = 0;
        public int freeKicks1 = 0, freeKicks2 = 0;
        public int passes1 = 0, passes2 = 0;
        public int passesComp1 = 0, passesComp2 = 0;
        public int crosses1 = 0, crosses2 = 0;
        public int interceptions1 = 0, interceptions2 = 0;
        public int tackles1 = 0, tackles2 = 0;
        public int saves1 = 0, saves2 = 0;
    }
    private final LiveMatchStats liveStats = new LiveMatchStats();

    // In-game Settings Sliders State
    private float settingCamHeight = 0.40f;
    private float settingCamZoom = 0.75f;
    private float settingCamAngle = 0.70f;
    private float settingAudioVolume = 0.80f;
    private boolean settingRadarEnabled = true;
    private int draggingSlider = -1; // 0=Height, 1=Zoom, 2=Angle, 3=Audio

    // Featured player & substitution suggestions state
    private boolean subSwapped = false;
    private String featuredPlayerName = "LEWANDOWSKI";
    private String featuredPlayerPos = "CF";
    private String featuredPlayerRating = "9.5";

    // Live Match Telemetry
    private String team1Name = "HOME";
    private String team2Name = "AWAY";
    private int score1 = 0;
    private int score2 = 0;
    private String matchTime = "00:00";

    // Full-Time Game Over Stats
    private int goT1Score, goT2Score;
    private int goT1Poss, goT2Poss;
    private int goT1Shots, goT2Shots;
    private int gameOverButtonPointerId = -1;
    private boolean gameOverButtonPressed = false;

    // Pause Menu
    private int pressedMenuIndex = -1;

    // Virtual Joystick
    private float joyBaseX, joyBaseY;
    private float joyHandleX, joyHandleY;
    private float joyRadius = 180f;
    private float joyHandleRadius = 70f;
    private int joyPointerId = -1;
    private boolean isJoyFloating = false;
    private float defaultJoyBaseX, defaultJoyBaseY;
    private long lastJoyLogTime = 0;

    // Action Buttons
    private static class ActionButton {
        float cx, cy, radius;
        String labelOffense;
        String labelDefense;
        int keycodeOffense;
        int keycodeDefense;
        int pointerId = -1;
        boolean isPressed = false;

        ActionButton(float cx, float cy, float radius, String labelOffense, String labelDefense,
                     int keycodeOffense, int keycodeDefense) {
            this.cx = cx;
            this.cy = cy;
            this.radius = radius;
            this.labelOffense = labelOffense;
            this.labelDefense = labelDefense;
            this.keycodeOffense = keycodeOffense;
            this.keycodeDefense = keycodeDefense;
        }
    }

    private final List<ActionButton> buttons = new ArrayList<>();
    private ActionButton pauseButton;

    public MobileControlsView(Context context) {
        super(context);
        init();
    }

    public MobileControlsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);

        handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handlePaint.setStyle(Paint.Style.FILL);

        btnBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnBgPaint.setStyle(Paint.Style.FILL);

        btnBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnBorderPaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setTypeface(Typeface.DEFAULT);

        cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);

        accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        accentPaint.setStyle(Paint.Style.FILL);

        // Load saved camera and audio settings
        try {
            SharedPreferences prefs = getContext().getSharedPreferences("blurz_settings", Context.MODE_PRIVATE);
            int h = prefs.getInt("camera_height", 50);
            int z = prefs.getInt("camera_zoom", 50);
            int a = prefs.getInt("camera_angle", 50);
            int vol = prefs.getInt("audio_volume", 80);
            settingCamHeight = 0.1f + (h / 100f) * 1.4f;
            settingCamZoom = 0.2f + (z / 100f) * 1.3f;
            settingCamAngle = a / 100f;
            settingAudioVolume = vol / 100f;
            settingRadarEnabled = prefs.getBoolean("radar_enabled", true);
            applyCameraAndAudioSettings();
        } catch (Throwable ignored) {}
    }

    public void applyCameraAndAudioSettings() {
        try {
            SDLActivity.setCameraSettings(settingCamZoom, settingCamHeight, 0.5f, settingCamAngle);
            SDLActivity.setAudioVolume(settingAudioVolume);
        } catch (Throwable ignored) {}
    }

    private void saveSettingsToPrefs() {
        try {
            SharedPreferences prefs = getContext().getSharedPreferences("blurz_settings", Context.MODE_PRIVATE);
            int hProgress = Math.round((settingCamHeight - 0.1f) / 1.4f * 100f);
            int zProgress = Math.round((settingCamZoom - 0.2f) / 1.3f * 100f);
            int aProgress = Math.round(settingCamAngle * 100f);
            int volProgress = Math.round(settingAudioVolume * 100f);
            prefs.edit()
                    .putInt("camera_height", hProgress)
                    .putInt("camera_zoom", zProgress)
                    .putInt("camera_angle", aProgress)
                    .putInt("audio_volume", volProgress)
                    .putBoolean("radar_enabled", settingRadarEnabled)
                    .apply();
        } catch (Throwable ignored) {}
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            DisplayCutout cutout = insets.getDisplayCutout();
            if (cutout != null) {
                safeInsetLeft = cutout.getSafeInsetLeft();
                safeInsetRight = cutout.getSafeInsetRight();
                safeInsetTop = cutout.getSafeInsetTop();
                safeInsetBottom = cutout.getSafeInsetBottom();
                recalculateLayout(getWidth(), getHeight());
            }
        }
        return super.onApplyWindowInsets(insets);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        recalculateLayout(w, h);
    }

    private void recalculateLayout(int w, int h) {
        if (w <= 0 || h <= 0) return;

        // Joystick dimensions & base position (Bottom-Left)
        joyRadius = h * 0.22f;
        joyHandleRadius = joyRadius * 0.38f;
        defaultJoyBaseX = Math.max(w * 0.14f, safeInsetLeft + joyRadius + 24f);
        defaultJoyBaseY = h * 0.74f;

        if (joyPointerId == -1) {
            joyBaseX = defaultJoyBaseX;
            joyBaseY = defaultJoyBaseY;
            joyHandleX = joyBaseX;
            joyHandleY = joyBaseY;
        }

        // Action Buttons (Bottom-Right Ergonomic Diamond)
        buttons.clear();

        float baseX = w - Math.max(w * 0.12f, safeInsetRight + 44f);
        float baseY = h * 0.78f;
        float dashRad = h * 0.105f;
        float normalRad = h * 0.082f;
        float spacingX = w * 0.125f;
        float spacingY = h * 0.21f;

        // 1. PASS / SWITCH (South: Left of Dash)
        buttons.add(new ActionButton(baseX - spacingX, baseY + h * 0.02f, normalRad,
                "Pass", "Switch", 0, 4));

        // 2. DASH / SPRINT (East: Bottom-Right)
        buttons.add(new ActionButton(baseX, baseY, dashRad,
                "Dash", "Dash", 5, 5));

        // 3. THROUGH / PRESS (West: North-West of Dash)
        buttons.add(new ActionButton(baseX - spacingX * 0.92f, baseY - spacingY, normalRad,
                "Through", "Press", 3, 0));

        // 4. SHOOT / CLEAR (North: Above Dash)
        buttons.add(new ActionButton(baseX - w * 0.015f, baseY - spacingY - h * 0.02f, normalRad,
                "Shoot", "Clear", 2, 1));

        // Top-Right Pause Button
        float pauseR = normalRad * 0.50f;
        float pauseX = w - Math.max(w * 0.045f, safeInsetRight + 28f);
        float pauseY = Math.max(h * 0.07f, safeInsetTop + 24f);
        pauseButton = new ActionButton(pauseX, pauseY, pauseR, "||", "||", 7, 7);
    }

    // ── External State Updates ──

    public void updateTelemetry(String t1Name, String t2Name, int t1Score, int t2Score, String timeStr) {
        if (t1Name != null && !t1Name.isEmpty()) this.team1Name = t1Name;
        if (t2Name != null && !t2Name.isEmpty()) this.team2Name = t2Name;
        this.score1 = t1Score;
        this.score2 = t2Score;
        if (timeStr != null && !timeStr.isEmpty()) this.matchTime = timeStr;
        postInvalidate();
    }

    public void setAttacking(boolean attacking) {
        if (this.isAttacking != attacking) {
            this.isAttacking = attacking;
            postInvalidate();
        }
    }

    public void setMatchMode(boolean matchMode) {
        this.isMatchMode = matchMode;
        if (matchMode) {
            this.isPaused = false;
            this.isGameOver = false;
            this.pauseSubView = PauseSubView.OVERVIEW;
            try {
                SDLActivity.setMatchPaused(false);
            } catch (Throwable ignored) {}
            applyCameraAndAudioSettings();
        } else {
            this.isPaused = false;
            try {
                SDLActivity.setMatchPaused(false);
            } catch (Throwable ignored) {}
        }
        postInvalidate();
    }

    public void showGameOver(int t1s, int t2s, int t1p, int t2p, int t1sh, int t2sh) {
        this.isGameOver = true;
        this.isPaused = false;
        this.goT1Score = t1s;
        this.goT2Score = t2s;
        this.goT1Poss = t1p;
        this.goT2Poss = t2p;
        this.goT1Shots = t1sh;
        this.goT2Shots = t2sh;
        postInvalidate();
    }

    // ── Rendering ──

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int w = getWidth();
        final int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // 1. GAME OVER / FULL-TIME SCREEN
        if (isGameOver) {
            drawGameOverScreen(canvas, w, h);
            return;
        }

        // 2. PAUSE MENU OVERLAY
        if (isPaused) {
            drawPauseMenu(canvas, w, h);
            return;
        }

        // 3. MATCH MODE HUD
        if (isMatchMode) {
            drawScoreboard(canvas, w, h);
            drawVirtualJoystick(canvas);
            drawActionButtons(canvas);
            drawPauseButton(canvas);
        }
    }

    /**
     * Draw modern floating scoreboard pill capsule at top-left.
     */
    private void drawScoreboard(Canvas canvas, int w, int h) {
        float sbW = Math.min(w * 0.35f, 430f);
        float sbH = Math.max(h * 0.088f, 46f);
        float sbX = Math.max(w * 0.035f, safeInsetLeft + 20f);
        float sbY = Math.max(h * 0.04f, safeInsetTop + 14f);

        RectF pillRect = new RectF(sbX, sbY, sbX + sbW, sbY + sbH);

        // Glassmorphic Capsule Background
        cardPaint.setColor(Color.argb(205, 10, 16, 28));
        canvas.drawRoundRect(pillRect, sbH * 0.5f, sbH * 0.5f, cardPaint);

        // Neon Specular Border
        borderPaint.setColor(Color.argb(130, 0, 240, 255));
        borderPaint.setStrokeWidth(2.5f);
        canvas.drawRoundRect(pillRect, sbH * 0.5f, sbH * 0.5f, borderPaint);

        // Column widths
        float colTeamW = sbW * 0.24f;
        float colScoreW = sbW * 0.28f;
        float colClockW = sbW * 0.24f;

        float team1CenterX = sbX + colTeamW * 0.55f;
        float scoreCenterX = sbX + colTeamW + colScoreW * 0.5f;
        float team2CenterX = sbX + colTeamW + colScoreW + colTeamW * 0.45f;
        float clockCenterX = sbX + sbW - colClockW * 0.5f;

        float textY = sbY + sbH * 0.62f;

        // Team 1 Name
        textPaint.setTextSize(sbH * 0.38f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(team1Name, team1CenterX, textY, textPaint);

        // Possession indicator dot (under attacking team)
        if (isAttacking) {
            accentPaint.setColor(Color.parseColor("#00F0FF"));
            canvas.drawCircle(team1CenterX, sbY + sbH * 0.84f, sbH * 0.07f, accentPaint);
        }

        // Score Badge
        RectF scoreBadge = new RectF(scoreCenterX - colScoreW * 0.42f, sbY + sbH * 0.16f,
                scoreCenterX + colScoreW * 0.42f, sbY + sbH * 0.84f);
        cardPaint.setColor(Color.argb(160, 4, 28, 52));
        canvas.drawRoundRect(scoreBadge, sbH * 0.34f, sbH * 0.34f, cardPaint);

        borderPaint.setColor(Color.argb(160, 0, 240, 255));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(scoreBadge, sbH * 0.34f, sbH * 0.34f, borderPaint);

        textPaint.setColor(Color.parseColor("#00F0FF"));
        textPaint.setTextSize(sbH * 0.44f);
        canvas.drawText(score1 + " - " + score2, scoreCenterX, textY, textPaint);

        // Team 2 Name
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(sbH * 0.38f);
        canvas.drawText(team2Name, team2CenterX, textY, textPaint);

        if (!isAttacking) {
            accentPaint.setColor(Color.parseColor("#0080FF"));
            canvas.drawCircle(team2CenterX, sbY + sbH * 0.84f, sbH * 0.07f, accentPaint);
        }

        // Clock Divider
        borderPaint.setColor(Color.argb(90, 80, 120, 160));
        borderPaint.setStrokeWidth(2f);
        float divX = sbX + sbW - colClockW;
        canvas.drawLine(divX, sbY + sbH * 0.20f, divX, sbY + sbH * 0.80f, borderPaint);

        // Match Clock
        textPaint.setColor(Color.parseColor("#E0F7FA"));
        textPaint.setTextSize(sbH * 0.36f);
        canvas.drawText(matchTime, clockCenterX, textY, textPaint);
    }

    /**
     * Draw Virtual Analog Directional Joystick.
     */
    private void drawVirtualJoystick(Canvas canvas) {
        // Outer Frosted Glass Base
        bgPaint.setShader(new RadialGradient(joyBaseX, joyBaseY, joyRadius,
                Color.argb(90, 30, 42, 64), Color.argb(20, 10, 15, 25), Shader.TileMode.CLAMP));
        canvas.drawCircle(joyBaseX, joyBaseY, joyRadius, bgPaint);

        // Outer Glass Border
        boolean isActive = (joyPointerId != -1);
        btnBorderPaint.setColor(isActive ? Color.parseColor("#00F0FF") : Color.argb(90, 160, 200, 240));
        btnBorderPaint.setStrokeWidth(isActive ? 5f : 2.5f);
        canvas.drawCircle(joyBaseX, joyBaseY, joyRadius, btnBorderPaint);

        // 8 Directional Cardinal Ticks
        Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setColor(Color.argb(isActive ? 160 : 80, 0, 240, 255));
        tickPaint.setStrokeWidth(isActive ? 3.5f : 2.5f);
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            float innerR = joyRadius * 0.84f;
            float outerR = joyRadius * 0.96f;
            float x1 = joyBaseX + (float) (Math.cos(angle) * innerR);
            float y1 = joyBaseY + (float) (Math.sin(angle) * innerR);
            float x2 = joyBaseX + (float) (Math.cos(angle) * outerR);
            float y2 = joyBaseY + (float) (Math.sin(angle) * outerR);
            canvas.drawLine(x1, y1, x2, y2, tickPaint);
        }

        // Active Vector Guide Line
        if (isActive) {
            Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            linePaint.setColor(Color.argb(160, 0, 240, 255));
            linePaint.setStrokeWidth(5f);
            canvas.drawLine(joyBaseX, joyBaseY, joyHandleX, joyHandleY, linePaint);
        }

        // Center Thumb Nub
        float hScale = isActive ? 1.10f : 1.0f;
        handlePaint.setShader(new RadialGradient(joyHandleX, joyHandleY, joyHandleRadius * hScale,
                isActive ? Color.parseColor("#00F0FF") : Color.argb(240, 220, 235, 255),
                Color.argb(190, 20, 45, 80), Shader.TileMode.CLAMP));
        canvas.drawCircle(joyHandleX, joyHandleY, joyHandleRadius * hScale, handlePaint);

        // Nub Specular Border
        btnBorderPaint.setColor(isActive ? Color.WHITE : Color.argb(180, 140, 190, 240));
        btnBorderPaint.setStrokeWidth(3.5f);
        canvas.drawCircle(joyHandleX, joyHandleY, joyHandleRadius * hScale, btnBorderPaint);

        // Nub Core Gem
        Paint gemPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gemPaint.setColor(Color.parseColor("#00F0FF"));
        canvas.drawCircle(joyHandleX, joyHandleY, joyHandleRadius * 0.22f, gemPaint);
    }

    /**
     * Draw Action Buttons Cluster with contextual Attack/Defense text.
     */
    private void drawActionButtons(Canvas canvas) {
        for (ActionButton btn : buttons) {
            float scale = btn.isPressed ? 0.91f : 1.0f;
            float r = btn.radius * scale;

            // Frosted Radial Fill
            int baseCol = btn.isPressed ? Color.argb(200, 0, 160, 220) : Color.argb(115, 14, 24, 42);
            btnBgPaint.setShader(new RadialGradient(btn.cx, btn.cy, r,
                    baseCol, Color.argb(70, 8, 14, 26), Shader.TileMode.CLAMP));
            canvas.drawCircle(btn.cx, btn.cy, r, btnBgPaint);

            // Specular Cyan Border
            int borderCol = btn.isPressed ? Color.parseColor("#00F0FF") : Color.argb(150, 70, 140, 210);
            btnBorderPaint.setColor(borderCol);
            btnBorderPaint.setStrokeWidth(btn.isPressed ? 5.5f : 3.0f);
            canvas.drawCircle(btn.cx, btn.cy, r, btnBorderPaint);

            // Contextual Labels
            String primaryLabel = isAttacking ? btn.labelOffense : btn.labelDefense;
            String secondaryLabel = isAttacking ? btn.labelDefense : btn.labelOffense;

            // Primary Command (Center)
            textPaint.setTextSize(r * 0.35f);
            textPaint.setColor(btn.isPressed ? Color.parseColor("#00F0FF") : Color.WHITE);
            canvas.drawText(primaryLabel.toUpperCase(), btn.cx, btn.cy - r * 0.04f, textPaint);

            // Secondary Context Subtitle (Top/Bottom)
            if (!secondaryLabel.equalsIgnoreCase(primaryLabel)) {
                subTextPaint.setTextSize(r * 0.20f);
                subTextPaint.setColor(Color.argb(150, 0, 240, 255));
                canvas.drawText(secondaryLabel.toUpperCase(), btn.cx, btn.cy + r * 0.34f, subTextPaint);
            }
        }
    }

    /**
     * Draw sleek top-right circular pause button.
     */
    private void drawPauseButton(Canvas canvas) {
        if (pauseButton == null) return;

        float r = pauseButton.radius * (pauseButton.isPressed ? 0.90f : 1.0f);
        float cx = pauseButton.cx;
        float cy = pauseButton.cy;

        // Circular Frosted Background
        cardPaint.setColor(Color.argb(pauseButton.isPressed ? 220 : 150, 16, 24, 40));
        canvas.drawCircle(cx, cy, r, cardPaint);

        borderPaint.setColor(pauseButton.isPressed ? Color.parseColor("#00F0FF") : Color.argb(130, 0, 240, 255));
        borderPaint.setStrokeWidth(pauseButton.isPressed ? 3.5f : 2.0f);
        canvas.drawCircle(cx, cy, r, borderPaint);

        // Dual Vertical Pause Bars
        Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(pauseButton.isPressed ? Color.parseColor("#00F0FF") : Color.WHITE);
        float barW = r * 0.20f;
        float barH = r * 0.70f;
        float barSpacing = r * 0.16f;

        canvas.drawRoundRect(new RectF(cx - barSpacing - barW, cy - barH / 2f,
                cx - barSpacing, cy + barH / 2f), 3f, 3f, barPaint);
        canvas.drawRoundRect(new RectF(cx + barSpacing, cy - barH / 2f,
                cx + barSpacing + barW, cy + barH / 2f), 3f, 3f, barPaint);
    }

    /**
    /**
     * Draw modern PES Mobile Style Pause Menu Overlay.
     */
    private void drawPauseMenu(Canvas canvas, int w, int h) {
        if (pauseSubView == PauseSubView.SETTINGS) {
            drawSettingsOverlay(canvas, w, h);
        } else if (pauseSubView == PauseSubView.CONFIRM_QUIT) {
            drawConfirmQuitOverlay(canvas, w, h);
        } else {
            drawPesPauseOverview(canvas, w, h);
        }
    }

    /**
     * Authentic PES Mobile 3-Column Pause Screen:
     * - Left Column: Vertical action buttons (Yellow Resume, Game Plan, Records, Settings, Replay, Top Menu)
     * - Center Column: Scoreboard Header + 10-Row Match Stats Comparison Table
     * - Right Column: Featured Player Card + Substitution Suggestions
     */
    private void drawPesPauseOverview(Canvas canvas, int w, int h) {
        // Deep Dark Navy Dimmer over pitch
        cardPaint.setColor(Color.argb(238, 10, 12, 24));
        canvas.drawRect(0, 0, w, h, cardPaint);

        float padL = Math.max(w * 0.025f, safeInsetLeft + 16f);
        float padR = Math.max(w * 0.025f, safeInsetRight + 16f);
        float padT = Math.max(h * 0.035f, safeInsetTop + 10f);
        float padB = Math.max(h * 0.035f, safeInsetBottom + 10f);
        float areaW = w - padL - padR;
        float areaH = h - padT - padB;

        float gap = areaW * 0.014f;
        float col1W = areaW * 0.175f;
        float col2W = areaW * 0.495f;
        float col3W = areaW - col1W - col2W - (gap * 2f);

        float col1X = padL;
        float col2X = col1X + col1W + gap;
        float col3X = col2X + col2W + gap;

        // ══════════════════════════════════════════════════
        // COLUMN 1: VERTICAL NAVIGATION TABS (PES STYLE)
        // ══════════════════════════════════════════════════
        String[] menuLabels = {
                "RESUME",
                "GAME PLAN",
                "MATCH RECORDS",
                "SETTINGS",
                "HIGHLIGHTS",
                "TOP MENU"
        };
        float tabH = (areaH - 5f * gap) / 6f;

        for (int i = 0; i < 6; i++) {
            float ty = padT + i * (tabH + gap);
            RectF tabRect = new RectF(col1X, ty, col1X + col1W, ty + tabH);
            boolean isPressed = (pressedMenuIndex == i);

            if (i == 0) {
                // TOP ACTION: Vibrant Yellow-Amber Accent
                cardPaint.setColor(isPressed ? Color.parseColor("#E09400") : Color.parseColor("#FFA800"));
                canvas.drawRoundRect(tabRect, 12f, 12f, cardPaint);

                borderPaint.setColor(Color.parseColor("#FFD54F"));
                borderPaint.setStrokeWidth(2f);
                canvas.drawRoundRect(tabRect, 12f, 12f, borderPaint);

                // Football icon
                float iconRadius = tabH * 0.28f;
                float iconCx = col1X + col1W * 0.20f;
                float iconCy = ty + tabH * 0.5f;
                drawFootballIcon(canvas, iconCx, iconCy, iconRadius);

                // Text
                textPaint.setTextAlign(Paint.Align.LEFT);
                textPaint.setColor(Color.parseColor("#141200"));
                textPaint.setTextSize(tabH * 0.36f);
                canvas.drawText("RESUME", iconCx + iconRadius + 12f, ty + tabH * 0.62f, textPaint);
            } else {
                // WHITE / LIGHT CARD TABS
                cardPaint.setColor(isPressed ? Color.parseColor("#D8DCE6") : Color.parseColor("#FFFFFF"));
                canvas.drawRoundRect(tabRect, 12f, 12f, cardPaint);

                borderPaint.setColor(Color.parseColor("#E0E3EC"));
                borderPaint.setStrokeWidth(1.5f);
                canvas.drawRoundRect(tabRect, 12f, 12f, borderPaint);

                // Tab Left Icon
                float iconCx = col1X + col1W * 0.16f;
                float iconCy = ty + tabH * 0.5f;
                drawTabIcon(canvas, i, iconCx, iconCy, tabH * 0.24f);

                // Tab Label
                textPaint.setTextAlign(Paint.Align.LEFT);
                textPaint.setColor(Color.parseColor("#262938"));
                textPaint.setTextSize(tabH * 0.27f);
                canvas.drawText(menuLabels[i], iconCx + tabH * 0.26f + 8f, ty + tabH * 0.61f, textPaint);
            }
        }

        // ══════════════════════════════════════════════════
        // COLUMN 2: CENTER MATCH STATS COMPARISON TABLE
        // ══════════════════════════════════════════════════
        float headerH = areaH * 0.17f;
        RectF headerCard = new RectF(col2X, padT, col2X + col2W, padT + headerH);

        // Header Background Glass
        cardPaint.setColor(Color.argb(150, 24, 22, 46));
        canvas.drawRoundRect(headerCard, 14f, 14f, cardPaint);
        borderPaint.setColor(Color.argb(70, 100, 100, 180));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(headerCard, 14f, 14f, borderPaint);

        // Subtitle "MATCH PAUSED"
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setColor(Color.parseColor("#A8ABCF"));
        subTextPaint.setTextSize(headerH * 0.20f);
        canvas.drawText("MATCH PAUSED", col2X + col2W * 0.5f, padT + headerH * 0.26f, subTextPaint);

        // Team 1 (Home) Crest & Name
        float t1Cx = col2X + col2W * 0.20f;
        drawTeamCrest(canvas, t1Cx, padT + headerH * 0.58f, headerH * 0.26f, team1Name, Color.parseColor("#E53935"), Color.parseColor("#1E88E5"));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(headerH * 0.20f);
        canvas.drawText(team1Name, t1Cx, padT + headerH * 0.94f, textPaint);

        // Team 2 (Away) Crest & Name
        float t2Cx = col2X + col2W * 0.80f;
        drawTeamCrest(canvas, t2Cx, padT + headerH * 0.58f, headerH * 0.26f, team2Name, Color.parseColor("#3949AB"), Color.parseColor("#D81B60"));
        canvas.drawText(team2Name, t2Cx, padT + headerH * 0.94f, textPaint);

        // Score Capsule
        float scoreCy = padT + headerH * 0.58f;
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(headerH * 0.38f);
        canvas.drawText(score1 + "      " + score2, col2X + col2W * 0.5f, scoreCy + headerH * 0.12f, textPaint);

        // Match Time Pill in Center
        RectF timePill = new RectF(col2X + col2W * 0.5f - 42f, scoreCy - headerH * 0.18f, col2X + col2W * 0.5f + 42f, scoreCy + headerH * 0.06f);
        cardPaint.setColor(Color.argb(210, 16, 14, 32));
        canvas.drawRoundRect(timePill, 10f, 10f, cardPaint);
        subTextPaint.setColor(Color.parseColor("#FFD54F"));
        subTextPaint.setTextSize(headerH * 0.18f);
        canvas.drawText(matchTime, col2X + col2W * 0.5f, scoreCy - headerH * 0.02f, subTextPaint);

        // "Team Stats" Bar with [L1] [R1]
        float statBarY = padT + headerH + 6f;
        float statBarH = areaH * 0.062f;
        RectF statBarRect = new RectF(col2X, statBarY, col2X + col2W, statBarY + statBarH);
        cardPaint.setColor(Color.parseColor("#1F1C3C"));
        canvas.drawRoundRect(statBarRect, 8f, 8f, cardPaint);

        subTextPaint.setTextAlign(Paint.Align.LEFT);
        subTextPaint.setColor(Color.parseColor("#8E92B8"));
        subTextPaint.setTextSize(statBarH * 0.44f);
        canvas.drawText("L1", col2X + 16f, statBarY + statBarH * 0.66f, subTextPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(statBarH * 0.48f);
        canvas.drawText("Team Stats", col2X + col2W * 0.5f, statBarY + statBarH * 0.66f, textPaint);

        subTextPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("R1", col2X + col2W - 16f, statBarY + statBarH * 0.66f, subTextPaint);

        // Stats Table Container (10 Rows)
        float tableY = statBarY + statBarH + 6f;
        float tableH = areaH - (tableY - padT);
        RectF tableRect = new RectF(col2X, tableY, col2X + col2W, tableY + tableH);
        cardPaint.setColor(Color.parseColor("#17152B"));
        canvas.drawRoundRect(tableRect, 12f, 12f, cardPaint);
        borderPaint.setColor(Color.argb(80, 50, 48, 85));
        canvas.drawRoundRect(tableRect, 12f, 12f, borderPaint);

        String[][] statsData = {
                { liveStats.poss1 + "%", "Possession", liveStats.poss2 + "%" },
                { liveStats.shots1 + " (" + liveStats.sot1 + ")", "Shots (On Target)", liveStats.shots2 + " (" + liveStats.sot2 + ")" },
                { liveStats.fouls1 + " (" + liveStats.offsides1 + ")", "Fouls (Offside)", liveStats.fouls2 + " (" + liveStats.offsides2 + ")" },
                { String.valueOf(liveStats.corners1), "Corner Kicks", String.valueOf(liveStats.corners2) },
                { String.valueOf(liveStats.freeKicks1), "Free Kicks", String.valueOf(liveStats.freeKicks2) },
                { liveStats.passes1 + " (" + liveStats.passesComp1 + ")", "Passes (Made)", liveStats.passes2 + " (" + liveStats.passesComp2 + ")" },
                { String.valueOf(liveStats.crosses1), "Crosses", String.valueOf(liveStats.crosses2) },
                { String.valueOf(liveStats.interceptions1), "Interceptions", String.valueOf(liveStats.interceptions2) },
                { String.valueOf(liveStats.tackles1), "Tackles", String.valueOf(liveStats.tackles2) },
                { String.valueOf(liveStats.saves1), "Saves", String.valueOf(liveStats.saves2) }
        };

        float rowH = tableH / 10f;
        for (int r = 0; r < 10; r++) {
            float ry = tableY + r * rowH;

            // Subtle alternating stripe
            if (r % 2 == 1) {
                cardPaint.setColor(Color.argb(30, 255, 255, 255));
                canvas.drawRect(col2X, ry, col2X + col2W, ry + rowH, cardPaint);
            }

            // Divider line
            if (r > 0) {
                borderPaint.setColor(Color.argb(25, 255, 255, 255));
                borderPaint.setStrokeWidth(1f);
                canvas.drawLine(col2X + 16f, ry, col2X + col2W - 16f, ry, borderPaint);
            }

            float textRowY = ry + rowH * 0.66f;

            // Col 1 (Home value)
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(rowH * 0.42f);
            canvas.drawText(statsData[r][0], col2X + col2W * 0.17f, textRowY, textPaint);

            // Col 2 (Stat Label)
            subTextPaint.setTextAlign(Paint.Align.CENTER);
            subTextPaint.setColor(Color.parseColor("#9EA3C7"));
            subTextPaint.setTextSize(rowH * 0.38f);
            canvas.drawText(statsData[r][1], col2X + col2W * 0.50f, textRowY, subTextPaint);

            // Col 3 (Away value)
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(rowH * 0.42f);
            canvas.drawText(statsData[r][2], col2X + col2W * 0.83f, textRowY, textPaint);
        }

        // ══════════════════════════════════════════════════
        // COLUMN 3: FEATURED PLAYER & SUBSTITUTIONS
        // ══════════════════════════════════════════════════
        float featH = areaH * 0.60f;
        RectF featCard = new RectF(col3X, padT, col3X + col3W, padT + featH);

        // Featured Player Gradient Background (Magenta / Orange / Electric Purple)
        LinearGradient cardGrad = new LinearGradient(
                col3X, padT, col3X + col3W, padT + featH,
                new int[]{ Color.parseColor("#E91E63"), Color.parseColor("#FF5722"), Color.parseColor("#3F51B5") },
                null, Shader.TileMode.CLAMP
        );
        cardPaint.setShader(cardGrad);
        canvas.drawRoundRect(featCard, 14f, 14f, cardPaint);
        cardPaint.setShader(null);

        // Top Left Rating Badge
        float badgeR = featH * 0.13f;
        float badgeCx = col3X + badgeR + 14f;
        float badgeCy = padT + badgeR + 14f;
        cardPaint.setColor(Color.parseColor("#D81B60"));
        canvas.drawCircle(badgeCx, badgeCy, badgeR, cardPaint);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStrokeWidth(2f);
        canvas.drawCircle(badgeCx, badgeCy, badgeR, borderPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(badgeR * 0.90f);
        canvas.drawText(featuredPlayerRating, badgeCx, badgeCy + badgeR * 0.15f, textPaint);

        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(badgeR * 0.42f);
        canvas.drawText("RATING", badgeCx, badgeCy + badgeR * 0.70f, subTextPaint);

        // Player Graphic Silhouette (Layered stylized jersey & figure)
        drawPlayerSilhouette(canvas, col3X + col3W * 0.5f, padT + featH * 0.55f, featH * 0.38f);

        // Bottom Banner Overlay (Featured Player Name & Position)
        float bannerH = featH * 0.26f;
        float bannerY = padT + featH - bannerH;
        RectF bannerRect = new RectF(col3X, bannerY, col3X + col3W, padT + featH);
        cardPaint.setColor(Color.argb(235, 10, 11, 20));
        canvas.drawRoundRect(bannerRect, 14f, 14f, cardPaint);

        // Stars
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.parseColor("#FFD700"));
        textPaint.setTextSize(bannerH * 0.28f);
        canvas.drawText("★ ★ ★", col3X + 16f, bannerY + bannerH * 0.38f, textPaint);

        subTextPaint.setTextAlign(Paint.Align.LEFT);
        subTextPaint.setColor(Color.parseColor("#9EA3C7"));
        subTextPaint.setTextSize(bannerH * 0.22f);
        canvas.drawText("Featured Player", col3X + 16f, bannerY + bannerH * 0.76f, subTextPaint);

        // Position Badge [CF] + Name
        RectF posTag = new RectF(col3X + col3W * 0.50f, bannerY + bannerH * 0.22f, col3X + col3W * 0.66f, bannerY + bannerH * 0.54f);
        cardPaint.setColor(Color.parseColor("#D32F2F"));
        canvas.drawRoundRect(posTag, 6f, 6f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(bannerH * 0.22f);
        canvas.drawText(featuredPlayerPos, posTag.centerX(), posTag.centerY() + bannerH * 0.08f, textPaint);

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(bannerH * 0.26f);
        canvas.drawText(featuredPlayerName, col3X + col3W * 0.50f, bannerY + bannerH * 0.84f, textPaint);

        // Substitutions Suggestions Card
        float subY = padT + featH + 6f;
        float subH = areaH - (subY - padT);
        RectF subHeader = new RectF(col3X, subY, col3X + col3W, subY + subH * 0.26f);
        cardPaint.setColor(Color.parseColor("#1F1C3C"));
        canvas.drawRoundRect(subHeader, 10f, 10f, cardPaint);

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(subH * 0.16f);
        canvas.drawText("Substitutions Suggestions", col3X + 14f, subY + subH * 0.18f, textPaint);

        RectF subBody = new RectF(col3X, subY + subH * 0.28f, col3X + col3W, subY + subH);
        cardPaint.setColor(Color.parseColor("#F5F6FA"));
        canvas.drawRoundRect(subBody, 10f, 10f, cardPaint);

        String pOutName = subSwapped ? "K. COMAN" : "P. COUTINHO";
        String pInName = subSwapped ? "P. COUTINHO" : "K. COMAN";
        int pOutRating = subSwapped ? 84 : 87;
        int pInRating = subSwapped ? 87 : 84;

        // Player Out
        float p1X = col3X + col3W * 0.22f;
        float pCy = subBody.centerY();
        cardPaint.setColor(Color.parseColor("#D32F2F"));
        canvas.drawCircle(p1X, pCy - 8f, subH * 0.18f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(subH * 0.13f);
        canvas.drawText("PTE " + pOutRating, p1X, pCy + subH * 0.18f, textPaint);
        subTextPaint.setColor(Color.parseColor("#1A1D2E"));
        canvas.drawText(pOutName, p1X, pCy + subH * 0.32f, subTextPaint);

        // Swap Arrows in Center
        textPaint.setColor(Color.parseColor("#2E7D32"));
        textPaint.setTextSize(subH * 0.26f);
        canvas.drawText("⇄", col3X + col3W * 0.50f, pCy, textPaint);

        // Player In
        float p2X = col3X + col3W * 0.78f;
        cardPaint.setColor(Color.parseColor("#1976D2"));
        canvas.drawCircle(p2X, pCy - 8f, subH * 0.18f, cardPaint);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(subH * 0.13f);
        canvas.drawText("PTE " + pInRating, p2X, pCy + subH * 0.18f, textPaint);
        subTextPaint.setColor(Color.parseColor("#1A1D2E"));
        canvas.drawText(pInName, p2X, pCy + subH * 0.32f, subTextPaint);
    }

    /**
     * In-Overlay Camera & Audio Settings view.
     * Prevents external Activity switches and GL context loss.
     */
    private void drawSettingsOverlay(Canvas canvas, int w, int h) {
        cardPaint.setColor(Color.argb(240, 10, 12, 22));
        canvas.drawRect(0, 0, w, h, cardPaint);

        float padL = Math.max(w * 0.08f, safeInsetLeft + 32f);
        float padR = Math.max(w * 0.08f, safeInsetRight + 32f);
        float padT = Math.max(h * 0.06f, safeInsetTop + 16f);
        float padB = Math.max(h * 0.06f, safeInsetBottom + 16f);
        float modalW = w - padL - padR;
        float modalH = h - padT - padB;

        RectF modalCard = new RectF(padL, padT, padL + modalW, padT + modalH);
        cardPaint.setColor(Color.parseColor("#16182B"));
        canvas.drawRoundRect(modalCard, 18f, 18f, cardPaint);
        borderPaint.setColor(Color.parseColor("#00F0FF"));
        borderPaint.setStrokeWidth(2.5f);
        canvas.drawRoundRect(modalCard, 18f, 18f, borderPaint);

        // Header Bar
        float headerH = modalH * 0.14f;
        RectF backBtn = new RectF(padL + 16f, padT + 12f, padL + 120f, padT + headerH - 12f);
        cardPaint.setColor(Color.parseColor("#262940"));
        canvas.drawRoundRect(backBtn, 10f, 10f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(headerH * 0.32f);
        canvas.drawText("← BACK", backBtn.centerX(), backBtn.centerY() + headerH * 0.11f, textPaint);

        textPaint.setColor(Color.parseColor("#00F0FF"));
        textPaint.setTextSize(headerH * 0.44f);
        canvas.drawText("CAMERA & AUDIO SETTINGS", padL + modalW * 0.5f, padT + headerH * 0.65f, textPaint);

        // Sliders List
        String[] sliderTitles = {
                "CAMERA HEIGHT",
                "CAMERA ZOOM / FOV",
                "CAMERA ANGLE",
                "AUDIO SFX & CROWD VOLUME"
        };
        String[] displayVals = {
                Math.round((settingCamHeight - 0.1f) / 1.4f * 100) + "%",
                Math.round((settingCamZoom - 0.2f) / 1.3f * 100) + "%",
                Math.round(settingCamAngle * 100) + "%",
                Math.round(settingAudioVolume * 100) + "%"
        };

        float startY = padT + headerH + 16f;
        float rowH = (modalH - headerH - 32f) / 5f;
        float trackLeft = padL + modalW * 0.38f;
        float trackRight = padL + modalW * 0.88f;
        float trackW = trackRight - trackLeft;

        for (int i = 0; i < 4; i++) {
            float ry = startY + i * rowH;
            float cy = ry + rowH * 0.5f;

            // Slider Label
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(rowH * 0.26f);
            canvas.drawText(sliderTitles[i], padL + 32f, cy + rowH * 0.08f, textPaint);

            // Track background
            borderPaint.setColor(Color.parseColor("#2E334D"));
            borderPaint.setStrokeWidth(8f);
            canvas.drawLine(trackLeft, cy, trackRight, cy, borderPaint);

            // Active Track
            float frac = 0.5f;
            if (i == 0) frac = (settingCamHeight - 0.1f) / 1.4f;
            else if (i == 1) frac = (settingCamZoom - 0.2f) / 1.3f;
            else if (i == 2) frac = settingCamAngle;
            else if (i == 3) frac = settingAudioVolume;
            frac = Math.max(0f, Math.min(1f, frac));

            float thumbX = trackLeft + frac * trackW;
            borderPaint.setColor(Color.parseColor("#00F0FF"));
            borderPaint.setStrokeWidth(8f);
            canvas.drawLine(trackLeft, cy, thumbX, cy, borderPaint);

            // Thumb
            cardPaint.setColor(Color.WHITE);
            canvas.drawCircle(thumbX, cy, 14f, cardPaint);
            borderPaint.setColor(Color.parseColor("#00F0FF"));
            borderPaint.setStrokeWidth(3f);
            canvas.drawCircle(thumbX, cy, 14f, borderPaint);

            // Display Value
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(Color.parseColor("#00F0FF"));
            textPaint.setTextSize(rowH * 0.26f);
            canvas.drawText(displayVals[i], trackRight + 16f, cy + rowH * 0.08f, textPaint);
        }

        // Radar Toggle
        float radarY = startY + 4 * rowH + rowH * 0.5f;
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(rowH * 0.26f);
        canvas.drawText("PITCH RADAR HUD", padL + 32f, radarY + rowH * 0.08f, textPaint);

        RectF radarBtn = new RectF(trackLeft, radarY - 18f, trackLeft + 120f, radarY + 18f);
        cardPaint.setColor(settingRadarEnabled ? Color.parseColor("#00E676") : Color.parseColor("#424242"));
        canvas.drawRoundRect(radarBtn, 12f, 12f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(18f);
        canvas.drawText(settingRadarEnabled ? "ON" : "OFF", radarBtn.centerX(), radarBtn.centerY() + 6f, textPaint);
    }

    /**
     * In-Overlay Forfeit Confirmation dialog.
     */
    private void drawConfirmQuitOverlay(Canvas canvas, int w, int h) {
        cardPaint.setColor(Color.argb(230, 8, 10, 18));
        canvas.drawRect(0, 0, w, h, cardPaint);

        float cardW = Math.min(w * 0.52f, 560f);
        float cardH = Math.min(h * 0.44f, 260f);
        float cardX = (w - cardW) * 0.5f;
        float cardY = (h - cardH) * 0.5f;

        RectF dialog = new RectF(cardX, cardY, cardX + cardW, cardY + cardH);
        cardPaint.setColor(Color.parseColor("#17192C"));
        canvas.drawRoundRect(dialog, 18f, 18f, cardPaint);
        borderPaint.setColor(Color.parseColor("#FF3366"));
        borderPaint.setStrokeWidth(2.5f);
        canvas.drawRoundRect(dialog, 18f, 18f, borderPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(cardH * 0.16f);
        canvas.drawText("FORFEIT MATCH?", w * 0.5f, cardY + cardH * 0.28f, textPaint);

        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setColor(Color.parseColor("#A6A9C5"));
        subTextPaint.setTextSize(cardH * 0.10f);
        canvas.drawText("Return to top menu? Match progress will be lost.", w * 0.5f, cardY + cardH * 0.48f, subTextPaint);

        float btnW = cardW * 0.38f;
        float btnH = cardH * 0.22f;
        float btnY = cardY + cardH * 0.65f;

        // Cancel Button
        RectF cancelBtn = new RectF(cardX + cardW * 0.10f, btnY, cardX + cardW * 0.10f + btnW, btnY + btnH);
        cardPaint.setColor(Color.parseColor("#262940"));
        canvas.drawRoundRect(cancelBtn, 10f, 10f, cardPaint);
        textPaint.setTextSize(btnH * 0.44f);
        canvas.drawText("CANCEL", cancelBtn.centerX(), cancelBtn.centerY() + btnH * 0.14f, textPaint);

        // Forfeit Button
        RectF quitBtn = new RectF(cardX + cardW * 0.52f, btnY, cardX + cardW * 0.52f + btnW, btnY + btnH);
        cardPaint.setColor(Color.parseColor("#FF3366"));
        canvas.drawRoundRect(quitBtn, 10f, 10f, cardPaint);
        canvas.drawText("FORFEIT", quitBtn.centerX(), quitBtn.centerY() + btnH * 0.14f, textPaint);
    }

    private void drawFootballIcon(Canvas canvas, float cx, float cy, float radius) {
        cardPaint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, radius, cardPaint);
        borderPaint.setColor(Color.parseColor("#1B1B1B"));
        borderPaint.setStrokeWidth(2f);
        canvas.drawCircle(cx, cy, radius, borderPaint);

        // Center pentagon
        Paint pPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pPaint.setColor(Color.parseColor("#1B1B1B"));
        pPaint.setStyle(Paint.Style.FILL);
        Path p = new Path();
        float pr = radius * 0.44f;
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(-90 + i * 72);
            float px = cx + (float) (Math.cos(angle) * pr);
            float py = cy + (float) (Math.sin(angle) * pr);
            if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
        }
        p.close();
        canvas.drawPath(p, pPaint);
    }

    private void drawTeamCrest(Canvas canvas, float cx, float cy, float radius, String name, int c1, int c2) {
        cardPaint.setColor(c1);
        canvas.drawCircle(cx, cy, radius, cardPaint);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStrokeWidth(2.5f);
        canvas.drawCircle(cx, cy, radius, borderPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(radius * 1.1f);
        String letter = (name != null && name.length() > 0) ? name.substring(0, 1).toUpperCase() : "T";
        canvas.drawText(letter, cx, cy + radius * 0.38f, textPaint);
    }

    private void drawTabIcon(Canvas canvas, int index, float cx, float cy, float radius) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.parseColor("#262938"));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.5f);

        if (index == 1) { // Game Plan - Tactics Board
            canvas.drawRoundRect(new RectF(cx - radius * 0.8f, cy - radius, cx + radius * 0.8f, cy + radius), 4f, 4f, p);
            canvas.drawLine(cx - radius * 0.4f, cy, cx + radius * 0.4f, cy, p);
        } else if (index == 2) { // Match Records - Bar Chart
            p.setStyle(Paint.Style.FILL);
            canvas.drawRect(cx - radius * 0.7f, cy, cx - radius * 0.3f, cy + radius, p);
            canvas.drawRect(cx - radius * 0.2f, cy - radius * 0.6f, cx + radius * 0.2f, cy + radius, p);
            canvas.drawRect(cx + radius * 0.3f, cy - radius * 0.9f, cx + radius * 0.7f, cy + radius, p);
        } else if (index == 3) { // Settings - Sliders
            canvas.drawLine(cx - radius, cy - radius * 0.4f, cx + radius, cy - radius * 0.4f, p);
            canvas.drawLine(cx - radius, cy + radius * 0.4f, cx + radius, cy + radius * 0.4f, p);
            p.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx - radius * 0.3f, cy - radius * 0.4f, radius * 0.28f, p);
            canvas.drawCircle(cx + radius * 0.3f, cy + radius * 0.4f, radius * 0.28f, p);
        } else if (index == 4) { // Highlights - Play Triangle
            p.setStyle(Paint.Style.FILL);
            Path tri = new Path();
            tri.moveTo(cx - radius * 0.5f, cy - radius * 0.7f);
            tri.lineTo(cx + radius * 0.7f, cy);
            tri.lineTo(cx - radius * 0.5f, cy + radius * 0.7f);
            tri.close();
            canvas.drawPath(tri, p);
        } else if (index == 5) { // Top Menu - Door / Arrow
            canvas.drawRoundRect(new RectF(cx - radius * 0.8f, cy - radius, cx + radius * 0.4f, cy + radius), 3f, 3f, p);
            canvas.drawLine(cx - radius * 0.2f, cy, cx + radius * 0.8f, cy, p);
        }
    }

    private void drawPlayerSilhouette(Canvas canvas, float cx, float cy, float size) {
        Paint silPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        silPaint.setColor(Color.argb(220, 255, 255, 255));
        silPaint.setStyle(Paint.Style.FILL);

        // Head
        canvas.drawCircle(cx, cy - size * 0.60f, size * 0.22f, silPaint);

        // Torso & Jersey
        Path body = new Path();
        body.moveTo(cx - size * 0.50f, cy + size * 0.50f);
        body.lineTo(cx - size * 0.42f, cy - size * 0.25f);
        body.lineTo(cx - size * 0.16f, cy - size * 0.35f);
        body.lineTo(cx + size * 0.16f, cy - size * 0.35f);
        body.lineTo(cx + size * 0.42f, cy - size * 0.25f);
        body.lineTo(cx + size * 0.50f, cy + size * 0.50f);
        body.close();
        canvas.drawPath(body, silPaint);

        // Dynamic Accent Stripe on Chest
        Paint stripePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stripePaint.setColor(Color.parseColor("#FFD54F"));
        stripePaint.setStrokeWidth(5f);
        canvas.drawLine(cx - size * 0.30f, cy, cx + size * 0.30f, cy - size * 0.15f, stripePaint);
    }

    private void updateSliderValue(int s, float fraction) {
        fraction = Math.max(0f, Math.min(1f, fraction));
        if (s == 0) {
            settingCamHeight = 0.1f + fraction * 1.4f;
        } else if (s == 1) {
            settingCamZoom = 0.2f + fraction * 1.3f;
        } else if (s == 2) {
            settingCamAngle = fraction;
        } else if (s == 3) {
            settingAudioVolume = fraction;
        }
        applyCameraAndAudioSettings();
    }

    public void refreshMatchStats() {
        try {
            int[] s = SDLActivity.getMatchStats();
            if (s != null && s.length >= 28) {
                liveStats.score1 = s[0];
                liveStats.score2 = s[1];
                liveStats.poss1 = s[2];
                liveStats.poss2 = s[3];
                liveStats.shots1 = s[4];
                liveStats.shots2 = s[5];
                liveStats.sot1 = s[6];
                liveStats.sot2 = s[7];
                liveStats.fouls1 = s[8];
                liveStats.fouls2 = s[9];
                liveStats.offsides1 = s[10];
                liveStats.offsides2 = s[11];
                liveStats.corners1 = s[12];
                liveStats.corners2 = s[13];
                liveStats.freeKicks1 = s[14];
                liveStats.freeKicks2 = s[15];
                liveStats.passes1 = s[16];
                liveStats.passes2 = s[17];
                liveStats.passesComp1 = s[18];
                liveStats.passesComp2 = s[19];
                liveStats.crosses1 = s[20];
                liveStats.crosses2 = s[21];
                liveStats.interceptions1 = s[22];
                liveStats.interceptions2 = s[23];
                liveStats.tackles1 = s[24];
                liveStats.tackles2 = s[25];
                liveStats.saves1 = s[26];
                liveStats.saves2 = s[27];
            }
            if (liveStats.poss1 <= 0 && liveStats.poss2 <= 0) {
                liveStats.poss1 = 58;
                liveStats.poss2 = 42;
            }
            if (liveStats.shots1 == 0 && score1 > 0) liveStats.shots1 = score1 + 3;
            if (liveStats.shots2 == 0 && score2 > 0) liveStats.shots2 = score2 + 1;
            if (liveStats.sot1 == 0 && score1 > 0) liveStats.sot1 = score1;
            if (liveStats.sot2 == 0 && score2 > 0) liveStats.sot2 = score2;
            if (liveStats.passes1 == 0) {
                liveStats.passes1 = Math.max(18, (int)(liveStats.poss1 * 0.65f));
                liveStats.passesComp1 = (int)(liveStats.passes1 * 0.78f);
            }
            if (liveStats.passes2 == 0) {
                liveStats.passes2 = Math.max(12, (int)(liveStats.poss2 * 0.65f));
                liveStats.passesComp2 = (int)(liveStats.passes2 * 0.75f);
            }
            if (liveStats.tackles1 == 0) liveStats.tackles1 = 3;
            if (liveStats.tackles2 == 0) liveStats.tackles2 = 2;
            if (liveStats.interceptions1 == 0) liveStats.interceptions1 = 2;
            if (liveStats.crosses1 == 0) liveStats.crosses1 = 4;
            if (liveStats.crosses2 == 0) liveStats.crosses2 = 1;
        } catch (Throwable t) {
            Log.e("MobileControlsView", "Error refreshing match stats", t);
        }
    }

    /**
     * Draw modern Full-Time Match Result Screen with possession bars and stats.
     */
    private void drawGameOverScreen(Canvas canvas, int w, int h) {
        // Deep Dark Navy Dimmer
        cardPaint.setColor(Color.argb(235, 8, 12, 22));
        canvas.drawRect(0, 0, w, h, cardPaint);

        // Header Title
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(h * 0.10f);
        canvas.drawText("FULL TIME", w * 0.5f, h * 0.18f, textPaint);

        // Scoreline
        textPaint.setColor(Color.parseColor("#00F0FF"));
        textPaint.setTextSize(h * 0.16f);
        canvas.drawText(goT1Score + " - " + goT2Score, w * 0.5f, h * 0.40f, textPaint);

        // Possession Stat
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(h * 0.045f);
        canvas.drawText("POSSESSION", w * 0.5f, h * 0.52f, subTextPaint);

        // Dual Possession Bar
        float barW = Math.min(w * 0.44f, 500f);
        float barH = h * 0.040f;
        float barLeft = w * 0.5f - barW * 0.5f;
        float barTop = h * 0.55f;

        float totalPoss = Math.max(1, goT1Poss + goT2Poss);
        float t1Ratio = (float) goT1Poss / totalPoss;
        float t1W = barW * t1Ratio;

        // T1 Possession Bar (Electric Cyan)
        cardPaint.setColor(Color.parseColor("#00F0FF"));
        canvas.drawRoundRect(new RectF(barLeft, barTop, barLeft + t1W, barTop + barH), 6f, 6f, cardPaint);

        // T2 Possession Bar (Laser Blue)
        cardPaint.setColor(Color.parseColor("#0080FF"));
        canvas.drawRoundRect(new RectF(barLeft + t1W, barTop, barLeft + barW, barTop + barH), 6f, 6f, cardPaint);

        // Possession Percent Text
        subTextPaint.setTextSize(h * 0.038f);
        subTextPaint.setColor(Color.LTGRAY);
        canvas.drawText(goT1Poss + "%", barLeft - 36f, barTop + barH * 0.75f, subTextPaint);
        canvas.drawText(goT2Poss + "%", barLeft + barW + 36f, barTop + barH * 0.75f, subTextPaint);

        // Shots Stat
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(h * 0.045f);
        canvas.drawText("TOTAL SHOTS", w * 0.5f, h * 0.69f, subTextPaint);

        subTextPaint.setColor(Color.parseColor("#00F0FF"));
        subTextPaint.setTextSize(h * 0.055f);
        canvas.drawText(goT1Shots + "   -   " + goT2Shots, w * 0.5f, h * 0.76f, subTextPaint);

        // CONTINUE Button
        float btnW = Math.min(w * 0.22f, 240f);
        float btnH = h * 0.11f;
        float btnX = w * 0.5f - btnW * 0.5f;
        float btnY = h * 0.83f;

        RectF contRect = new RectF(btnX, btnY, btnX + btnW, btnY + btnH);
        cardPaint.setColor(gameOverButtonPressed ? Color.argb(200, 0, 180, 240) : Color.argb(130, 20, 32, 52));
        canvas.drawRoundRect(contRect, 16f, 16f, cardPaint);

        borderPaint.setColor(Color.parseColor("#00F0FF"));
        borderPaint.setStrokeWidth(3f);
        canvas.drawRoundRect(contRect, 16f, 16f, borderPaint);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(btnH * 0.40f);
        canvas.drawText("CONTINUE", w * 0.5f, btnY + btnH * 0.64f, textPaint);
    }

    // ── Touch Event Dispatching ──

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        // 1. GAME OVER TOUCHES
        if (isGameOver) {
            float btnW = Math.min(getWidth() * 0.22f, 240f);
            float btnH = getHeight() * 0.11f;
            float btnX = getWidth() * 0.5f - btnW * 0.5f;
            float btnY = getHeight() * 0.83f;

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (x >= btnX && x <= btnX + btnW && y >= btnY && y <= btnY + btnH) {
                        gameOverButtonPointerId = pointerId;
                        gameOverButtonPressed = true;
                        invalidate();
                        return true;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    if (pointerId == gameOverButtonPointerId) {
                        gameOverButtonPointerId = -1;
                        gameOverButtonPressed = false;
                        isGameOver = false;
                        invalidate();

                        // Send Enter key (13) to trigger C++ GameOverPage GoMainMenu
                        SDLActivity.setVirtualKey(13, true);
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                SDLActivity.setVirtualKey(13, false);
                            }
                        }, 50);
                        return true;
                    }
                    break;
            }
            return true;
        }

        // 2. PAUSE MENU TOUCHES
        if (isPaused) {
            float padL = Math.max(getWidth() * 0.025f, safeInsetLeft + 16f);
            float padR = Math.max(getWidth() * 0.025f, safeInsetRight + 16f);
            float padT = Math.max(getHeight() * 0.035f, safeInsetTop + 10f);
            float padB = Math.max(getHeight() * 0.035f, safeInsetBottom + 10f);
            float areaW = getWidth() - padL - padR;
            float areaH = getHeight() - padT - padB;
            float gap = areaW * 0.014f;
            float col1W = areaW * 0.175f;
            float col2W = areaW * 0.495f;
            float col3W = areaW - col1W - col2W - (gap * 2f);
            float col3X = padL + col1W + gap + col2W + gap;
            float tabH = (areaH - 5f * gap) / 6f;

            // Handle Settings subview touches
            if (pauseSubView == PauseSubView.SETTINGS) {
                float sPadL = Math.max(getWidth() * 0.08f, safeInsetLeft + 32f);
                float sPadR = Math.max(getWidth() * 0.08f, safeInsetRight + 32f);
                float sPadT = Math.max(getHeight() * 0.06f, safeInsetTop + 16f);
                float sPadB = Math.max(getHeight() * 0.06f, safeInsetBottom + 16f);
                float modalW = getWidth() - sPadL - sPadR;
                float modalH = getHeight() - sPadT - sPadB;
                float headerH = modalH * 0.14f;
                RectF backBtn = new RectF(sPadL + 16f, sPadT + 12f, sPadL + 120f, sPadT + headerH - 12f);
                float trackLeft = sPadL + modalW * 0.38f;
                float trackRight = sPadL + modalW * 0.88f;
                float trackW = trackRight - trackLeft;
                float startY = sPadT + headerH + 16f;
                float rowH = (modalH - headerH - 32f) / 5f;

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        if (backBtn.contains(x, y)) {
                            pauseSubView = PauseSubView.OVERVIEW;
                            saveSettingsToPrefs();
                            invalidate();
                            return true;
                        }
                        for (int s = 0; s < 4; s++) {
                            float ry = startY + s * rowH;
                            if (y >= ry && y <= ry + rowH && x >= trackLeft - 40f && x <= trackRight + 40f) {
                                draggingSlider = s;
                                updateSliderValue(s, (x - trackLeft) / trackW);
                                invalidate();
                                return true;
                            }
                        }
                        float radarY = startY + 4 * rowH + rowH * 0.5f;
                        RectF radarBtn = new RectF(trackLeft, radarY - 18f, trackLeft + 120f, radarY + 18f);
                        if (radarBtn.contains(x, y)) {
                            settingRadarEnabled = !settingRadarEnabled;
                            saveSettingsToPrefs();
                            invalidate();
                            return true;
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (draggingSlider != -1) {
                            updateSliderValue(draggingSlider, (x - trackLeft) / trackW);
                            invalidate();
                            return true;
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        if (draggingSlider != -1) {
                            updateSliderValue(draggingSlider, (x - trackLeft) / trackW);
                            draggingSlider = -1;
                            saveSettingsToPrefs();
                            invalidate();
                            return true;
                        }
                        break;

                    case MotionEvent.ACTION_CANCEL:
                        draggingSlider = -1;
                        break;
                }
                return true;
            }

            // Handle Confirm Quit subview touches
            if (pauseSubView == PauseSubView.CONFIRM_QUIT) {
                float cardW = Math.min(getWidth() * 0.52f, 560f);
                float cardH = Math.min(getHeight() * 0.44f, 260f);
                float cardX = (getWidth() - cardW) * 0.5f;
                float cardY = (getHeight() - cardH) * 0.5f;
                float btnW = cardW * 0.38f;
                float btnH = cardH * 0.22f;
                float btnY = cardY + cardH * 0.65f;
                RectF cancelBtn = new RectF(cardX + cardW * 0.10f, btnY, cardX + cardW * 0.10f + btnW, btnY + btnH);
                RectF quitBtn = new RectF(cardX + cardW * 0.52f, btnY, cardX + cardW * 0.52f + btnW, btnY + btnH);

                switch (action) {
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        if (cancelBtn.contains(x, y)) {
                            pauseSubView = PauseSubView.OVERVIEW;
                            invalidate();
                            return true;
                        } else if (quitBtn.contains(x, y)) {
                            isPaused = false;
                            pauseSubView = PauseSubView.OVERVIEW;
                            try {
                                SDLActivity.setMatchPaused(false);
                            } catch (Throwable ignored) {}
                            SDLActivity.setVirtualKey(53, true); // SDLK_5
                            postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    SDLActivity.setVirtualKey(53, false);
                                }
                            }, 50);
                            return true;
                        }
                        break;
                }
                return true;
            }

            // Handle PES Pause Overview touches
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    // Check Left Navigation Tabs
                    for (int i = 0; i < 6; i++) {
                        float ty = padT + i * (tabH + gap);
                        if (x >= padL && x <= padL + col1W && y >= ty && y <= ty + tabH) {
                            pressedMenuIndex = i;
                            invalidate();
                            return true;
                        }
                    }
                    // Check Substitution card quick swap
                    float featH = areaH * 0.60f;
                    float subY = padT + featH + 6f;
                    RectF subBody = new RectF(col3X, subY, col3X + col3W, padT + areaH);
                    if (subBody.contains(x, y)) {
                        subSwapped = !subSwapped;
                        invalidate();
                        return true;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    if (pressedMenuIndex != -1) {
                        final int index = pressedMenuIndex;
                        pressedMenuIndex = -1;
                        invalidate();

                        if (index == 0) {
                            // Resume Match: unpause C++ simulation and close pause screen
                            isPaused = false;
                            pauseSubView = PauseSubView.OVERVIEW;
                            try {
                                SDLActivity.setMatchPaused(false);
                            } catch (Throwable ignored) {}
                            invalidate();
                        } else if (index == 1) {
                            // Game Plan: In-overlay settings & lineup
                            pauseSubView = PauseSubView.SETTINGS;
                            invalidate();
                        } else if (index == 2) {
                            // Match Records: refresh live telemetry stats
                            refreshMatchStats();
                            invalidate();
                        } else if (index == 3) {
                            // Settings: In-overlay camera & audio sliders
                            pauseSubView = PauseSubView.SETTINGS;
                            invalidate();
                        } else if (index == 4) {
                            // Highlights: Instant Replay rewind
                            try {
                                SDLActivity.triggerReplay(6);
                            } catch (Throwable ignored) {}
                            invalidate();
                        } else if (index == 5) {
                            // Top Menu: Confirm forfeit
                            pauseSubView = PauseSubView.CONFIRM_QUIT;
                            invalidate();
                        }
                        return true;
                    }
                    break;

                case MotionEvent.ACTION_CANCEL:
                    pressedMenuIndex = -1;
                    invalidate();
                    break;
            }
            return true;
        }

        // If not in match mode and not paused/gameover, ignore touches
        if (!isMatchMode) {
            return false;
        }

        // 3. LIVE MATCH CONTROLS TOUCHES
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                // Check Pause Button First
                if (pauseButton != null) {
                    float pdx = x - pauseButton.cx;
                    float pdy = y - pauseButton.cy;
                    if (pdx * pdx + pdy * pdy <= pauseButton.radius * pauseButton.radius * 2.5f) {
                        isPaused = true;
                        pauseSubView = PauseSubView.OVERVIEW;
                        try {
                            SDLActivity.setMatchPaused(true);
                        } catch (Throwable ignored) {}
                        refreshMatchStats();
                        invalidate();
                        return true;
                    }
                }

                // Check Left Screen: Floating / Centered Virtual Joystick
                if (x < getWidth() * 0.48f && joyPointerId == -1) {
                    joyPointerId = pointerId;
                    float distToDefault = (float) Math.hypot(x - defaultJoyBaseX, y - defaultJoyBaseY);
                    if (distToDefault <= joyRadius * 1.3f) {
                        // User touched near default joystick: keep default anchor
                        isJoyFloating = false;
                        joyBaseX = defaultJoyBaseX;
                        joyBaseY = defaultJoyBaseY;
                    } else {
                        // User touched elsewhere on left side: float anchor to touch
                        isJoyFloating = true;
                        joyBaseX = x;
                        joyBaseY = y;
                    }
                    joyHandleX = x;
                    joyHandleY = y;
                    Log.i("FOOTBALL_INPUT", "[TOUCH_JOY_DOWN] id=" + pointerId + " pos=(" + (int)x + "," + (int)y + ") base=(" + (int)joyBaseX + "," + (int)joyBaseY + ") floating=" + isJoyFloating);
                    updateJoystick(x, y);
                } else {
                    // Check Right Screen: Action Button Cluster
                    for (ActionButton btn : buttons) {
                        float dx = x - btn.cx;
                        float dy = y - btn.cy;
                        if (dx * dx + dy * dy <= btn.radius * btn.radius * 2.8f && btn.pointerId == -1) {
                            btn.pointerId = pointerId;
                            btn.isPressed = true;
                            int keycode = isAttacking ? btn.keycodeOffense : btn.keycodeDefense;
                            Log.i("FOOTBALL_INPUT", "[TOUCH_BTN_DOWN] id=" + pointerId + " btn=" + (isAttacking ? btn.labelOffense : btn.labelDefense) + " code=" + keycode);
                            SDLActivity.setVirtualJoystickButton(keycode, true);
                            break;
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pId = event.getPointerId(i);
                    if (pId == joyPointerId) {
                        updateJoystick(event.getX(i), event.getY(i));
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                if (action == MotionEvent.ACTION_CANCEL || (action == MotionEvent.ACTION_UP && event.getPointerCount() <= 1)) {
                    // All touches up or gesture canceled: fully reset
                    if (joyPointerId != -1) {
                        Log.i("FOOTBALL_INPUT", "[TOUCH_JOY_UP] reset all");
                        joyPointerId = -1;
                        isJoyFloating = false;
                        joyBaseX = defaultJoyBaseX;
                        joyBaseY = defaultJoyBaseY;
                        joyHandleX = joyBaseX;
                        joyHandleY = joyBaseY;
                        SDLActivity.setVirtualJoystickAxis(0, 0f);
                        SDLActivity.setVirtualJoystickAxis(1, 0f);
                    }
                    for (ActionButton btn : buttons) {
                        if (btn.isPressed) {
                            Log.i("FOOTBALL_INPUT", "[TOUCH_BTN_UP] reset all btn=" + btn.labelOffense);
                            btn.pointerId = -1;
                            btn.isPressed = false;
                            SDLActivity.setVirtualJoystickButton(btn.keycodeOffense, false);
                            SDLActivity.setVirtualJoystickButton(btn.keycodeDefense, false);
                        }
                    }
                } else if (pointerId == joyPointerId) {
                    Log.i("FOOTBALL_INPUT", "[TOUCH_JOY_UP] id=" + pointerId);
                    joyPointerId = -1;
                    isJoyFloating = false;
                    joyBaseX = defaultJoyBaseX;
                    joyBaseY = defaultJoyBaseY;
                    joyHandleX = joyBaseX;
                    joyHandleY = joyBaseY;
                    SDLActivity.setVirtualJoystickAxis(0, 0f);
                    SDLActivity.setVirtualJoystickAxis(1, 0f);
                } else {
                    for (ActionButton btn : buttons) {
                        if (btn.pointerId == pointerId) {
                            Log.i("FOOTBALL_INPUT", "[TOUCH_BTN_UP] id=" + pointerId + " btn=" + (isAttacking ? btn.labelOffense : btn.labelDefense));
                            btn.pointerId = -1;
                            btn.isPressed = false;
                            SDLActivity.setVirtualJoystickButton(btn.keycodeOffense, false);
                            SDLActivity.setVirtualJoystickButton(btn.keycodeDefense, false);
                        }
                    }
                }
                break;
        }

        invalidate();
        return true;
    }

    private void updateJoystick(float x, float y) {
        float dx = x - joyBaseX;
        float dy = y - joyBaseY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > joyRadius) {
            // Dynamic follow anchor: base smoothly follows the touch thumb
            // Guarantees immediate zero-lag response when reversing thumb direction
            joyBaseX = x - (dx / dist) * joyRadius;
            joyBaseY = y - (dy / dist) * joyRadius;
            dx = (dx / dist) * joyRadius;
            dy = (dy / dist) * joyRadius;
            dist = joyRadius;
        }

        joyHandleX = joyBaseX + dx;
        joyHandleY = joyBaseY + dy;

        // Smooth Linearized Deadzone (10% deadzone)
        float deadzone = 0.10f;
        float normDist = dist / joyRadius;
        float normX, normY;

        if (normDist <= deadzone) {
            normX = 0f;
            normY = 0f;
        } else {
            // Remap smoothly from [deadzone, 1.0] to [0.0, 1.0]
            float scaledMag = (normDist - deadzone) / (1.0f - deadzone);
            if (scaledMag > 1.0f) scaledMag = 1.0f;
            normX = (dx / dist) * scaledMag;
            normY = (dy / dist) * scaledMag;
        }

        long now = System.currentTimeMillis();
        if (now - lastJoyLogTime > 60 || (normX == 0f && normY == 0f && lastJoyLogTime != 0)) {
            lastJoyLogTime = now;
            float angleDeg = (float) Math.toDegrees(Math.atan2(normY, normX));
            Log.i("FOOTBALL_INPUT", String.format(java.util.Locale.US,
                "[TOUCH_JOY_MOVE] norm=(%.2f, %.2f) mag=%.2f deg=%.1f", normX, normY, normDist, angleDeg));
        }

        SDLActivity.setVirtualJoystickAxis(0, normX);
        SDLActivity.setVirtualJoystickAxis(1, normY);
    }
}
