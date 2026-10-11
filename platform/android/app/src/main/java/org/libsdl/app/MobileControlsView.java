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
            case SETTINGS_GRID:
                drawSettingsGrid(canvas, w, h);
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
            case OVERVIEW:
            default:
                drawPesPauseOverview(canvas, w, h);
                break;
        }
    }

    /**
     * PES Mobile Pause Screen Overview:
     * - Left Column (22%): Vertical action buttons (Yellow Resume, Game Plan, Records, Settings, Highlights, Top Menu)
     * - Right Column (76%): Full-width Scoreboard Header + 10-Row Match Stats Comparison Table
     * Note: Player featured card and substitutions removed per request.
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

        float gap = areaW * 0.016f;
        float col1W = areaW * 0.22f;
        float col2W = areaW - col1W - gap;

        float col1X = padL;
        float col2X = col1X + col1W + gap;

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
                float iconCx = col1X + col1W * 0.18f;
                float iconCy = ty + tabH * 0.5f;
                drawFootballIcon(canvas, iconCx, iconCy, iconRadius);

                // Text
                textPaint.setTextAlign(Paint.Align.LEFT);
                textPaint.setColor(Color.parseColor("#141200"));
                textPaint.setTextSize(tabH * 0.36f);
                canvas.drawText("RESUME", iconCx + iconRadius + 10f, ty + tabH * 0.62f, textPaint);
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
        cardPaint.setColor(Color.argb(160, 24, 22, 46));
        canvas.drawRoundRect(headerCard, 14f, 14f, cardPaint);
        borderPaint.setColor(Color.argb(80, 100, 100, 180));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(headerCard, 14f, 14f, borderPaint);

        // Subtitle "MATCH PAUSED"
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setColor(Color.parseColor("#A8ABCF"));
        subTextPaint.setTextSize(headerH * 0.19f);
        canvas.drawText("MATCH PAUSED", col2X + col2W * 0.5f, padT + headerH * 0.25f, subTextPaint);

        // Team 1 (Home) Crest & Name
        float t1Cx = col2X + col2W * 0.20f;
        drawTeamCrest(canvas, t1Cx, padT + headerH * 0.56f, headerH * 0.26f, 0, team1Name, Color.parseColor("#E53935"), Color.parseColor("#1E88E5"));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(headerH * 0.20f);
        canvas.drawText(team1Name, t1Cx, padT + headerH * 0.93f, textPaint);

        // Team 2 (Away) Crest & Name
        float t2Cx = col2X + col2W * 0.80f;
        drawTeamCrest(canvas, t2Cx, padT + headerH * 0.56f, headerH * 0.26f, 1, team2Name, Color.parseColor("#3949AB"), Color.parseColor("#D81B60"));
        canvas.drawText(team2Name, t2Cx, padT + headerH * 0.93f, textPaint);

        // Score Capsule
        float scoreCy = padT + headerH * 0.56f;
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(headerH * 0.38f);
        canvas.drawText(score1 + "      " + score2, col2X + col2W * 0.5f, scoreCy + headerH * 0.12f, textPaint);

        // Match Time Pill in Center
        RectF timePill = new RectF(col2X + col2W * 0.5f - 46f, scoreCy - headerH * 0.19f, col2X + col2W * 0.5f + 46f, scoreCy + headerH * 0.06f);
        cardPaint.setColor(Color.argb(210, 16, 14, 32));
        canvas.drawRoundRect(timePill, 10f, 10f, cardPaint);
        subTextPaint.setColor(Color.parseColor("#FFD54F"));
        subTextPaint.setTextSize(headerH * 0.18f);
        canvas.drawText(matchTime, col2X + col2W * 0.5f, scoreCy - headerH * 0.02f, subTextPaint);

        // "Team Stats" Bar with [L1] [R1]
        float statBarY = padT + headerH + 6f;
        float statBarH = areaH * 0.060f;
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
    }

    /**
     * PES Mobile 5-Card Settings Screen:
     * - Top Header: Team 1 Crest & Name | Score "0 - 0" | Team 2 Crest & Name
     * - Row 1 (3 White Cards): Squad Management, Controls List, General Settings
     * - Row 2 (2 White Cards): Camera Settings, Sound Settings
     * - Bottom Left: Blue pill "< Back" button
     */
    private void drawSettingsGrid(Canvas canvas, int w, int h) {
        // Deep background dimming
        cardPaint.setColor(Color.argb(240, 10, 12, 22));
        canvas.drawRect(0, 0, w, h, cardPaint);

        float padL = Math.max(w * 0.05f, safeInsetLeft + 24f);
        float padR = Math.max(w * 0.05f, safeInsetRight + 24f);
        float headY = Math.max(h * 0.04f, safeInsetTop + 14f);
        float headH = h * 0.13f;

        // ── TOP HEADER SCOREBOARD ──
        RectF headerRect = new RectF(padL, headY, w - padR, headY + headH);
        cardPaint.setColor(Color.argb(160, 24, 22, 46));
        canvas.drawRoundRect(headerRect, 14f, 14f, cardPaint);
        borderPaint.setColor(Color.argb(70, 100, 100, 180));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(headerRect, 14f, 14f, borderPaint);

        // Team 1 (Home)
        float t1Cx = padL + 36f;
        float crestR = headH * 0.32f;
        drawTeamCrest(canvas, t1Cx, headY + headH * 0.50f, crestR, 0, team1Name, Color.parseColor("#E53935"), Color.parseColor("#1E88E5"));
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(headH * 0.36f);
        canvas.drawText(team1Name, t1Cx + crestR + 16f, headY + headH * 0.62f, textPaint);

        // Center Score Capsule
        float midX = w * 0.5f;
        RectF scoreCapsule = new RectF(midX - 70f, headY + headH * 0.16f, midX + 70f, headY + headH * 0.84f);
        cardPaint.setColor(Color.argb(200, 14, 16, 32));
        canvas.drawRoundRect(scoreCapsule, 10f, 10f, cardPaint);
        borderPaint.setColor(Color.argb(120, 0, 240, 255));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(scoreCapsule, 10f, 10f, borderPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(headH * 0.44f);
        canvas.drawText(score1 + " - " + score2, midX, headY + headH * 0.65f, textPaint);

        // Team 2 (Away)
        float t2Cx = w - padR - 36f;
        drawTeamCrest(canvas, t2Cx, headY + headH * 0.50f, crestR, 1, team2Name, Color.parseColor("#3949AB"), Color.parseColor("#D81B60"));
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(headH * 0.36f);
        canvas.drawText(team2Name, t2Cx - crestR - 16f, headY + headH * 0.62f, textPaint);

        // ── 5 WHITE ROUNDED CARDS GRID ──
        float gridTop = headY + headH + 16f;
        float gridBottom = h - Math.max(h * 0.04f, safeInsetBottom + 16f) - 52f;
        float gridW = w - padL - padR;
        float gridH = gridBottom - gridTop;
        float cardGap = gridW * 0.018f;

        float cardW1 = (gridW - 2f * cardGap) / 3f;
        float cardH = (gridH - cardGap) / 2f;
        float cardW2 = (gridW - cardGap) / 2f;

        String[] cardTitles = {
                "Squad Management",
                "Controls List",
                "General Settings",
                "Camera Settings",
                "Sound Settings"
        };
        String[] cardSubtitles = {
                "Tactics & Lineup",
                "Touch Buttons Guide",
                "Visuals, Kits & Sun",
                "Height, Zoom & Angle",
                "Volume & Commentary"
        };

        RectF[] cardRects = new RectF[5];
        cardRects[0] = new RectF(padL, gridTop, padL + cardW1, gridTop + cardH);
        cardRects[1] = new RectF(padL + cardW1 + cardGap, gridTop, padL + cardW1 * 2f + cardGap, gridTop + cardH);
        cardRects[2] = new RectF(padL + (cardW1 + cardGap) * 2f, gridTop, padL + gridW, gridTop + cardH);
        cardRects[3] = new RectF(padL, gridTop + cardH + cardGap, padL + cardW2, gridTop + gridH);
        cardRects[4] = new RectF(padL + cardW2 + cardGap, gridTop + cardH + cardGap, padL + gridW, gridTop + gridH);

        for (int i = 0; i < 5; i++) {
            RectF r = cardRects[i];
            boolean isPressed = (pressedSettingsCardIndex == i);

            // Pure White Card Fill
            cardPaint.setColor(isPressed ? Color.parseColor("#E4E7ED") : Color.parseColor("#FFFFFF"));
            canvas.drawRoundRect(r, 16f, 16f, cardPaint);

            // Subtle border
            borderPaint.setColor(Color.parseColor("#DFE2EC"));
            borderPaint.setStrokeWidth(1.5f);
            canvas.drawRoundRect(r, 16f, 16f, borderPaint);

            // Sleek Vector Icon on Card
            float iconCx = r.left + r.width() * 0.16f;
            float iconCy = r.centerY();
            drawSettingsCardIcon(canvas, i, iconCx, iconCy, r.height() * 0.24f);

            // Card Title Text
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(Color.parseColor("#1A1C29"));
            textPaint.setTextSize(r.height() * 0.22f);
            canvas.drawText(cardTitles[i], iconCx + r.height() * 0.24f + 14f, r.centerY() - 4f, textPaint);

            // Card Subtitle
            subTextPaint.setTextAlign(Paint.Align.LEFT);
            subTextPaint.setColor(Color.parseColor("#787E96"));
            subTextPaint.setTextSize(r.height() * 0.15f);
            canvas.drawText(cardSubtitles[i], iconCx + r.height() * 0.24f + 14f, r.centerY() + r.height() * 0.22f, subTextPaint);
        }

        // ── BOTTOM LEFT BLUE PILL BACK BUTTON ──
        RectF backBtn = new RectF(padL, gridBottom + 10f, padL + 130f, gridBottom + 48f);
        cardPaint.setColor(Color.parseColor("#007AFF"));
        canvas.drawRoundRect(backBtn, 19f, 19f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(18f);
        canvas.drawText("← Back", backBtn.centerX(), backBtn.centerY() + 6f, textPaint);
    }

    /**
     * In-Overlay Squad Management & 4-3-3 Tactics Formation View.
     */
    private void drawSquadManagementOverlay(Canvas canvas, int w, int h) {
        cardPaint.setColor(Color.argb(245, 10, 12, 22));
        canvas.drawRect(0, 0, w, h, cardPaint);

        float padX = Math.max(w * 0.05f, safeInsetLeft + 24f);
        float topY = Math.max(h * 0.04f, safeInsetTop + 14f);

        // Back Button
        RectF backBtn = new RectF(padX, topY, padX + 120f, topY + 42f);
        cardPaint.setColor(Color.parseColor("#007AFF"));
        canvas.drawRoundRect(backBtn, 16f, 16f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("← Back", backBtn.centerX(), backBtn.centerY() + 6f, textPaint);

        // Header Title
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(22f);
        canvas.drawText("SQUAD MANAGEMENT & FORMATION (4-3-3)", padX + 140f, topY + 30f, textPaint);

        float bodyTop = topY + 54f;
        float bodyH = h - bodyTop - Math.max(h * 0.04f, safeInsetBottom + 16f);
        float colW = (w - padX * 2f - 20f) / 2f;

        // LEFT: Tactical Pitch Board
        RectF pitchCard = new RectF(padX, bodyTop, padX + colW, bodyTop + bodyH);
        cardPaint.setColor(Color.parseColor("#1B5E20"));
        canvas.drawRoundRect(pitchCard, 14f, 14f, cardPaint);

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

        // RIGHT: Lineup Roster Card
        RectF rosterCard = new RectF(padX + colW + 20f, bodyTop, padX + colW * 2f + 20f, bodyTop + bodyH);
        cardPaint.setColor(Color.parseColor("#1A1D2E"));
        canvas.drawRoundRect(rosterCard, 14f, 14f, cardPaint);
        borderPaint.setColor(Color.argb(80, 100, 100, 180));
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(rosterCard, 14f, 14f, borderPaint);

        // Starting XI Roster
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
     * In-Overlay Touch Controls Guide.
     */
    private void drawControlsListOverlay(Canvas canvas, int w, int h) {
        cardPaint.setColor(Color.argb(245, 10, 12, 22));
        canvas.drawRect(0, 0, w, h, cardPaint);

        float padX = Math.max(w * 0.05f, safeInsetLeft + 24f);
        float topY = Math.max(h * 0.04f, safeInsetTop + 14f);

        // Back Button
        RectF backBtn = new RectF(padX, topY, padX + 120f, topY + 42f);
        cardPaint.setColor(Color.parseColor("#007AFF"));
        canvas.drawRoundRect(backBtn, 16f, 16f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("← Back", backBtn.centerX(), backBtn.centerY() + 6f, textPaint);

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(22f);
        canvas.drawText("TOUCH CONTROLS GUIDE", padX + 140f, topY + 30f, textPaint);

        float bodyTop = topY + 54f;
        float bodyH = h - bodyTop - Math.max(h * 0.04f, safeInsetBottom + 16f);
        float colW = (w - padX * 2f - 20f) / 2f;

        // Attacking Controls Card
        RectF atkCard = new RectF(padX, bodyTop, padX + colW, bodyTop + bodyH);
        cardPaint.setColor(Color.parseColor("#1A1D2E"));
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
        cardPaint.setColor(Color.parseColor("#1A1D2E"));
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
     * In-Overlay General & Visual Settings (Kits, Sun position, Radar, Overhead names).
     * Restores missing visual options from GameplayFootball-master.
     */
    private void drawGeneralSettingsOverlay(Canvas canvas, int w, int h) {
        cardPaint.setColor(Color.argb(245, 10, 12, 22));
        canvas.drawRect(0, 0, w, h, cardPaint);

        float padX = Math.max(w * 0.05f, safeInsetLeft + 24f);
        float topY = Math.max(h * 0.04f, safeInsetTop + 14f);

        // Back Button
        RectF backBtn = new RectF(padX, topY, padX + 120f, topY + 42f);
        cardPaint.setColor(Color.parseColor("#007AFF"));
        canvas.drawRoundRect(backBtn, 16f, 16f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("← Back", backBtn.centerX(), backBtn.centerY() + 6f, textPaint);

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(22f);
        canvas.drawText("GENERAL & VISUAL SETTINGS", padX + 140f, topY + 30f, textPaint);

        float bodyTop = topY + 54f;
        float bodyH = h - bodyTop - Math.max(h * 0.04f, safeInsetBottom + 16f);
        float bodyW = w - padX * 2f;

        RectF card = new RectF(padX, bodyTop, padX + bodyW, bodyTop + bodyH);
        cardPaint.setColor(Color.parseColor("#16182B"));
        canvas.drawRoundRect(card, 16f, 16f, cardPaint);
        borderPaint.setColor(Color.parseColor("#00F0FF"));
        borderPaint.setStrokeWidth(2f);
        canvas.drawRoundRect(card, 16f, 16f, borderPaint);

        float rowH = 46f;
        float rowGap = 12f;

        // Row 0: Team 1 Kit
        float r0Y = bodyTop + 24f;
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(18f);
        canvas.drawText(team1Name + " Kit Selection", padX + 32f, r0Y + 30f, textPaint);

        RectF kit1_0 = new RectF(padX + bodyW * 0.52f, r0Y, padX + bodyW * 0.52f + 110f, r0Y + rowH);
        RectF kit1_1 = new RectF(padX + bodyW * 0.52f + 125f, r0Y, padX + bodyW * 0.52f + 235f, r0Y + rowH);
        cardPaint.setColor(selectedKit1 == 0 ? Color.parseColor("#00F0FF") : Color.parseColor("#262940"));
        canvas.drawRoundRect(kit1_0, 10f, 10f, cardPaint);
        cardPaint.setColor(selectedKit1 == 1 ? Color.parseColor("#00F0FF") : Color.parseColor("#262940"));
        canvas.drawRoundRect(kit1_1, 10f, 10f, cardPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(selectedKit1 == 0 ? Color.BLACK : Color.WHITE);
        canvas.drawText("Kit 01", kit1_0.centerX(), kit1_0.centerY() + 6f, textPaint);
        textPaint.setColor(selectedKit1 == 1 ? Color.BLACK : Color.WHITE);
        canvas.drawText("Kit 02", kit1_1.centerX(), kit1_1.centerY() + 6f, textPaint);

        // Row 1: Team 2 Kit
        float r1Y = r0Y + rowH + rowGap;
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(team2Name + " Kit Selection", padX + 32f, r1Y + 30f, textPaint);

        RectF kit2_0 = new RectF(padX + bodyW * 0.52f, r1Y, padX + bodyW * 0.52f + 110f, r1Y + rowH);
        RectF kit2_1 = new RectF(padX + bodyW * 0.52f + 125f, r1Y, padX + bodyW * 0.52f + 235f, r1Y + rowH);
        cardPaint.setColor(selectedKit2 == 0 ? Color.parseColor("#00F0FF") : Color.parseColor("#262940"));
        canvas.drawRoundRect(kit2_0, 10f, 10f, cardPaint);
        cardPaint.setColor(selectedKit2 == 1 ? Color.parseColor("#00F0FF") : Color.parseColor("#262940"));
        canvas.drawRoundRect(kit2_1, 10f, 10f, cardPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(selectedKit2 == 0 ? Color.BLACK : Color.WHITE);
        canvas.drawText("Kit 01", kit2_0.centerX(), kit2_0.centerY() + 6f, textPaint);
        textPaint.setColor(selectedKit2 == 1 ? Color.BLACK : Color.WHITE);
        canvas.drawText("Kit 02", kit2_1.centerX(), kit2_1.centerY() + 6f, textPaint);

        // Row 2: Sun Position & Lighting
        float r2Y = r1Y + rowH + rowGap;
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("Pitch Lighting & Sun Angle", padX + 32f, r2Y + 30f, textPaint);

        RectF sunBtn = new RectF(padX + bodyW * 0.52f, r2Y, padX + bodyW * 0.52f + 260f, r2Y + rowH);
        cardPaint.setColor(Color.parseColor("#FFA800"));
        canvas.drawRoundRect(sunBtn, 10f, 10f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.parseColor("#141200"));
        canvas.drawText("☀ Randomize Sun Position", sunBtn.centerX(), sunBtn.centerY() + 6f, textPaint);

        // Row 3: Pitch Radar HUD
        float r3Y = r2Y + rowH + rowGap;
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("Pitch Radar HUD Overlay", padX + 32f, r3Y + 30f, textPaint);

        RectF radarBtn = new RectF(padX + bodyW * 0.52f, r3Y, padX + bodyW * 0.52f + 110f, r3Y + rowH);
        cardPaint.setColor(settingRadarEnabled ? Color.parseColor("#00E676") : Color.parseColor("#424242"));
        canvas.drawRoundRect(radarBtn, 10f, 10f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(settingRadarEnabled ? "ON" : "OFF", radarBtn.centerX(), radarBtn.centerY() + 6f, textPaint);

        // Row 4: Overhead Player Names
        float r4Y = r3Y + rowH + rowGap;
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("Player Names & Cursor Overhead", padX + 32f, r4Y + 30f, textPaint);

        RectF namesBtn = new RectF(padX + bodyW * 0.52f, r4Y, padX + bodyW * 0.52f + 110f, r4Y + rowH);
        cardPaint.setColor(settingPlayerNames ? Color.parseColor("#00E676") : Color.parseColor("#424242"));
        canvas.drawRoundRect(namesBtn, 10f, 10f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(settingPlayerNames ? "ON" : "OFF", namesBtn.centerX(), namesBtn.centerY() + 6f, textPaint);
    }

    /**
     * In-Overlay Camera Settings (Height, Zoom, Dynamic Pitch Angle).
     */
    private void drawCameraSettingsOverlay(Canvas canvas, int w, int h) {
        cardPaint.setColor(Color.argb(245, 10, 12, 22));
        canvas.drawRect(0, 0, w, h, cardPaint);

        float padX = Math.max(w * 0.05f, safeInsetLeft + 24f);
        float topY = Math.max(h * 0.04f, safeInsetTop + 14f);

        // Back Button
        RectF backBtn = new RectF(padX, topY, padX + 120f, topY + 42f);
        cardPaint.setColor(Color.parseColor("#007AFF"));
        canvas.drawRoundRect(backBtn, 16f, 16f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("← Back", backBtn.centerX(), backBtn.centerY() + 6f, textPaint);

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(22f);
        canvas.drawText("CAMERA SETTINGS", padX + 140f, topY + 30f, textPaint);

        float bodyTop = topY + 54f;
        float bodyH = h - bodyTop - Math.max(h * 0.04f, safeInsetBottom + 16f);
        float bodyW = w - padX * 2f;

        RectF card = new RectF(padX, bodyTop, padX + bodyW, bodyTop + bodyH);
        cardPaint.setColor(Color.parseColor("#16182B"));
        canvas.drawRoundRect(card, 16f, 16f, cardPaint);
        borderPaint.setColor(Color.parseColor("#00F0FF"));
        borderPaint.setStrokeWidth(2f);
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

        float trackLeft = padX + bodyW * 0.38f;
        float trackRight = padX + bodyW * 0.88f;
        float trackW = trackRight - trackLeft;
        float rowH = 50f;
        float rowGap = 16f;

        for (int i = 0; i < 3; i++) {
            float ry = bodyTop + 20f + i * (rowH + rowGap);
            float cy = ry + rowH * 0.5f;

            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(18f);
            canvas.drawText(sliderTitles[i], padX + 32f, cy + 6f, textPaint);

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
            textPaint.setTextSize(18f);
            canvas.drawText(displayVals[i], trackRight + 16f, cy + 6f, textPaint);
        }

        // Reset to Defaults Button
        RectF resetBtn = new RectF(padX + bodyW * 0.52f, bodyTop + 3f * (rowH + rowGap) + 10f,
                padX + bodyW * 0.52f + 200f, bodyTop + 3f * (rowH + rowGap) + 52f);
        cardPaint.setColor(Color.parseColor("#262940"));
        canvas.drawRoundRect(resetBtn, 10f, 10f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(16f);
        canvas.drawText("Reset Defaults", resetBtn.centerX(), resetBtn.centerY() + 5f, textPaint);
    }

    /**
     * In-Overlay Sound Settings (SFX, Commentary, Master Volume).
     */
    private void drawSoundSettingsOverlay(Canvas canvas, int w, int h) {
        cardPaint.setColor(Color.argb(245, 10, 12, 22));
        canvas.drawRect(0, 0, w, h, cardPaint);

        float padX = Math.max(w * 0.05f, safeInsetLeft + 24f);
        float topY = Math.max(h * 0.04f, safeInsetTop + 14f);

        // Back Button
        RectF backBtn = new RectF(padX, topY, padX + 120f, topY + 42f);
        cardPaint.setColor(Color.parseColor("#007AFF"));
        canvas.drawRoundRect(backBtn, 16f, 16f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("← Back", backBtn.centerX(), backBtn.centerY() + 6f, textPaint);

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(22f);
        canvas.drawText("SOUND SETTINGS", padX + 140f, topY + 30f, textPaint);

        float bodyTop = topY + 54f;
        float bodyH = h - bodyTop - Math.max(h * 0.04f, safeInsetBottom + 16f);
        float bodyW = w - padX * 2f;

        RectF card = new RectF(padX, bodyTop, padX + bodyW, bodyTop + bodyH);
        cardPaint.setColor(Color.parseColor("#16182B"));
        canvas.drawRoundRect(card, 16f, 16f, cardPaint);
        borderPaint.setColor(Color.parseColor("#00F0FF"));
        borderPaint.setStrokeWidth(2f);
        canvas.drawRoundRect(card, 16f, 16f, borderPaint);

        float trackLeft = padX + bodyW * 0.38f;
        float trackRight = padX + bodyW * 0.88f;
        float trackW = trackRight - trackLeft;
        float cy = bodyTop + 60f;

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(18f);
        canvas.drawText("AUDIO & CROWD SFX", padX + 32f, cy + 6f, textPaint);

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
        canvas.drawText(Math.round(frac * 100) + "%", trackRight + 16f, cy + 6f, textPaint);

        // Mute toggle button
        RectF muteBtn = new RectF(padX + bodyW * 0.52f, bodyTop + 110f, padX + bodyW * 0.52f + 160f, bodyTop + 152f);
        cardPaint.setColor((settingAudioVolume < 0.01f) ? Color.parseColor("#E53935") : Color.parseColor("#262940"));
        canvas.drawRoundRect(muteBtn, 10f, 10f, cardPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(16f);
        canvas.drawText((settingAudioVolume < 0.01f) ? "Muted" : "Mute Sound", muteBtn.centerX(), muteBtn.centerY() + 5f, textPaint);
    }

    /**
     * Replay Playback Overlay:
     * Interactive HUD allowing stepping forward/backward, pause/play, and exiting replay.
     */
    private void drawReplayPlaybackOverlay(Canvas canvas, int w, int h) {
        // Transparent cinematic bars
        cardPaint.setColor(Color.argb(120, 0, 0, 0));
        canvas.drawRect(0, 0, w, 56f, cardPaint);
        canvas.drawRect(0, h - 80f, w, h, cardPaint);

        // Top Replay indicator
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.parseColor("#FFA800"));
        textPaint.setTextSize(20f);
        canvas.drawText("● REPLAY PLAYBACK", w * 0.5f, 38f, textPaint);

        // Bottom playback controls
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

        // 1. Rewind 1s
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f);
        canvas.drawText("<< 1s", barX + btnW * 0.5f, barY + barH * 0.62f, textPaint);

        // 2. Play/Pause
        textPaint.setColor(Color.parseColor("#00F0FF"));
        canvas.drawText(isReplayPlaying ? "❚❚ Pause" : "▶ Play", barX + btnW * 1.5f, barY + barH * 0.62f, textPaint);

        // 3. Forward 1s
        textPaint.setColor(Color.WHITE);
        canvas.drawText("1s >>", barX + btnW * 2.5f, barY + barH * 0.62f, textPaint);

        // 4. Close Replay
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

    private void drawSettingsCardIcon(Canvas canvas, int index, float cx, float cy, float radius) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.parseColor("#1E88E5"));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.8f);

        if (index == 0) { // Squad Management - Tactics board
            canvas.drawRoundRect(new RectF(cx - radius * 0.8f, cy - radius * 0.9f, cx + radius * 0.8f, cy + radius * 0.9f), 4f, 4f, p);
            canvas.drawLine(cx - radius * 0.8f, cy, cx + radius * 0.8f, cy, p);
            canvas.drawCircle(cx, cy, radius * 0.35f, p);
        } else if (index == 1) { // Controls List - Game Controller
            canvas.drawRoundRect(new RectF(cx - radius, cy - radius * 0.6f, cx + radius, cy + radius * 0.6f), 8f, 8f, p);
            canvas.drawLine(cx - radius * 0.5f, cy, cx - radius * 0.2f, cy, p);
            canvas.drawLine(cx - radius * 0.35f, cy - radius * 0.2f, cx - radius * 0.35f, cy + radius * 0.2f, p);
            p.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx + radius * 0.35f, cy - radius * 0.15f, radius * 0.12f, p);
            canvas.drawCircle(cx + radius * 0.55f, cy + radius * 0.10f, radius * 0.12f, p);
        } else if (index == 2) { // General Settings - Cog / Gear
            canvas.drawCircle(cx, cy, radius * 0.55f, p);
            p.setStrokeWidth(4f);
            for (int i = 0; i < 4; i++) {
                double a = Math.toRadians(i * 45);
                float x1 = cx + (float)(Math.cos(a) * radius * 0.55f);
                float y1 = cy + (float)(Math.sin(a) * radius * 0.55f);
                float x2 = cx + (float)(Math.cos(a) * radius * 0.90f);
                float y2 = cy + (float)(Math.sin(a) * radius * 0.90f);
                canvas.drawLine(x1, y1, x2, y2, p);
            }
        } else if (index == 3) { // Camera Settings - Video Camera
            canvas.drawRoundRect(new RectF(cx - radius * 0.8f, cy - radius * 0.55f, cx + radius * 0.3f, cy + radius * 0.55f), 4f, 4f, p);
            p.setStyle(Paint.Style.FILL);
            Path tri = new Path();
            tri.moveTo(cx + radius * 0.35f, cy - radius * 0.25f);
            tri.lineTo(cx + radius * 0.85f, cy - radius * 0.55f);
            tri.lineTo(cx + radius * 0.85f, cy + radius * 0.55f);
            tri.lineTo(cx + radius * 0.35f, cy + radius * 0.25f);
            tri.close();
            canvas.drawPath(tri, p);
        } else if (index == 4) { // Sound Settings - Speaker
            Path spk = new Path();
            spk.moveTo(cx - radius * 0.7f, cy - radius * 0.3f);
            spk.lineTo(cx - radius * 0.3f, cy - radius * 0.3f);
            spk.lineTo(cx + radius * 0.1f, cy - radius * 0.7f);
            spk.lineTo(cx + radius * 0.1f, cy + radius * 0.7f);
            spk.lineTo(cx - radius * 0.3f, cy + radius * 0.3f);
            spk.lineTo(cx - radius * 0.7f, cy + radius * 0.3f);
            spk.close();
            p.setStyle(Paint.Style.FILL);
            canvas.drawPath(spk, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.5f);
            canvas.drawArc(new RectF(cx + radius * 0.15f, cy - radius * 0.5f, cx + radius * 0.75f, cy + radius * 0.5f), -45f, 90f, false, p);
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
            float gap = areaW * 0.016f;
            float col1W = areaW * 0.22f;
            float tabH = (areaH - 5f * gap) / 6f;

            // 2.A: SETTINGS_GRID TOUCHES (PES Mobile 5-Card Grid)
            if (pauseSubView == PauseSubView.SETTINGS_GRID) {
                float headY = Math.max(getHeight() * 0.04f, safeInsetTop + 14f);
                float headH = getHeight() * 0.13f;
                float gridPadL = Math.max(getWidth() * 0.05f, safeInsetLeft + 24f);
                float gridPadR = Math.max(getWidth() * 0.05f, safeInsetRight + 24f);
                float gridTop = headY + headH + 16f;
                float gridBottom = getHeight() - Math.max(getHeight() * 0.04f, safeInsetBottom + 16f) - 52f;
                float gridW = getWidth() - gridPadL - gridPadR;
                float gridH = gridBottom - gridTop;
                float cardGap = gridW * 0.018f;
                float cardW1 = (gridW - 2f * cardGap) / 3f;
                float cardH = (gridH - cardGap) / 2f;
                float cardW2 = (gridW - cardGap) / 2f;

                RectF backBtn = new RectF(gridPadL, gridBottom + 10f, gridPadL + 130f, gridBottom + 48f);

                RectF card0 = new RectF(gridPadL, gridTop, gridPadL + cardW1, gridTop + cardH);
                RectF card1 = new RectF(gridPadL + cardW1 + cardGap, gridTop, gridPadL + cardW1 * 2f + cardGap, gridTop + cardH);
                RectF card2 = new RectF(gridPadL + (cardW1 + cardGap) * 2f, gridTop, gridPadL + gridW, gridTop + cardH);
                RectF card3 = new RectF(gridPadL, gridTop + cardH + cardGap, gridPadL + cardW2, gridTop + gridH);
                RectF card4 = new RectF(gridPadL + cardW2 + cardGap, gridTop + cardH + cardGap, gridPadL + gridW, gridTop + gridH);

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        if (backBtn.contains(x, y)) {
                            pauseSubView = PauseSubView.OVERVIEW;
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
                RectF backBtn = new RectF(sPadX, sTopY, sPadX + 120f, sTopY + 42f);
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                    if (backBtn.contains(x, y)) {
                        pauseSubView = PauseSubView.SETTINGS_GRID;
                        invalidate();
                        return true;
                    }
                }
                return true;
            }

            // 2.C: GENERAL_SETTINGS TOUCHES (Kits, Sun, Radar, Overhead names)
            if (pauseSubView == PauseSubView.GENERAL_SETTINGS) {
                float gPadX = Math.max(getWidth() * 0.05f, safeInsetLeft + 24f);
                float gTopY = Math.max(getHeight() * 0.04f, safeInsetTop + 14f);
                RectF backBtn = new RectF(gPadX, gTopY, gPadX + 120f, gTopY + 42f);

                float bodyTop = gTopY + 54f;
                float bodyW = getWidth() - gPadX * 2f;
                float rowH = 46f;
                float rowGap = 12f;

                float r0Y = bodyTop + 24f;
                RectF kit1_0 = new RectF(gPadX + bodyW * 0.52f, r0Y, gPadX + bodyW * 0.52f + 110f, r0Y + rowH);
                RectF kit1_1 = new RectF(gPadX + bodyW * 0.52f + 125f, r0Y, gPadX + bodyW * 0.52f + 235f, r0Y + rowH);

                float r1Y = r0Y + rowH + rowGap;
                RectF kit2_0 = new RectF(gPadX + bodyW * 0.52f, r1Y, gPadX + bodyW * 0.52f + 110f, r1Y + rowH);
                RectF kit2_1 = new RectF(gPadX + bodyW * 0.52f + 125f, r1Y, gPadX + bodyW * 0.52f + 235f, r1Y + rowH);

                float r2Y = r1Y + rowH + rowGap;
                RectF sunBtn = new RectF(gPadX + bodyW * 0.52f, r2Y, gPadX + bodyW * 0.52f + 260f, r2Y + rowH);

                float r3Y = r2Y + rowH + rowGap;
                RectF radarBtn = new RectF(gPadX + bodyW * 0.52f, r3Y, gPadX + bodyW * 0.52f + 110f, r3Y + rowH);

                float r4Y = r3Y + rowH + rowGap;
                RectF namesBtn = new RectF(gPadX + bodyW * 0.52f, r4Y, gPadX + bodyW * 0.52f + 110f, r4Y + rowH);

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
                    }
                }
                return true;
            }

            // 2.D: CAMERA_SETTINGS TOUCHES
            if (pauseSubView == PauseSubView.CAMERA_SETTINGS) {
                float cPadX = Math.max(getWidth() * 0.05f, safeInsetLeft + 24f);
                float cTopY = Math.max(getHeight() * 0.04f, safeInsetTop + 14f);
                RectF backBtn = new RectF(cPadX, cTopY, cPadX + 120f, cTopY + 42f);

                float bodyTop = cTopY + 54f;
                float bodyW = getWidth() - cPadX * 2f;
                float trackLeft = cPadX + bodyW * 0.38f;
                float trackRight = cPadX + bodyW * 0.88f;
                float trackW = trackRight - trackLeft;
                float rowH = 50f;
                float rowGap = 16f;

                RectF resetBtn = new RectF(cPadX + bodyW * 0.52f, bodyTop + 3f * (rowH + rowGap) + 10f,
                        cPadX + bodyW * 0.52f + 200f, bodyTop + 3f * (rowH + rowGap) + 52f);

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
                            float ry = bodyTop + 20f + s * (rowH + rowGap);
                            if (y >= ry - 20f && y <= ry + rowH + 20f && x >= trackLeft - 40f && x <= trackRight + 40f) {
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
                RectF backBtn = new RectF(sndPadX, sndTopY, sndPadX + 120f, sndTopY + 42f);

                float bodyTop = sndTopY + 54f;
                float bodyW = getWidth() - sndPadX * 2f;
                float trackLeft = sndPadX + bodyW * 0.38f;
                float trackRight = sndPadX + bodyW * 0.88f;
                float trackW = trackRight - trackLeft;

                RectF muteBtn = new RectF(sndPadX + bodyW * 0.52f, bodyTop + 110f, sndPadX + bodyW * 0.52f + 160f, bodyTop + 152f);

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
                        float ry = bodyTop + 60f;
                        if (y >= ry - 30f && y <= ry + 30f && x >= trackLeft - 40f && x <= trackRight + 40f) {
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
                        pauseSubView = PauseSubView.OVERVIEW;
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
                        pauseSubView = PauseSubView.OVERVIEW;
                        invalidate();
                        return true;
                    } else if (quitBtn.contains(x, y)) {
                        isPaused = false;
                        pauseSubView = PauseSubView.OVERVIEW;
                        try {
                            SDLActivity.quitMatchToMainMenu();
                        } catch (Throwable ignored) {}
                        return true;
                    }
                }
                return true;
            }

            // 2.H: OVERVIEW (PES MAIN PAUSE SCREEN) TOUCHES
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    for (int i = 0; i < 6; i++) {
                        float ty = padT + i * (tabH + gap);
                        if (x >= padL && x <= padL + col1W && y >= ty && y <= ty + tabH) {
                            pressedMenuIndex = i;
                            invalidate();
                            return true;
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    if (pressedMenuIndex != -1) {
                        final int index = pressedMenuIndex;
                        pressedMenuIndex = -1;
                        invalidate();

                        if (index == 0) {
                            // Resume Match
                            isPaused = false;
                            pauseSubView = PauseSubView.OVERVIEW;
                            try {
                                SDLActivity.setMatchPaused(false);
                            } catch (Throwable ignored) {}
                            invalidate();
                        } else if (index == 1) {
                            // Game Plan: Squad Management & Formation
                            pauseSubView = PauseSubView.SQUAD_MANAGEMENT;
                            invalidate();
                        } else if (index == 2) {
                            // Match Records: refresh live telemetry stats
                            refreshMatchStats();
                            invalidate();
                        } else if (index == 3) {
                            // Settings: PES 5-Card Grid
                            pauseSubView = PauseSubView.SETTINGS_GRID;
                            invalidate();
                        } else if (index == 4) {
                            // Highlights: Instant Replay Playback
                            try {
                                SDLActivity.triggerReplay(6);
                            } catch (Throwable ignored) {}
                            pauseSubView = PauseSubView.REPLAY_PLAYBACK;
                            isReplayPlaying = true;
                            removeCallbacks(replayStepper);
                            post(replayStepper);
                            invalidate();
                        } else if (index == 5) {
                            // Top Menu: Forfeit Confirmation Dialog
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
