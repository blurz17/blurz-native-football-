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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import java.io.InputStream;
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
        SETTINGS_GRID,
        SQUAD_MANAGEMENT,
        CONTROLS_LIST,
        GENERAL_SETTINGS,
        CAMERA_SETTINGS,
        SOUND_SETTINGS,
        REPLAY_PLAYBACK,
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
    private float settingCamAngle = 0.50f;
    private float settingAudioVolume = 0.80f;
    private boolean settingRadarEnabled = true;
    private boolean settingPlayerNames = true;
    private int selectedKit1 = 0; // 0=Kit 01, 1=Kit 02
    private int selectedKit2 = 1; // 0=Kit 01, 1=Kit 02
    private int draggingSlider = -1; // 0=Height, 1=Zoom, 2=Angle, 3=Audio
    private int pressedSettingsCardIndex = -1; // 0=Squad, 1=Controls, 2=General, 3=Camera, 4=Sound

    // Team Logos and Branding
    private Bitmap team1LogoBitmap = null;
    private Bitmap team2LogoBitmap = null;
    private String team1ShortName = "BAY";
    private String team2ShortName = "CPJ";

    // Replay Playback State
    private boolean isReplayPlaying = true;
    private final Runnable replayStepper = new Runnable() {
        @Override
        public void run() {
            if (isPaused && pauseSubView == PauseSubView.REPLAY_PLAYBACK && isReplayPlaying) {
                try {
                    SDLActivity.stepReplay(16);
                } catch (Throwable ignored) {}
                postDelayed(this, 16);
            }
        }
    };

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
            settingPlayerNames = prefs.getBoolean("player_names", true);
            applyCameraAndAudioSettings();
            loadTeamData();
        } catch (Throwable ignored) {}
    }

    public void applyCameraAndAudioSettings() {
        try {
            SDLActivity.setCameraSettings(settingCamZoom, settingCamHeight, 0.5f, settingCamAngle);
            SDLActivity.setAudioVolume(settingAudioVolume);
        } catch (Throwable ignored) {}
    }

    public void loadTeamData() {
        try {
            String[] t1 = SDLActivity.getTeamInfo(0);
            if (t1 != null && t1.length >= 4) {
                if (t1[0] != null && !t1[0].isEmpty()) team1Name = t1[0];
                if (t1[1] != null && !t1[1].isEmpty()) team1ShortName = t1[1];
                String path1 = t1[2];
                if (path1 != null && !path1.isEmpty() && team1LogoBitmap == null) {
                    try (InputStream is = getContext().getAssets().open(path1)) {
                        team1LogoBitmap = BitmapFactory.decodeStream(is);
                    } catch (Throwable ignored) {}
                }
            }
            String[] t2 = SDLActivity.getTeamInfo(1);
            if (t2 != null && t2.length >= 4) {
                if (t2[0] != null && !t2[0].isEmpty()) team2Name = t2[0];
                if (t2[1] != null && !t2[1].isEmpty()) team2ShortName = t2[1];
                String path2 = t2[2];
                if (path2 != null && !path2.isEmpty() && team2LogoBitmap == null) {
                    try (InputStream is = getContext().getAssets().open(path2)) {
                        team2LogoBitmap = BitmapFactory.decodeStream(is);
                    } catch (Throwable ignored) {}
                }
            }
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
                    .putBoolean("player_names", settingPlayerNames)
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
        loadTeamData();
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
            this.pauseSubView = PauseSubView.SETTINGS_GRID;
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
        float crestR = sbH * 0.32f;

        // Team 1 Logo & Name
        float t1CrestX = sbX + crestR + 12f;
        drawTeamCrest(canvas, t1CrestX, sbY + sbH * 0.50f, crestR, 0, team1Name, Color.parseColor("#E53935"), Color.parseColor("#1E88E5"));
        textPaint.setTextSize(sbH * 0.38f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(team1ShortName, team1CenterX, textY, textPaint);

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

        // Team 2 Logo & Name
        float t2CrestX = sbX + sbW - colClockW - crestR - 12f;
        drawTeamCrest(canvas, t2CrestX, sbY + sbH * 0.50f, crestR, 1, team2Name, Color.parseColor("#3949AB"), Color.parseColor("#D81B60"));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(sbH * 0.38f);
        canvas.drawText(team2ShortName, team2CenterX, textY, textPaint);

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
     * Draw modern PES Mobile Style Pause Menu Overlay.
     */
    private void drawPauseMenu(Canvas canvas, int w, int h) {
        switch (pauseSubView) {
            case OVERVIEW:
                drawPesPauseOverview(canvas, w, h);
                break;
            case SQUAD_MANAGEMENT:
                drawSquadManagementOverlay(canvas, w, h);
                break;
            case CONTROLS_LIST:
                drawControlsListOverlay(canvas, w, h);
                break;
            case GENERAL_SETTINGS:
                drawGeneralSettingsOverlay(canvas, w, h);
                break;
            case CAMERA_SETTINGS:
                drawCameraSettingsOverlay(canvas, w, h);
                break;
            case SOUND_SETTINGS:
                drawSoundSettingsOverlay(canvas, w, h);
                break;
            case REPLAY_PLAYBACK:
                drawReplayPlaybackOverlay(canvas, w, h);
                break;
            case CONFIRM_QUIT:
                drawConfirmQuitOverlay(canvas, w, h);
                break;
            case SETTINGS_GRID:
            default:
                drawSettingsGrid(canvas, w, h);
                break;
        }
    }

    /**
     * PES Mobile Authentic 5-Card In-Match Pause Menu:
     * Matches media_1791741932997_18ccc2a2.png:
     * - Soft pitch turf backdrop
     * - Large prominent team crests with team name and subtitle below
     * - Big bold clean score in center (tapping opens Match Records)
     * - 5 pure white rounded cards (equal width & height, icons on top, royal blue titles below)
     * - Prominent blue pill button "< Resume" at bottom left
     */
    private void drawSettingsGrid(Canvas canvas, int w, int h) {
        // Soft stadium pitch dimming (translucent so real pitch is visible underneath)
        cardPaint.setColor(Color.argb(140, 8, 16, 12));
        canvas.drawRect(0, 0, w, h, cardPaint);

        float padL = Math.max(w * 0.08f, safeInsetLeft + 28f);
        float padR = Math.max(w * 0.08f, safeInsetRight + 28f);
        float topY = Math.max(h * 0.04f, safeInsetTop + 10f);

        // ── TOP HEADER (Large Team Crests & Prominent Centered Score) ──
        float leftTeamCx = w * 0.25f;
        float rightTeamCx = w * 0.75f;
        float scoreCx = w * 0.50f;

        float crestR = Math.min(h * 0.085f, 44f);
        float crestCy = topY + crestR + 2f;

        // Team 1 Crest (Large Circular Badge)
        drawTeamCrest(canvas, leftTeamCx, crestCy, crestR, 0, team1Name, Color.parseColor("#E53935"), Color.parseColor("#1E88E5"));

        // Team 1 Name below crest
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(Math.min(h * 0.044f, 18f));
        float t1NameY = crestCy + crestR + 18f;
        canvas.drawText(team1Name.toUpperCase(), leftTeamCx, t1NameY, textPaint);

        // Team 1 Subtitle ("blurz" / Manager)
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setColor(Color.parseColor("#B0BEC5"));
        subTextPaint.setTextSize(Math.min(h * 0.032f, 13.5f));
        canvas.drawText("blurz", leftTeamCx, t1NameY + 16f, subTextPaint);

        // Center Big Bold Score "0 - 0"
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(Math.min(h * 0.12f, 48f));
        canvas.drawText(score1 + "  -  " + score2, scoreCx, crestCy + 14f, textPaint);

        // Subtle Match Records Hint under Score
        subTextPaint.setColor(Color.parseColor("#80DEEA"));
        subTextPaint.setTextSize(Math.min(h * 0.028f, 12f));
        canvas.drawText("TAP FOR MATCH STATS", scoreCx, crestCy + 34f, subTextPaint);

        // Team 2 Crest (Large Circular Badge)
        drawTeamCrest(canvas, rightTeamCx, crestCy, crestR, 1, team2Name, Color.parseColor("#3949AB"), Color.parseColor("#D81B60"));

        // Team 2 Name below crest
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(Math.min(h * 0.044f, 18f));
        float t2NameY = crestCy + crestR + 18f;
        canvas.drawText(team2Name.toUpperCase(), rightTeamCx, t2NameY, textPaint);

        // Team 2 Subtitle ("Special Team")
        subTextPaint.setColor(Color.parseColor("#B0BEC5"));
        subTextPaint.setTextSize(Math.min(h * 0.032f, 13.5f));
        canvas.drawText("Special Team", rightTeamCx, t2NameY + 16f, subTextPaint);

        // ── 5 WHITE ROUNDED CARDS GRID (ALL EQUAL IN WIDTH & HEIGHT) ──
        float gridTop = t1NameY + 28f;
        float bottomBarH = 58f;
        float gridBottom = h - Math.max(h * 0.04f, safeInsetBottom + 12f) - bottomBarH;
        float gridW = w - padL - padR;
        float gridH = gridBottom - gridTop;

        float gapX = gridW * 0.032f;
        float gapY = gridH * 0.08f;
        float cardW = (gridW - 2f * gapX) / 3f;
        float cardH = (gridH - gapY) / 2f;

        String[] cardTitles = {
                "Squad Management",
                "Controls List",
                "General Settings",
                "Camera Settings",
                "Sound Settings"
        };

        RectF[] cardRects = new RectF[5];
        // Row 1: 3 cards
        cardRects[0] = new RectF(padL + 0 * (cardW + gapX), gridTop, padL + 0 * (cardW + gapX) + cardW, gridTop + cardH);
        cardRects[1] = new RectF(padL + 1 * (cardW + gapX), gridTop, padL + 1 * (cardW + gapX) + cardW, gridTop + cardH);
        cardRects[2] = new RectF(padL + 2 * (cardW + gapX), gridTop, padL + 2 * (cardW + gapX) + cardW, gridTop + cardH);
        // Row 2: 2 cards (col 0 and col 1, col 2 empty matching PES screenshot)
        cardRects[3] = new RectF(padL + 0 * (cardW + gapX), gridTop + cardH + gapY, padL + 0 * (cardW + gapX) + cardW, gridTop + 2 * cardH + gapY);
        cardRects[4] = new RectF(padL + 1 * (cardW + gapX), gridTop + cardH + gapY, padL + 1 * (cardW + gapX) + cardW, gridTop + 2 * cardH + gapY);

        for (int i = 0; i < 5; i++) {
            RectF r = cardRects[i];
            boolean isPressed = (pressedSettingsCardIndex == i);

            // Pure White Card Fill with soft elevation
            cardPaint.setColor(isPressed ? Color.parseColor("#E4E7ED") : Color.parseColor("#FFFFFF"));
            canvas.drawRoundRect(r, 22f, 22f, cardPaint);

            // Subtle border
            borderPaint.setColor(Color.parseColor("#DFE3EC"));
            borderPaint.setStrokeWidth(1.5f);
            canvas.drawRoundRect(r, 22f, 22f, borderPaint);

            // Icon on upper portion of card
            float iconCx = r.left + 38f;
            float iconCy = r.top + r.height() * 0.38f;
            float iconR = r.height() * 0.22f;
            drawSettingsCardIcon(canvas, i, iconCx, iconCy, iconR);

            // Single-line Royal Blue Title below icon
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(Color.parseColor("#1565C0")); // PES Royal Blue
            textPaint.setTextSize(Math.min(r.height() * 0.17f, 16.5f));
            canvas.drawText(cardTitles[i], r.left + 22f, r.top + r.height() * 0.82f, textPaint);
        }

        // ── BOTTOM LEFT BLUE PILL RESUME BUTTON ──
        float btnY = h - Math.max(h * 0.04f, safeInsetBottom + 12f) - 48f;
        RectF resumeBtn = new RectF(padL, btnY, padL + 160f, btnY + 48f);
        cardPaint.setColor(Color.parseColor("#007AFF")); // Vibrant Blue
        canvas.drawRoundRect(resumeBtn, 24f, 24f, cardPaint);

        borderPaint.setColor(Color.argb(120, 255, 255, 255));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(resumeBtn, 24f, 24f, borderPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(18f);
        canvas.drawText("❮  Resume", resumeBtn.centerX(), resumeBtn.centerY() + 6.5f, textPaint);
    }

    /**
     * PES Mobile Match Records / Stats Comparison Screen:
     * 10-Row Match Stats Comparison Table with authentic live data.
     */
    private void drawPesPauseOverview(Canvas canvas, int w, int h) {
        cardPaint.setColor(Color.argb(238, 10, 12, 24));
        canvas.drawRect(0, 0, w, h, cardPaint);

        float padL = Math.max(w * 0.035f, safeInsetLeft + 20f);
        float padR = Math.max(w * 0.035f, safeInsetRight + 20f);
        float padT = Math.max(h * 0.035f, safeInsetTop + 10f);
        float padB = Math.max(h * 0.035f, safeInsetBottom + 10f);
        float areaW = w - padL - padR;
        float areaH = h - padT - padB;

        // Header Card
        float headerH = areaH * 0.16f;
        RectF headerCard = new RectF(padL, padT, padL + areaW, padT + headerH);
        cardPaint.setColor(Color.argb(160, 24, 22, 46));
        canvas.drawRoundRect(headerCard, 14f, 14f, cardPaint);
        borderPaint.setColor(Color.argb(80, 100, 100, 180));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(headerCard, 14f, 14f, borderPaint);

        // Subtitle "MATCH RECORDS & TELEMETRY"
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setColor(Color.parseColor("#A8ABCF"));
        subTextPaint.setTextSize(headerH * 0.20f);
        canvas.drawText("MATCH RECORDS & TELEMETRY", padL + areaW * 0.5f, padT + headerH * 0.25f, subTextPaint);

        // Team 1 Crest & Name
        float t1Cx = padL + areaW * 0.20f;
        drawTeamCrest(canvas, t1Cx, padT + headerH * 0.56f, headerH * 0.26f, 0, team1Name, Color.parseColor("#E53935"), Color.parseColor("#1E88E5"));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(headerH * 0.22f);
        canvas.drawText(team1Name, t1Cx, padT + headerH * 0.93f, textPaint);

        // Team 2 Crest & Name
        float t2Cx = padL + areaW * 0.80f;
        drawTeamCrest(canvas, t2Cx, padT + headerH * 0.56f, headerH * 0.26f, 1, team2Name, Color.parseColor("#3949AB"), Color.parseColor("#D81B60"));
        canvas.drawText(team2Name, t2Cx, padT + headerH * 0.93f, textPaint);

        // Score
        float scoreCy = padT + headerH * 0.56f;
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(headerH * 0.38f);
        canvas.drawText(score1 + "      " + score2, padL + areaW * 0.5f, scoreCy + headerH * 0.12f, textPaint);

        // Match Time Pill
        RectF timePill = new RectF(padL + areaW * 0.5f - 46f, scoreCy - headerH * 0.19f, padL + areaW * 0.5f + 46f, scoreCy + headerH * 0.06f);
        cardPaint.setColor(Color.argb(210, 16, 14, 32));
        canvas.drawRoundRect(timePill, 10f, 10f, cardPaint);
        subTextPaint.setColor(Color.parseColor("#FFD54F"));
        subTextPaint.setTextSize(headerH * 0.18f);
        canvas.drawText(matchTime, padL + areaW * 0.5f, scoreCy - headerH * 0.02f, subTextPaint);

        // Stats Table Container (10 Rows)
        float tableY = padT + headerH + 10f;
        float bottomBarH = 50f;
        float tableH = areaH - (tableY - padT) - bottomBarH;
        RectF tableRect = new RectF(padL, tableY, padL + areaW, tableY + tableH);
        cardPaint.setColor(Color.parseColor("#17152B"));
        canvas.drawRoundRect(tableRect, 14f, 14f, cardPaint);
        borderPaint.setColor(Color.argb(90, 0, 240, 255));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(tableRect, 14f, 14f, borderPaint);

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
                cardPaint.setColor(Color.argb(25, 255, 255, 255));
                canvas.drawRect(padL, ry, padL + areaW, ry + rowH, cardPaint);
            }

            if (r > 0) {
                borderPaint.setColor(Color.argb(25, 255, 255, 255));
                borderPaint.setStrokeWidth(1f);
                canvas.drawLine(padL + 16f, ry, padL + areaW - 16f, ry, borderPaint);
            }

            float textRowY = ry + rowH * 0.66f;

            // Col 1 (Home value)
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(rowH * 0.42f);
            canvas.drawText(statsData[r][0], padL + areaW * 0.20f, textRowY, textPaint);

            // Col 2 (Stat Label)
            subTextPaint.setTextAlign(Paint.Align.CENTER);
            subTextPaint.setColor(Color.parseColor("#9EA3C7"));
            subTextPaint.setTextSize(rowH * 0.38f);
            canvas.drawText(statsData[r][1], padL + areaW * 0.50f, textRowY, subTextPaint);

            // Col 3 (Away value)
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(rowH * 0.42f);
            canvas.drawText(statsData[r][2], padL + areaW * 0.80f, textRowY, textPaint);
        }

        // Bottom Left Back Button to Settings Grid
        float btnY = padT + areaH - 44f;
        RectF backBtn = new RectF(padL, btnY, padL + 150f, btnY + 44f);
        cardPaint.setColor(Color.parseColor("#007AFF"));
        canvas.drawRoundRect(backBtn, 22f, 22f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("❮  Back", backBtn.centerX(), backBtn.centerY() + 6f, textPaint);
    }

    /**
     * In-Overlay Squad Management & 4-3-3 Tactics Formation View (Match Records Style).
     */
    private void drawSquadManagementOverlay(Canvas canvas, int w, int h) {
        cardPaint.setColor(Color.argb(245, 10, 12, 22));
        canvas.drawRect(0, 0, w, h, cardPaint);

        float padX = Math.max(w * 0.05f, safeInsetLeft + 24f);
        float topY = Math.max(h * 0.04f, safeInsetTop + 14f);

        // Back Button
        RectF backBtn = new RectF(padX, topY, padX + 130f, topY + 42f);
        cardPaint.setColor(Color.parseColor("#007AFF"));
        canvas.drawRoundRect(backBtn, 21f, 21f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("❮  Back", backBtn.centerX(), backBtn.centerY() + 6f, textPaint);

        // Header Title
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(22f);
        canvas.drawText("SQUAD MANAGEMENT & FORMATION (4-3-3)", padX + 150f, topY + 30f, textPaint);

        float bodyTop = topY + 54f;
        float bodyH = h - bodyTop - Math.max(h * 0.04f, safeInsetBottom + 16f);
        float colW = (w - padX * 2f - 20f) / 2f;

        // LEFT: Tactical Pitch Board
        RectF pitchCard = new RectF(padX, bodyTop, padX + colW, bodyTop + bodyH);
        cardPaint.setColor(Color.parseColor("#1B5E20"));
        canvas.drawRoundRect(pitchCard, 14f, 14f, cardPaint);
        borderPaint.setColor(Color.parseColor("#00F0FF"));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(pitchCard, 14f, 14f, borderPaint);

        // White Pitch Markings
        borderPaint.setColor(Color.argb(160, 255, 255, 255));
        borderPaint.setStrokeWidth(2f);
        canvas.drawRoundRect(new RectF(pitchCard.left + 16f, pitchCard.top + 16f, pitchCard.right - 16f, pitchCard.bottom - 16f), 4f, 4f, borderPaint);
        canvas.drawLine(pitchCard.left + 16f, pitchCard.centerY(), pitchCard.right - 16f, pitchCard.centerY(), borderPaint);
        canvas.drawCircle(pitchCard.centerX(), pitchCard.centerY(), pitchCard.height() * 0.14f, borderPaint);

        // 11 Player Nodes in 4-3-3 formation
        float[][] pos433 = {
                { 0.50f, 0.90f }, // GK
                { 0.15f, 0.72f }, { 0.38f, 0.75f }, { 0.62f, 0.75f }, { 0.85f, 0.72f }, // LB, CB, CB, RB
                { 0.50f, 0.56f }, { 0.28f, 0.45f }, { 0.72f, 0.45f }, // DM, CM, CM
                { 0.20f, 0.24f }, { 0.50f, 0.18f }, { 0.80f, 0.24f }  // LW, CF, RW
        };
        String[] posNames = { "GK", "LB", "CB", "CB", "RB", "DM", "CM", "CM", "LW", "CF", "RW" };

        for (int p = 0; p < 11; p++) {
            float px = pitchCard.left + 16f + pos433[p][0] * (pitchCard.width() - 32f);
            float py = pitchCard.top + 16f + pos433[p][1] * (pitchCard.height() - 32f);

            cardPaint.setColor(Color.WHITE);
            canvas.drawCircle(px, py, 14f, cardPaint);
            borderPaint.setColor(Color.parseColor("#E53935"));
            borderPaint.setStrokeWidth(3f);
            canvas.drawCircle(px, py, 14f, borderPaint);

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.parseColor("#1B1B1B"));
            textPaint.setTextSize(10f);
            canvas.drawText(posNames[p], px, py + 3.5f, textPaint);
        }

        // RIGHT: Lineup Roster Card in Match Record Style
        RectF rosterCard = new RectF(padX + colW + 20f, bodyTop, padX + colW * 2f + 20f, bodyTop + bodyH);
        cardPaint.setColor(Color.parseColor("#16182B"));
        canvas.drawRoundRect(rosterCard, 14f, 14f, cardPaint);
        borderPaint.setColor(Color.parseColor("#00F0FF"));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(rosterCard, 14f, 14f, borderPaint);

        String[] lineupPlayers = {
                "1  M. NEUER (GK)", "19 A. DAVIES (LB)", "2  D. UPAMECANO (CB)", "4  M. DE LIGT (CB)",
                "40 N. MAZRAOUI (RB)", "6  J. KIMMICH (DM)", "8  L. GORETZKA (CM)", "42 J. MUSIALA (CM)",
                "11 K. COMAN (LW)", "9  H. KANE (CF)", "10 L. SANÉ (RW)"
        };
        float rowH = (bodyH - 24f) / 11f;
        for (int r = 0; r < 11; r++) {
            float ry = bodyTop + 12f + r * rowH;
            if (r % 2 == 1) {
                cardPaint.setColor(Color.argb(20, 255, 255, 255));
                canvas.drawRect(rosterCard.left + 10f, ry, rosterCard.right - 10f, ry + rowH, cardPaint);
            }
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(rowH * 0.45f);
            canvas.drawText(lineupPlayers[r], rosterCard.left + 24f, ry + rowH * 0.65f, textPaint);

            textPaint.setTextAlign(Paint.Align.RIGHT);
            textPaint.setColor(Color.parseColor("#00F0FF"));
            canvas.drawText("OVR " + (84 + (r * 7) % 7), rosterCard.right - 24f, ry + rowH * 0.65f, textPaint);
        }
    }

    /**
     * In-Overlay Touch Controls Guide (Match Records Style).
     */
    private void drawControlsListOverlay(Canvas canvas, int w, int h) {
        cardPaint.setColor(Color.argb(245, 10, 12, 22));
        canvas.drawRect(0, 0, w, h, cardPaint);

        float padX = Math.max(w * 0.05f, safeInsetLeft + 24f);
        float topY = Math.max(h * 0.04f, safeInsetTop + 14f);

        // Back Button
        RectF backBtn = new RectF(padX, topY, padX + 130f, topY + 42f);
        cardPaint.setColor(Color.parseColor("#007AFF"));
        canvas.drawRoundRect(backBtn, 21f, 21f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("❮  Back", backBtn.centerX(), backBtn.centerY() + 6f, textPaint);

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(22f);
        canvas.drawText("TOUCH CONTROLS GUIDE", padX + 150f, topY + 30f, textPaint);

        float bodyTop = topY + 54f;
        float bodyH = h - bodyTop - Math.max(h * 0.04f, safeInsetBottom + 16f);
        float colW = (w - padX * 2f - 20f) / 2f;

        // Attacking Controls Card
        RectF atkCard = new RectF(padX, bodyTop, padX + colW, bodyTop + bodyH);
        cardPaint.setColor(Color.parseColor("#16182B"));
        canvas.drawRoundRect(atkCard, 14f, 14f, cardPaint);
        borderPaint.setColor(Color.parseColor("#00F0FF"));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(atkCard, 14f, 14f, borderPaint);

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.parseColor("#00F0FF"));
        textPaint.setTextSize(18f);
        canvas.drawText("ATTACKING CONTROLS", atkCard.left + 24f, atkCard.top + 34f, textPaint);

        String[][] atkHelp = {
                { "Analog Stick", "360° Movement & Precision Dribbling" },
                { "Pass Button", "Direct Ground Pass towards teammate" },
                { "Through Button", "Through Ball into space" },
                { "Shoot Button", "Strike on goal (Hold for power)" },
                { "Dash Button", "Sprint burst in current direction" }
        };
        float rowH = (bodyH - 54f) / 5f;
        for (int i = 0; i < 5; i++) {
            float ry = atkCard.top + 48f + i * rowH;
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(16f);
            canvas.drawText(atkHelp[i][0], atkCard.left + 24f, ry + 22f, textPaint);
            subTextPaint.setTextAlign(Paint.Align.LEFT);
            subTextPaint.setColor(Color.parseColor("#9EA3C7"));
            subTextPaint.setTextSize(13f);
            canvas.drawText(atkHelp[i][1], atkCard.left + 24f, ry + 42f, subTextPaint);
        }

        // Defending Controls Card
        RectF defCard = new RectF(padX + colW + 20f, bodyTop, padX + colW * 2f + 20f, bodyTop + bodyH);
        cardPaint.setColor(Color.parseColor("#16182B"));
        canvas.drawRoundRect(defCard, 14f, 14f, cardPaint);
        borderPaint.setColor(Color.parseColor("#FF5252"));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(defCard, 14f, 14f, borderPaint);

        textPaint.setColor(Color.parseColor("#FF5252"));
        textPaint.setTextSize(18f);
        canvas.drawText("DEFENDING CONTROLS", defCard.left + 24f, defCard.top + 34f, textPaint);

        String[][] defHelp = {
                { "Analog Stick", "Track opponent runner & jockey" },
                { "Switch Button", "Switch active defender to ball" },
                { "Pressure Button", "Close down and press opponent" },
                { "Clear / Tackle", "Clear ball or execute standing tackle" },
                { "Dash Button", "Defensive sprint recovery" }
        };
        for (int i = 0; i < 5; i++) {
            float ry = defCard.top + 48f + i * rowH;
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(16f);
            canvas.drawText(defHelp[i][0], defCard.left + 24f, ry + 22f, textPaint);
            subTextPaint.setTextAlign(Paint.Align.LEFT);
            subTextPaint.setColor(Color.parseColor("#9EA3C7"));
            subTextPaint.setTextSize(13f);
            canvas.drawText(defHelp[i][1], defCard.left + 24f, ry + 42f, subTextPaint);
        }
    }

    /**
     * In-Overlay General & Visual Settings in Match Records Style:
     * Structured rows with dividers, equal-width badge buttons, no text clipping!
     */
    private void drawGeneralSettingsOverlay(Canvas canvas, int w, int h) {
        cardPaint.setColor(Color.argb(245, 10, 12, 22));
        canvas.drawRect(0, 0, w, h, cardPaint);

        float padX = Math.max(w * 0.05f, safeInsetLeft + 24f);
        float topY = Math.max(h * 0.04f, safeInsetTop + 14f);

        // Back Button
        RectF backBtn = new RectF(padX, topY, padX + 130f, topY + 42f);
        cardPaint.setColor(Color.parseColor("#007AFF"));
        canvas.drawRoundRect(backBtn, 21f, 21f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("❮  Back", backBtn.centerX(), backBtn.centerY() + 6f, textPaint);

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(22f);
        canvas.drawText("GENERAL & VISUAL SETTINGS", padX + 150f, topY + 30f, textPaint);

        float bodyTop = topY + 54f;
        float bodyH = h - bodyTop - Math.max(h * 0.04f, safeInsetBottom + 16f);
        float bodyW = w - padX * 2f;

        RectF card = new RectF(padX, bodyTop, padX + bodyW, bodyTop + bodyH);
        cardPaint.setColor(Color.parseColor("#16182B"));
        canvas.drawRoundRect(card, 16f, 16f, cardPaint);
        borderPaint.setColor(Color.parseColor("#00F0FF"));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(card, 16f, 16f, borderPaint);

        // Column partitions inside the card
        float colCtrlLeft = card.left + bodyW * 0.50f;
        float colCtrlRight = card.right - 28f;
        float colCtrlW = colCtrlRight - colCtrlLeft;

        float rowH = (bodyH - 24f) / 6f;

        // ── Row 0: Team 1 Kit ──
        float r0Y = bodyTop + 12f;
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText(team1Name + " Kit", card.left + 28f, r0Y + rowH * 0.42f, textPaint);
        subTextPaint.setTextAlign(Paint.Align.LEFT);
        subTextPaint.setColor(Color.parseColor("#9EA3C7"));
        subTextPaint.setTextSize(12.5f);
        canvas.drawText("Select active home kit", card.left + 28f, r0Y + rowH * 0.76f, subTextPaint);

        float kitBtnW = (colCtrlW - 14f) / 2f;
        float btnH = rowH * 0.70f;
        float btnPadY = r0Y + (rowH - btnH) * 0.5f;
        RectF kit1_0 = new RectF(colCtrlLeft, btnPadY, colCtrlLeft + kitBtnW, btnPadY + btnH);
        RectF kit1_1 = new RectF(colCtrlLeft + kitBtnW + 14f, btnPadY, colCtrlRight, btnPadY + btnH);

        cardPaint.setColor(selectedKit1 == 0 ? Color.parseColor("#00F0FF") : Color.parseColor("#262940"));
        canvas.drawRoundRect(kit1_0, 10f, 10f, cardPaint);
        cardPaint.setColor(selectedKit1 == 1 ? Color.parseColor("#00F0FF") : Color.parseColor("#262940"));
        canvas.drawRoundRect(kit1_1, 10f, 10f, cardPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(selectedKit1 == 0 ? Color.BLACK : Color.WHITE);
        textPaint.setTextSize(15f);
        canvas.drawText("Kit 01", kit1_0.centerX(), kit1_0.centerY() + 5f, textPaint);
        textPaint.setColor(selectedKit1 == 1 ? Color.BLACK : Color.WHITE);
        canvas.drawText("Kit 02", kit1_1.centerX(), kit1_1.centerY() + 5f, textPaint);

        // Divider
        borderPaint.setColor(Color.argb(30, 255, 255, 255));
        borderPaint.setStrokeWidth(1f);
        canvas.drawLine(card.left + 20f, r0Y + rowH, card.right - 20f, r0Y + rowH, borderPaint);

        // ── Row 1: Team 2 Kit ──
        float r1Y = r0Y + rowH;
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText(team2Name + " Kit", card.left + 28f, r1Y + rowH * 0.42f, textPaint);
        subTextPaint.setTextAlign(Paint.Align.LEFT);
        subTextPaint.setColor(Color.parseColor("#9EA3C7"));
        subTextPaint.setTextSize(12.5f);
        canvas.drawText("Select active away kit", card.left + 28f, r1Y + rowH * 0.76f, subTextPaint);

        float btnPadY1 = r1Y + (rowH - btnH) * 0.5f;
        RectF kit2_0 = new RectF(colCtrlLeft, btnPadY1, colCtrlLeft + kitBtnW, btnPadY1 + btnH);
        RectF kit2_1 = new RectF(colCtrlLeft + kitBtnW + 14f, btnPadY1, colCtrlRight, btnPadY1 + btnH);

        cardPaint.setColor(selectedKit2 == 0 ? Color.parseColor("#00F0FF") : Color.parseColor("#262940"));
        canvas.drawRoundRect(kit2_0, 10f, 10f, cardPaint);
        cardPaint.setColor(selectedKit2 == 1 ? Color.parseColor("#00F0FF") : Color.parseColor("#262940"));
        canvas.drawRoundRect(kit2_1, 10f, 10f, cardPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(selectedKit2 == 0 ? Color.BLACK : Color.WHITE);
        textPaint.setTextSize(15f);
        canvas.drawText("Kit 01", kit2_0.centerX(), kit2_0.centerY() + 5f, textPaint);
        textPaint.setColor(selectedKit2 == 1 ? Color.BLACK : Color.WHITE);
        canvas.drawText("Kit 02", kit2_1.centerX(), kit2_1.centerY() + 5f, textPaint);

        canvas.drawLine(card.left + 20f, r1Y + rowH, card.right - 20f, r1Y + rowH, borderPaint);

        // ── Row 2: Pitch Lighting ──
        float r2Y = r1Y + rowH;
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("Pitch Lighting & Sun", card.left + 28f, r2Y + rowH * 0.42f, textPaint);
        subTextPaint.setColor(Color.parseColor("#9EA3C7"));
        canvas.drawText("Randomize sun angle and shadows", card.left + 28f, r2Y + rowH * 0.76f, subTextPaint);

        float btnPadY2 = r2Y + (rowH - btnH) * 0.5f;
        RectF sunBtn = new RectF(colCtrlLeft, btnPadY2, colCtrlRight, btnPadY2 + btnH);
        cardPaint.setColor(Color.parseColor("#FFA800"));
        canvas.drawRoundRect(sunBtn, 10f, 10f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.parseColor("#141200"));
        textPaint.setTextSize(15f);
        canvas.drawText("☀  Randomize Sun Position", sunBtn.centerX(), sunBtn.centerY() + 5f, textPaint);

        canvas.drawLine(card.left + 20f, r2Y + rowH, card.right - 20f, r2Y + rowH, borderPaint);

        // ── Row 3: Pitch Radar HUD ──
        float r3Y = r2Y + rowH;
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("Pitch Radar HUD", card.left + 28f, r3Y + rowH * 0.42f, textPaint);
        subTextPaint.setColor(Color.parseColor("#9EA3C7"));
        canvas.drawText("Toggle 2D minimap on pitch", card.left + 28f, r3Y + rowH * 0.76f, subTextPaint);

        float btnPadY3 = r3Y + (rowH - btnH) * 0.5f;
        RectF radarBtn = new RectF(colCtrlLeft, btnPadY3, colCtrlLeft + 120f, btnPadY3 + btnH);
        cardPaint.setColor(settingRadarEnabled ? Color.parseColor("#00E676") : Color.parseColor("#424242"));
        canvas.drawRoundRect(radarBtn, 10f, 10f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(15f);
        canvas.drawText(settingRadarEnabled ? "ON" : "OFF", radarBtn.centerX(), radarBtn.centerY() + 5f, textPaint);

        canvas.drawLine(card.left + 20f, r3Y + rowH, card.right - 20f, r3Y + rowH, borderPaint);

        // ── Row 4: Overhead Player Names ──
        float r4Y = r3Y + rowH;
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("Overhead Indicators", card.left + 28f, r4Y + rowH * 0.42f, textPaint);
        subTextPaint.setColor(Color.parseColor("#9EA3C7"));
        canvas.drawText("Player name & cursor over active player", card.left + 28f, r4Y + rowH * 0.76f, subTextPaint);

        float btnPadY4 = r4Y + (rowH - btnH) * 0.5f;
        RectF namesBtn = new RectF(colCtrlLeft, btnPadY4, colCtrlLeft + 120f, btnPadY4 + btnH);
        cardPaint.setColor(settingPlayerNames ? Color.parseColor("#00E676") : Color.parseColor("#424242"));
        canvas.drawRoundRect(namesBtn, 10f, 10f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(15f);
        canvas.drawText(settingPlayerNames ? "ON" : "OFF", namesBtn.centerX(), namesBtn.centerY() + 5f, textPaint);

        canvas.drawLine(card.left + 20f, r4Y + rowH, card.right - 20f, r4Y + rowH, borderPaint);

        // ── Row 5: Actions: Match Records & Forfeit ──
        float r5Y = r4Y + rowH;
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("Match Management", card.left + 28f, r5Y + rowH * 0.42f, textPaint);
        subTextPaint.setColor(Color.parseColor("#9EA3C7"));
        canvas.drawText("Live telemetry and match exit", card.left + 28f, r5Y + rowH * 0.76f, subTextPaint);

        float btnPadY5 = r5Y + (rowH - btnH) * 0.5f;
        float actBtnW = (colCtrlW - 14f) / 2f;
        RectF recordsBtn = new RectF(colCtrlLeft, btnPadY5, colCtrlLeft + actBtnW, btnPadY5 + btnH);
        RectF forfeitBtn = new RectF(colCtrlLeft + actBtnW + 14f, btnPadY5, colCtrlRight, btnPadY5 + btnH);

        cardPaint.setColor(Color.parseColor("#1F2438"));
        canvas.drawRoundRect(recordsBtn, 10f, 10f, cardPaint);
        borderPaint.setColor(Color.parseColor("#00F0FF"));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(recordsBtn, 10f, 10f, borderPaint);

        cardPaint.setColor(Color.parseColor("#D32F2F"));
        canvas.drawRoundRect(forfeitBtn, 10f, 10f, cardPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.parseColor("#00F0FF"));
        textPaint.setTextSize(14f);
        canvas.drawText("Match Records", recordsBtn.centerX(), recordsBtn.centerY() + 5f, textPaint);

        textPaint.setColor(Color.WHITE);
        canvas.drawText("Forfeit Match", forfeitBtn.centerX(), forfeitBtn.centerY() + 5f, textPaint);
    }

    /**
     * In-Overlay Camera Settings (Match Records Style).
     */
    private void drawCameraSettingsOverlay(Canvas canvas, int w, int h) {
        cardPaint.setColor(Color.argb(245, 10, 12, 22));
        canvas.drawRect(0, 0, w, h, cardPaint);

        float padX = Math.max(w * 0.05f, safeInsetLeft + 24f);
        float topY = Math.max(h * 0.04f, safeInsetTop + 14f);

        // Back Button
        RectF backBtn = new RectF(padX, topY, padX + 130f, topY + 42f);
        cardPaint.setColor(Color.parseColor("#007AFF"));
        canvas.drawRoundRect(backBtn, 21f, 21f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("❮  Back", backBtn.centerX(), backBtn.centerY() + 6f, textPaint);

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(22f);
        canvas.drawText("CAMERA SETTINGS", padX + 150f, topY + 30f, textPaint);

        float bodyTop = topY + 54f;
        float bodyH = h - bodyTop - Math.max(h * 0.04f, safeInsetBottom + 16f);
        float bodyW = w - padX * 2f;

        RectF card = new RectF(padX, bodyTop, padX + bodyW, bodyTop + bodyH);
        cardPaint.setColor(Color.parseColor("#16182B"));
        canvas.drawRoundRect(card, 16f, 16f, cardPaint);
        borderPaint.setColor(Color.parseColor("#00F0FF"));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(card, 16f, 16f, borderPaint);

        String[] sliderTitles = {
                "CAMERA HEIGHT",
                "CAMERA ZOOM / FOV",
                "CAMERA ANGLE"
        };
        String[] displayVals = {
                Math.round((settingCamHeight - 0.1f) / 1.4f * 100) + "%",
                Math.round((settingCamZoom - 0.2f) / 1.3f * 100) + "%",
                Math.round(settingCamAngle * 100) + "%"
        };

        float trackLeft = card.left + bodyW * 0.42f;
        float trackRight = card.right - 90f;
        float trackW = trackRight - trackLeft;
        float rowH = (bodyH - 24f) / 4f;

        for (int i = 0; i < 3; i++) {
            float ry = bodyTop + 12f + i * rowH;
            float cy = ry + rowH * 0.5f;

            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(17f);
            canvas.drawText(sliderTitles[i], card.left + 28f, cy + 6f, textPaint);

            // Track background
            borderPaint.setColor(Color.parseColor("#2E334D"));
            borderPaint.setStrokeWidth(8f);
            canvas.drawLine(trackLeft, cy, trackRight, cy, borderPaint);

            // Active Track
            float frac = 0.5f;
            if (i == 0) frac = (settingCamHeight - 0.1f) / 1.4f;
            else if (i == 1) frac = (settingCamZoom - 0.2f) / 1.3f;
            else if (i == 2) frac = settingCamAngle;
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

            // Value text
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(Color.parseColor("#00F0FF"));
            textPaint.setTextSize(16f);
            canvas.drawText(displayVals[i], trackRight + 16f, cy + 6f, textPaint);

            // Divider
            borderPaint.setColor(Color.argb(30, 255, 255, 255));
            borderPaint.setStrokeWidth(1f);
            canvas.drawLine(card.left + 20f, ry + rowH, card.right - 20f, ry + rowH, borderPaint);
        }

        // Reset to Defaults Button
        float r3Y = bodyTop + 12f + 3f * rowH;
        float resetBtnW = 200f;
        RectF resetBtn = new RectF(trackLeft, r3Y + rowH * 0.20f, trackLeft + resetBtnW, r3Y + rowH * 0.80f);
        cardPaint.setColor(Color.parseColor("#262940"));
        canvas.drawRoundRect(resetBtn, 10f, 10f, cardPaint);
        borderPaint.setColor(Color.parseColor("#00F0FF"));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(resetBtn, 10f, 10f, borderPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(15f);
        canvas.drawText("Reset Defaults", resetBtn.centerX(), resetBtn.centerY() + 5f, textPaint);
    }

    /**
     * In-Overlay Sound Settings (Match Records Style).
     */
    private void drawSoundSettingsOverlay(Canvas canvas, int w, int h) {
        cardPaint.setColor(Color.argb(245, 10, 12, 22));
        canvas.drawRect(0, 0, w, h, cardPaint);

        float padX = Math.max(w * 0.05f, safeInsetLeft + 24f);
        float topY = Math.max(h * 0.04f, safeInsetTop + 14f);

        // Back Button
        RectF backBtn = new RectF(padX, topY, padX + 130f, topY + 42f);
        cardPaint.setColor(Color.parseColor("#007AFF"));
        canvas.drawRoundRect(backBtn, 21f, 21f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("❮  Back", backBtn.centerX(), backBtn.centerY() + 6f, textPaint);

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(22f);
        canvas.drawText("SOUND SETTINGS", padX + 150f, topY + 30f, textPaint);

        float bodyTop = topY + 54f;
        float bodyH = h - bodyTop - Math.max(h * 0.04f, safeInsetBottom + 16f);
        float bodyW = w - padX * 2f;

        RectF card = new RectF(padX, bodyTop, padX + bodyW, bodyTop + bodyH);
        cardPaint.setColor(Color.parseColor("#16182B"));
        canvas.drawRoundRect(card, 16f, 16f, cardPaint);
        borderPaint.setColor(Color.parseColor("#00F0FF"));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(card, 16f, 16f, borderPaint);

        float trackLeft = card.left + bodyW * 0.42f;
        float trackRight = card.right - 90f;
        float trackW = trackRight - trackLeft;
        float rowH = (bodyH - 24f) / 3f;

        // Row 0: Volume Slider
        float r0Y = bodyTop + 12f;
        float cy = r0Y + rowH * 0.5f;

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("AUDIO & CROWD SFX", card.left + 28f, cy + 6f, textPaint);

        // Track background
        borderPaint.setColor(Color.parseColor("#2E334D"));
        borderPaint.setStrokeWidth(8f);
        canvas.drawLine(trackLeft, cy, trackRight, cy, borderPaint);

        // Active Track
        float frac = Math.max(0f, Math.min(1f, settingAudioVolume));
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
        textPaint.setColor(Color.parseColor("#00F0FF"));
        textPaint.setTextSize(16f);
        canvas.drawText(Math.round(frac * 100) + "%", trackRight + 16f, cy + 6f, textPaint);

        // Divider
        borderPaint.setColor(Color.argb(30, 255, 255, 255));
        borderPaint.setStrokeWidth(1f);
        canvas.drawLine(card.left + 20f, r0Y + rowH, card.right - 20f, r0Y + rowH, borderPaint);

        // Row 1: Mute Toggle Button
        float r1Y = r0Y + rowH;
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("MASTER MUTE", card.left + 28f, r1Y + rowH * 0.5f + 6f, textPaint);

        RectF muteBtn = new RectF(trackLeft, r1Y + rowH * 0.22f, trackLeft + 160f, r1Y + rowH * 0.78f);
        cardPaint.setColor((settingAudioVolume < 0.01f) ? Color.parseColor("#E53935") : Color.parseColor("#262940"));
        canvas.drawRoundRect(muteBtn, 10f, 10f, cardPaint);
        borderPaint.setColor(Color.parseColor("#00F0FF"));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(muteBtn, 10f, 10f, borderPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(15f);
        canvas.drawText((settingAudioVolume < 0.01f) ? "Muted" : "Mute Sound", muteBtn.centerX(), muteBtn.centerY() + 5f, textPaint);
    }

    /**
     * Replay Playback Overlay (Interactive step HUD).
     */
    private void drawReplayPlaybackOverlay(Canvas canvas, int w, int h) {
        cardPaint.setColor(Color.argb(120, 0, 0, 0));
        canvas.drawRect(0, 0, w, 56f, cardPaint);
        canvas.drawRect(0, h - 80f, w, h, cardPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.parseColor("#FFA800"));
        textPaint.setTextSize(20f);
        canvas.drawText("● REPLAY PLAYBACK", w * 0.5f, 38f, textPaint);

        float barW = Math.min(w * 0.60f, 520f);
        float barH = 56f;
        float barX = (w - barW) * 0.5f;
        float barY = h - Math.max(h * 0.06f, safeInsetBottom + 20f) - barH;

        RectF barRect = new RectF(barX, barY, barX + barW, barY + barH);
        cardPaint.setColor(Color.argb(220, 16, 20, 36));
        canvas.drawRoundRect(barRect, 14f, 14f, cardPaint);
        borderPaint.setColor(Color.parseColor("#00F0FF"));
        borderPaint.setStrokeWidth(2f);
        canvas.drawRoundRect(barRect, 14f, 14f, borderPaint);

        float btnW = barW / 4f;

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("<< 1s", barX + btnW * 0.5f, barY + barH * 0.62f, textPaint);

        textPaint.setColor(Color.parseColor("#00F0FF"));
        canvas.drawText(isReplayPlaying ? "❚❚ Pause" : "▶ Play", barX + btnW * 1.5f, barY + barH * 0.62f, textPaint);

        textPaint.setColor(Color.WHITE);
        canvas.drawText("1s >>", barX + btnW * 2.5f, barY + barH * 0.62f, textPaint);

        textPaint.setColor(Color.parseColor("#FF5252"));
        canvas.drawText("✕ Close", barX + btnW * 3.5f, barY + barH * 0.62f, textPaint);
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

    private void drawTeamCrest(Canvas canvas, float cx, float cy, float radius, int teamId, String name, int c1, int c2) {
        Bitmap logo = (teamId == 0) ? team1LogoBitmap : team2LogoBitmap;
        if (logo != null && !logo.isRecycled()) {
            canvas.save();
            Path circlePath = new Path();
            circlePath.addCircle(cx, cy, radius, Path.Direction.CW);
            canvas.clipPath(circlePath);

            cardPaint.setColor(Color.WHITE);
            canvas.drawCircle(cx, cy, radius, cardPaint);

            Rect src = new Rect(0, 0, logo.getWidth(), logo.getHeight());
            RectF dst = new RectF(cx - radius * 0.90f, cy - radius * 0.90f, cx + radius * 0.90f, cy + radius * 0.90f);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(logo, src, dst, p);
            canvas.restore();

            borderPaint.setColor(Color.WHITE);
            borderPaint.setStrokeWidth(2.5f);
            canvas.drawCircle(cx, cy, radius, borderPaint);
            return;
        }

        // Vector shield fallback
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

    private void drawTeamCrest(Canvas canvas, float cx, float cy, float radius, String name, int c1, int c2) {
        int teamId = 0;
        if (team2Name != null && team2Name.equalsIgnoreCase(name)) {
            teamId = 1;
        }
        drawTeamCrest(canvas, cx, cy, radius, teamId, name, c1, c2);
    }

    private void drawSettingsCardIcon(Canvas canvas, int index, float cx, float cy, float radius) {
        Paint pBlue = new Paint(Paint.ANTI_ALIAS_FLAG);
        pBlue.setColor(Color.parseColor("#1976D2"));
        pBlue.setStyle(Paint.Style.STROKE);
        pBlue.setStrokeWidth(2.8f);

        Paint pGreen = new Paint(Paint.ANTI_ALIAS_FLAG);
        pGreen.setColor(Color.parseColor("#00C853"));
        pGreen.setStyle(Paint.Style.FILL);

        if (index == 0) { // Squad Management: Pitch board with green map pin
            RectF pitch = new RectF(cx - radius * 0.9f, cy - radius * 0.7f, cx + radius * 0.9f, cy + radius * 0.7f);
            canvas.drawRoundRect(pitch, 4f, 4f, pBlue);
            canvas.drawLine(cx, pitch.top, cx, pitch.bottom, pBlue);
            canvas.drawCircle(cx, cy, radius * 0.32f, pBlue);

            // Green Pin marker at bottom right
            float pinX = pitch.right - 4f;
            float pinY = pitch.bottom;
            canvas.drawCircle(pinX, pinY - radius * 0.22f, radius * 0.22f, pGreen);
            Path pinTip = new Path();
            pinTip.moveTo(pinX - radius * 0.18f, pinY - radius * 0.16f);
            pinTip.lineTo(pinX + radius * 0.18f, pinY - radius * 0.16f);
            pinTip.lineTo(pinX, pinY + radius * 0.16f);
            pinTip.close();
            canvas.drawPath(pinTip, pGreen);
            pGreen.setColor(Color.WHITE);
            canvas.drawCircle(pinX, pinY - radius * 0.22f, radius * 0.08f, pGreen);

        } else if (index == 1) { // Controls List: Device with [?] badge
            RectF tab = new RectF(cx - radius * 0.95f, cy - radius * 0.7f, cx + radius * 0.95f, cy + radius * 0.7f);
            canvas.drawRoundRect(tab, 6f, 6f, pBlue);

            pBlue.setStyle(Paint.Style.FILL);
            RectF badge = new RectF(cx - radius * 0.38f, cy - radius * 0.55f, cx + radius * 0.38f, cy + radius * 0.25f);
            canvas.drawRoundRect(badge, 4f, 4f, pBlue);

            Paint textP = new Paint(Paint.ANTI_ALIAS_FLAG);
            textP.setTextAlign(Paint.Align.CENTER);
            textP.setColor(Color.WHITE);
            textP.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            textP.setTextSize(radius * 0.55f);
            canvas.drawText("?", cx, cy + radius * 0.06f, textP);

        } else if (index == 2) { // General Settings: Mechanical Gear / Cog
            pBlue.setStyle(Paint.Style.STROKE);
            pBlue.setStrokeWidth(3.5f);
            canvas.drawCircle(cx, cy, radius * 0.48f, pBlue);

            pBlue.setStyle(Paint.Style.FILL);
            for (int i = 0; i < 8; i++) {
                double a = Math.toRadians(i * 45);
                float tx = cx + (float)(Math.cos(a) * radius * 0.68f);
                float ty = cy + (float)(Math.sin(a) * radius * 0.68f);
                canvas.drawCircle(tx, ty, radius * 0.16f, pBlue);
            }
            pBlue.setColor(Color.WHITE);
            canvas.drawCircle(cx, cy, radius * 0.24f, pBlue);

        } else if (index == 3) { // Camera Settings: Video Camera
            pBlue.setStyle(Paint.Style.FILL);
            RectF body = new RectF(cx - radius * 0.9f, cy - radius * 0.55f, cx + radius * 0.35f, cy + radius * 0.55f);
            canvas.drawRoundRect(body, 6f, 6f, pBlue);

            Path lens = new Path();
            lens.moveTo(cx + radius * 0.40f, cy - radius * 0.26f);
            lens.lineTo(cx + radius * 0.95f, cy - radius * 0.60f);
            lens.lineTo(cx + radius * 0.95f, cy + radius * 0.60f);
            lens.lineTo(cx + radius * 0.40f, cy + radius * 0.26f);
            lens.close();
            canvas.drawPath(lens, pBlue);

        } else if (index == 4) { // Sound Settings: Speaker with green sound waves
            pBlue.setStyle(Paint.Style.FILL);
            Path spk = new Path();
            spk.moveTo(cx - radius * 0.85f, cy - radius * 0.28f);
            spk.lineTo(cx - radius * 0.40f, cy - radius * 0.28f);
            spk.lineTo(cx + radius * 0.05f, cy - radius * 0.68f);
            spk.lineTo(cx + radius * 0.05f, cy + radius * 0.68f);
            spk.lineTo(cx - radius * 0.40f, cy + radius * 0.28f);
            spk.lineTo(cx - radius * 0.85f, cy + radius * 0.28f);
            spk.close();
            canvas.drawPath(spk, pBlue);

            pGreen.setStyle(Paint.Style.STROKE);
            pGreen.setStrokeWidth(3.0f);
            pGreen.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawArc(new RectF(cx - radius * 0.1f, cy - radius * 0.45f, cx + radius * 0.55f, cy + radius * 0.45f), -45f, 90f, false, pGreen);
            canvas.drawArc(new RectF(cx + radius * 0.1f, cy - radius * 0.72f, cx + radius * 0.85f, cy + radius * 0.72f), -45f, 90f, false, pGreen);
        }
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
                liveStats.poss1 = 50;
                liveStats.poss2 = 50;
            }
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
            // 2.A: SETTINGS_GRID TOUCHES (PES Mobile 5-Card Grid)
            if (pauseSubView == PauseSubView.SETTINGS_GRID) {
                float padL = Math.max(getWidth() * 0.08f, safeInsetLeft + 28f);
                float padR = Math.max(getWidth() * 0.08f, safeInsetRight + 28f);
                float topY = Math.max(getHeight() * 0.04f, safeInsetTop + 10f);

                float scoreCx = getWidth() * 0.50f;
                float crestR = Math.min(getHeight() * 0.085f, 44f);
                float crestCy = topY + crestR + 2f;
                float t1NameY = crestCy + crestR + 18f;

                float gridTop = t1NameY + 28f;
                float bottomBarH = 58f;
                float gridBottom = getHeight() - Math.max(getHeight() * 0.04f, safeInsetBottom + 12f) - bottomBarH;
                float gridW = getWidth() - padL - padR;
                float gridH = gridBottom - gridTop;

                float gapX = gridW * 0.032f;
                float gapY = gridH * 0.08f;
                float cardW = (gridW - 2f * gapX) / 3f;
                float cardH = (gridH - gapY) / 2f;

                // Resume button at bottom left
                float btnY = getHeight() - Math.max(getHeight() * 0.04f, safeInsetBottom + 12f) - 48f;
                RectF resumeBtn = new RectF(padL, btnY, padL + 160f, btnY + 48f);

                // Center score area (tap to open Match Records)
                RectF scoreArea = new RectF(scoreCx - 140f, topY, scoreCx + 140f, gridTop);

                RectF card0 = new RectF(padL + 0 * (cardW + gapX), gridTop, padL + 0 * (cardW + gapX) + cardW, gridTop + cardH);
                RectF card1 = new RectF(padL + 1 * (cardW + gapX), gridTop, padL + 1 * (cardW + gapX) + cardW, gridTop + cardH);
                RectF card2 = new RectF(padL + 2 * (cardW + gapX), gridTop, padL + 2 * (cardW + gapX) + cardW, gridTop + cardH);
                RectF card3 = new RectF(padL + 0 * (cardW + gapX), gridTop + cardH + gapY, padL + 0 * (cardW + gapX) + cardW, gridTop + 2 * cardH + gapY);
                RectF card4 = new RectF(padL + 1 * (cardW + gapX), gridTop + cardH + gapY, padL + 1 * (cardW + gapX) + cardW, gridTop + 2 * cardH + gapY);

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        if (resumeBtn.contains(x, y)) {
                            // Resume match
                            isPaused = false;
                            try {
                                SDLActivity.setMatchPaused(false);
                            } catch (Throwable ignored) {}
                            invalidate();
                            return true;
                        }
                        if (scoreArea.contains(x, y)) {
                            pauseSubView = PauseSubView.OVERVIEW;
                            refreshMatchStats();
                            invalidate();
                            return true;
                        }
                        if (card0.contains(x, y)) pressedSettingsCardIndex = 0;
                        else if (card1.contains(x, y)) pressedSettingsCardIndex = 1;
                        else if (card2.contains(x, y)) pressedSettingsCardIndex = 2;
                        else if (card3.contains(x, y)) pressedSettingsCardIndex = 3;
                        else if (card4.contains(x, y)) pressedSettingsCardIndex = 4;
                        invalidate();
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        if (pressedSettingsCardIndex != -1) {
                            int idx = pressedSettingsCardIndex;
                            pressedSettingsCardIndex = -1;
                            if (idx == 0) pauseSubView = PauseSubView.SQUAD_MANAGEMENT;
                            else if (idx == 1) pauseSubView = PauseSubView.CONTROLS_LIST;
                            else if (idx == 2) pauseSubView = PauseSubView.GENERAL_SETTINGS;
                            else if (idx == 3) pauseSubView = PauseSubView.CAMERA_SETTINGS;
                            else if (idx == 4) pauseSubView = PauseSubView.SOUND_SETTINGS;
                            invalidate();
                            return true;
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        pressedSettingsCardIndex = -1;
                        invalidate();
                        break;
                }
                return true;
            }

            // 2.B: SQUAD_MANAGEMENT & CONTROLS_LIST TOUCHES (Back button)
            if (pauseSubView == PauseSubView.SQUAD_MANAGEMENT || pauseSubView == PauseSubView.CONTROLS_LIST) {
                float sPadX = Math.max(getWidth() * 0.05f, safeInsetLeft + 24f);
                float sTopY = Math.max(getHeight() * 0.04f, safeInsetTop + 14f);
                RectF backBtn = new RectF(sPadX, sTopY, sPadX + 130f, sTopY + 42f);
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                    if (backBtn.contains(x, y)) {
                        pauseSubView = PauseSubView.SETTINGS_GRID;
                        invalidate();
                        return true;
                    }
                }
                return true;
            }

            // 2.C: GENERAL_SETTINGS TOUCHES
            if (pauseSubView == PauseSubView.GENERAL_SETTINGS) {
                float gPadX = Math.max(getWidth() * 0.05f, safeInsetLeft + 24f);
                float gTopY = Math.max(getHeight() * 0.04f, safeInsetTop + 14f);
                RectF backBtn = new RectF(gPadX, gTopY, gPadX + 130f, gTopY + 42f);

                float bodyTop = gTopY + 54f;
                float bodyH = getHeight() - bodyTop - Math.max(getHeight() * 0.04f, safeInsetBottom + 16f);
                float bodyW = getWidth() - gPadX * 2f;
                float colCtrlLeft = gPadX + bodyW * 0.50f;
                float colCtrlRight = (gPadX + bodyW) - 28f;
                float colCtrlW = colCtrlRight - colCtrlLeft;

                float rowH = (bodyH - 24f) / 6f;
                float kitBtnW = (colCtrlW - 14f) / 2f;
                float btnH = rowH * 0.70f;

                // Row 0: Team 1 Kit
                float r0Y = bodyTop + 12f;
                float btnPadY0 = r0Y + (rowH - btnH) * 0.5f;
                RectF kit1_0 = new RectF(colCtrlLeft, btnPadY0, colCtrlLeft + kitBtnW, btnPadY0 + btnH);
                RectF kit1_1 = new RectF(colCtrlLeft + kitBtnW + 14f, btnPadY0, colCtrlRight, btnPadY0 + btnH);

                // Row 1: Team 2 Kit
                float r1Y = r0Y + rowH;
                float btnPadY1 = r1Y + (rowH - btnH) * 0.5f;
                RectF kit2_0 = new RectF(colCtrlLeft, btnPadY1, colCtrlLeft + kitBtnW, btnPadY1 + btnH);
                RectF kit2_1 = new RectF(colCtrlLeft + kitBtnW + 14f, btnPadY1, colCtrlRight, btnPadY1 + btnH);

                // Row 2: Sun / Lighting
                float r2Y = r1Y + rowH;
                float btnPadY2 = r2Y + (rowH - btnH) * 0.5f;
                RectF sunBtn = new RectF(colCtrlLeft, btnPadY2, colCtrlRight, btnPadY2 + btnH);

                // Row 3: Radar
                float r3Y = r2Y + rowH;
                float btnPadY3 = r3Y + (rowH - btnH) * 0.5f;
                RectF radarBtn = new RectF(colCtrlLeft, btnPadY3, colCtrlLeft + 120f, btnPadY3 + btnH);

                // Row 4: Overhead Names
                float r4Y = r3Y + rowH;
                float btnPadY4 = r4Y + (rowH - btnH) * 0.5f;
                RectF namesBtn = new RectF(colCtrlLeft, btnPadY4, colCtrlLeft + 120f, btnPadY4 + btnH);

                // Row 5: Actions: Match Records & Forfeit Match
                float r5Y = r4Y + rowH;
                float btnPadY5 = r5Y + (rowH - btnH) * 0.5f;
                float actBtnW = (colCtrlW - 14f) / 2f;
                RectF recordsBtn = new RectF(colCtrlLeft, btnPadY5, colCtrlLeft + actBtnW, btnPadY5 + btnH);
                RectF forfeitBtn = new RectF(colCtrlLeft + actBtnW + 14f, btnPadY5, colCtrlRight, btnPadY5 + btnH);

                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                    if (backBtn.contains(x, y)) {
                        pauseSubView = PauseSubView.SETTINGS_GRID;
                        saveSettingsToPrefs();
                        invalidate();
                        return true;
                    } else if (kit1_0.contains(x, y)) {
                        selectedKit1 = 0;
                        try { SDLActivity.setTeamKit(0, 1); } catch (Throwable ignored) {}
                        invalidate();
                        return true;
                    } else if (kit1_1.contains(x, y)) {
                        selectedKit1 = 1;
                        try { SDLActivity.setTeamKit(0, 2); } catch (Throwable ignored) {}
                        invalidate();
                        return true;
                    } else if (kit2_0.contains(x, y)) {
                        selectedKit2 = 0;
                        try { SDLActivity.setTeamKit(1, 1); } catch (Throwable ignored) {}
                        invalidate();
                        return true;
                    } else if (kit2_1.contains(x, y)) {
                        selectedKit2 = 1;
                        try { SDLActivity.setTeamKit(1, 2); } catch (Throwable ignored) {}
                        invalidate();
                        return true;
                    } else if (sunBtn.contains(x, y)) {
                        try { SDLActivity.randomizeSunLighting(); } catch (Throwable ignored) {}
                        invalidate();
                        return true;
                    } else if (radarBtn.contains(x, y)) {
                        settingRadarEnabled = !settingRadarEnabled;
                        saveSettingsToPrefs();
                        invalidate();
                        return true;
                    } else if (namesBtn.contains(x, y)) {
                        settingPlayerNames = !settingPlayerNames;
                        saveSettingsToPrefs();
                        invalidate();
                        return true;
                    } else if (recordsBtn.contains(x, y)) {
                        pauseSubView = PauseSubView.OVERVIEW;
                        refreshMatchStats();
                        invalidate();
                        return true;
                    } else if (forfeitBtn.contains(x, y)) {
                        pauseSubView = PauseSubView.CONFIRM_QUIT;
                        invalidate();
                        return true;
                    }
                }
                return true;
            }

            // 2.D: CAMERA_SETTINGS TOUCHES
            if (pauseSubView == PauseSubView.CAMERA_SETTINGS) {
                float cPadX = Math.max(getWidth() * 0.05f, safeInsetLeft + 24f);
                float cTopY = Math.max(getHeight() * 0.04f, safeInsetTop + 14f);
                RectF backBtn = new RectF(cPadX, cTopY, cPadX + 130f, cTopY + 42f);

                float bodyTop = cTopY + 54f;
                float bodyH = getHeight() - bodyTop - Math.max(getHeight() * 0.04f, safeInsetBottom + 16f);
                float bodyW = getWidth() - cPadX * 2f;
                float trackLeft = cPadX + bodyW * 0.42f;
                float trackRight = cPadX + bodyW - 90f;
                float trackW = trackRight - trackLeft;
                float rowH = (bodyH - 24f) / 4f;

                float r3Y = bodyTop + 12f + 3f * rowH;
                float resetBtnW = 200f;
                RectF resetBtn = new RectF(trackLeft, r3Y + rowH * 0.20f, trackLeft + resetBtnW, r3Y + rowH * 0.80f);

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        if (backBtn.contains(x, y)) {
                            pauseSubView = PauseSubView.SETTINGS_GRID;
                            saveSettingsToPrefs();
                            invalidate();
                            return true;
                        }
                        if (resetBtn.contains(x, y)) {
                            settingCamHeight = 0.40f;
                            settingCamZoom = 0.75f;
                            settingCamAngle = 0.50f;
                            applyCameraAndAudioSettings();
                            saveSettingsToPrefs();
                            invalidate();
                            return true;
                        }
                        for (int s = 0; s < 3; s++) {
                            float ry = bodyTop + 12f + s * rowH;
                            float cy = ry + rowH * 0.5f;
                            if (y >= cy - 25f && y <= cy + 25f && x >= trackLeft - 30f && x <= trackRight + 30f) {
                                draggingSlider = s;
                                updateSliderValue(s, (x - trackLeft) / trackW);
                                invalidate();
                                return true;
                            }
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (draggingSlider != -1 && draggingSlider < 3) {
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

            // 2.E: SOUND_SETTINGS TOUCHES
            if (pauseSubView == PauseSubView.SOUND_SETTINGS) {
                float sndPadX = Math.max(getWidth() * 0.05f, safeInsetLeft + 24f);
                float sndTopY = Math.max(getHeight() * 0.04f, safeInsetTop + 14f);
                RectF backBtn = new RectF(sndPadX, sndTopY, sndPadX + 130f, sndTopY + 42f);

                float bodyTop = sndTopY + 54f;
                float bodyH = getHeight() - bodyTop - Math.max(getHeight() * 0.04f, safeInsetBottom + 16f);
                float bodyW = getWidth() - sndPadX * 2f;
                float trackLeft = sndPadX + bodyW * 0.42f;
                float trackRight = sndPadX + bodyW - 90f;
                float trackW = trackRight - trackLeft;
                float rowH = (bodyH - 24f) / 3f;

                float r0Y = bodyTop + 12f;
                float cy = r0Y + rowH * 0.5f;
                float r1Y = r0Y + rowH;
                RectF muteBtn = new RectF(trackLeft, r1Y + rowH * 0.22f, trackLeft + 160f, r1Y + rowH * 0.78f);

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        if (backBtn.contains(x, y)) {
                            pauseSubView = PauseSubView.SETTINGS_GRID;
                            saveSettingsToPrefs();
                            invalidate();
                            return true;
                        }
                        if (muteBtn.contains(x, y)) {
                            if (settingAudioVolume > 0.01f) {
                                settingAudioVolume = 0.0f;
                            } else {
                                settingAudioVolume = 0.80f;
                            }
                            applyCameraAndAudioSettings();
                            saveSettingsToPrefs();
                            invalidate();
                            return true;
                        }
                        if (y >= cy - 25f && y <= cy + 25f && x >= trackLeft - 30f && x <= trackRight + 30f) {
                            draggingSlider = 3;
                            updateSliderValue(3, (x - trackLeft) / trackW);
                            invalidate();
                            return true;
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (draggingSlider == 3) {
                            updateSliderValue(3, (x - trackLeft) / trackW);
                            invalidate();
                            return true;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        if (draggingSlider == 3) {
                            updateSliderValue(3, (x - trackLeft) / trackW);
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

            // 2.F: REPLAY_PLAYBACK TOUCHES
            if (pauseSubView == PauseSubView.REPLAY_PLAYBACK) {
                float barW = Math.min(getWidth() * 0.60f, 520f);
                float barH = 56f;
                float barX = (getWidth() - barW) * 0.5f;
                float barY = getHeight() - Math.max(getHeight() * 0.06f, safeInsetBottom + 20f) - barH;
                float btnW = barW / 4f;

                RectF rewBtn = new RectF(barX, barY, barX + btnW, barY + barH);
                RectF playBtn = new RectF(barX + btnW, barY, barX + btnW * 2f, barY + barH);
                RectF fwdBtn = new RectF(barX + btnW * 2f, barY, barX + btnW * 3f, barY + barH);
                RectF closeBtn = new RectF(barX + btnW * 3f, barY, barX + barW, barY + barH);

                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                    if (rewBtn.contains(x, y)) {
                        try { SDLActivity.stepReplay(-1000); } catch (Throwable ignored) {}
                        invalidate();
                        return true;
                    } else if (playBtn.contains(x, y)) {
                        isReplayPlaying = !isReplayPlaying;
                        if (isReplayPlaying) {
                            removeCallbacks(replayStepper);
                            post(replayStepper);
                        }
                        invalidate();
                        return true;
                    } else if (fwdBtn.contains(x, y)) {
                        try { SDLActivity.stepReplay(1000); } catch (Throwable ignored) {}
                        invalidate();
                        return true;
                    } else if (closeBtn.contains(x, y)) {
                        isReplayPlaying = false;
                        removeCallbacks(replayStepper);
                        try { SDLActivity.stopReplay(); } catch (Throwable ignored) {}
                        pauseSubView = PauseSubView.SETTINGS_GRID;
                        invalidate();
                        return true;
                    }
                }
                return true;
            }

            // 2.G: CONFIRM_QUIT TOUCHES
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

                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                    if (cancelBtn.contains(x, y)) {
                        pauseSubView = PauseSubView.SETTINGS_GRID;
                        invalidate();
                        return true;
                    } else if (quitBtn.contains(x, y)) {
                        isPaused = false;
                        pauseSubView = PauseSubView.SETTINGS_GRID;
                        try {
                            SDLActivity.quitMatchToMainMenu();
                        } catch (Throwable ignored) {}
                        return true;
                    }
                }
                return true;
            }

            // 2.H: OVERVIEW (PES MATCH RECORDS / TELEMETRY) TOUCHES
            if (pauseSubView == PauseSubView.OVERVIEW) {
                float padL = Math.max(getWidth() * 0.035f, safeInsetLeft + 20f);
                float padR = Math.max(getWidth() * 0.035f, safeInsetRight + 20f);
                float padT = Math.max(getHeight() * 0.035f, safeInsetTop + 10f);
                float padB = Math.max(getHeight() * 0.035f, safeInsetBottom + 10f);
                float areaW = getWidth() - padL - padR;
                float areaH = getHeight() - padT - padB;

                float btnY = padT + areaH - 44f;
                RectF backBtn = new RectF(padL, btnY, padL + 150f, btnY + 44f);

                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                    if (backBtn.contains(x, y)) {
                        pauseSubView = PauseSubView.SETTINGS_GRID;
                        invalidate();
                        return true;
                    }
                }
                return true;
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
                        pauseSubView = PauseSubView.SETTINGS_GRID;
                        try {
                            SDLActivity.setMatchPaused(true);
                        } catch (Throwable ignored) {}
                        loadTeamData();
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
