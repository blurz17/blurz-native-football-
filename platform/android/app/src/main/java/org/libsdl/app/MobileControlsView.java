package org.libsdl.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
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
     * Draw modern Glassmorphic Pause Menu Overlay.
     */
    private void drawPauseMenu(Canvas canvas, int w, int h) {
        // Deep Dark Navy Glass Dimmer
        cardPaint.setColor(Color.argb(225, 8, 12, 22));
        canvas.drawRect(0, 0, w, h, cardPaint);

        // Header Section (Left Side)
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(h * 0.075f);
        float leftX = Math.max(w * 0.08f, safeInsetLeft + 32f);
        canvas.drawText("MATCH PAUSED", leftX, h * 0.20f, textPaint);

        // Live Match Snapshot Card
        float cardW = w * 0.36f;
        float cardH = h * 0.20f;
        float cardY = h * 0.26f;
        RectF statCard = new RectF(leftX, cardY, leftX + cardW, cardY + cardH);

        cardPaint.setColor(Color.argb(120, 20, 30, 48));
        canvas.drawRoundRect(statCard, 16f, 16f, cardPaint);

        borderPaint.setColor(Color.argb(90, 0, 240, 255));
        borderPaint.setStrokeWidth(2f);
        canvas.drawRoundRect(statCard, 16f, 16f, borderPaint);

        // Scoreline inside stat card
        textPaint.setColor(Color.parseColor("#00F0FF"));
        textPaint.setTextSize(h * 0.065f);
        canvas.drawText(team1Name + "   " + score1 + " - " + score2 + "   " + team2Name,
                leftX + 24f, cardY + cardH * 0.52f, textPaint);

        subTextPaint.setTextAlign(Paint.Align.LEFT);
        subTextPaint.setColor(Color.argb(180, 200, 220, 255));
        subTextPaint.setTextSize(h * 0.035f);
        canvas.drawText("MATCH CLOCK: " + matchTime, leftX + 24f, cardY + cardH * 0.82f, subTextPaint);

        // Menu Options (Right Side)
        float btnW = Math.min(w * 0.42f, 480f);
        float btnH = h * 0.10f;
        float startX = w - Math.max(w * 0.48f, safeInsetRight + btnW + 24f);
        float startY = h * 0.12f;
        float spacing = h * 0.125f;

        String[] menuItems = {
                "RESUME MATCH",
                "SQUAD MANAGEMENT",
                "MATCH SETTINGS",
                "CAMERA SETTINGS",
                "COMMAND LIST",
                "FORFEIT MATCH"
        };

        for (int i = 0; i < menuItems.length; i++) {
            float y = startY + (i * spacing);
            boolean isPressed = (pressedMenuIndex == i);

            // Card Background
            cardPaint.setColor(Color.argb(isPressed ? 190 : 110, 22, 32, 50));
            canvas.drawRoundRect(new RectF(startX, y, startX + btnW, y + btnH), 18f, 18f, cardPaint);

            // Card Border
            int borderCol = (i == 0) ? Color.parseColor("#00F0FF")
                    : (isPressed ? Color.parseColor("#00F0FF") : Color.argb(110, 50, 75, 105));
            borderPaint.setColor(borderCol);
            borderPaint.setStrokeWidth(isPressed || i == 0 ? 3.5f : 2f);
            canvas.drawRoundRect(new RectF(startX, y, startX + btnW, y + btnH), 18f, 18f, borderPaint);

            // Left Accent Indicator
            accentPaint.setColor(i == 5 ? Color.parseColor("#FF3366")
                    : (i == 0 || isPressed ? Color.parseColor("#00F0FF") : Color.parseColor("#00E676")));
            canvas.drawRoundRect(new RectF(startX, y + btnH * 0.20f, startX + 10f, y + btnH * 0.80f),
                    4f, 4f, accentPaint);

            // Item Text
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setTextSize(btnH * 0.38f);
            textPaint.setColor(isPressed || i == 0 ? Color.parseColor("#00F0FF") : Color.WHITE);
            canvas.drawText(menuItems[i], startX + 28f, y + btnH * 0.62f, textPaint);
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
            float btnW = Math.min(getWidth() * 0.42f, 480f);
            float btnH = getHeight() * 0.10f;
            float startX = getWidth() - Math.max(getWidth() * 0.48f, safeInsetRight + btnW + 24f);
            float startY = getHeight() * 0.12f;
            float spacing = getHeight() * 0.125f;

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    for (int i = 0; i < 6; i++) {
                        float itemY = startY + (i * spacing);
                        if (x >= startX && x <= startX + btnW && y >= itemY && y <= itemY + btnH) {
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
                            // Resume Match: unpause simulation and close pause screen
                            isPaused = false;
                            invalidate();
                            SDLActivity.setVirtualKey(27, true);
                            postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    SDLActivity.setVirtualKey(27, false);
                                }
                            }, 50);
                        } else if (index == 1) {
                            getContext().startActivity(new Intent(getContext(), org.libsdl.app.ui.GamePlanActivity.class));
                        } else if (index == 2) {
                            getContext().startActivity(new Intent(getContext(), org.libsdl.app.ui.SettingsActivity.class));
                        } else if (index == 3) {
                            getContext().startActivity(new Intent(getContext(), org.libsdl.app.ui.CameraSettingsActivity.class));
                        } else if (index == 4) {
                            getContext().startActivity(new Intent(getContext(), org.libsdl.app.ui.CommandListActivity.class));
                        } else if (index == 5) {
                            new AlertDialog.Builder(getContext())
                                    .setTitle("FORFEIT MATCH")
                                    .setMessage("Are you sure you want to forfeit this match?")
                                    .setPositiveButton("YES, FORFEIT", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            isPaused = false;
                                            SDLActivity.setVirtualKey(53, true); // SDLK_5
                                            postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    SDLActivity.setVirtualKey(53, false);
                                                }
                                            }, 50);
                                        }
                                    })
                                    .setNegativeButton("CANCEL", null)
                                    .show();
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
                        invalidate();
                        SDLActivity.setVirtualKey(27, true);
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                SDLActivity.setVirtualKey(27, false);
                            }
                        }, 50);
                        return true;
                    }
                }

                // Check Left Screen: Floating Virtual Joystick
                if (x < getWidth() * 0.45f && joyPointerId == -1) {
                    joyPointerId = pointerId;
                    isJoyFloating = true;
                    joyBaseX = x;
                    joyBaseY = y;
                    joyHandleX = x;
                    joyHandleY = y;
                    updateJoystick(x, y);
                } else {
                    // Check Right Screen: Action Button Cluster
                    for (ActionButton btn : buttons) {
                        float dx = x - btn.cx;
                        float dy = y - btn.cy;
                        if (dx * dx + dy * dy <= btn.radius * btn.radius * 2.5f && btn.pointerId == -1) {
                            btn.pointerId = pointerId;
                            btn.isPressed = true;
                            int keycode = isAttacking ? btn.keycodeOffense : btn.keycodeDefense;
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
                if (pointerId == joyPointerId) {
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
                            btn.pointerId = -1;
                            btn.isPressed = false;
                            int keycode = isAttacking ? btn.keycodeOffense : btn.keycodeDefense;
                            SDLActivity.setVirtualJoystickButton(keycode, false);
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
            dx = (dx / dist) * joyRadius;
            dy = (dy / dist) * joyRadius;
        }

        joyHandleX = joyBaseX + dx;
        joyHandleY = joyBaseY + dy;

        // Apply 15% Deadzone
        float normX = dx / joyRadius;
        float normY = dy / joyRadius;

        if (dist < joyRadius * 0.15f) {
            normX = 0f;
            normY = 0f;
        }

        SDLActivity.setVirtualJoystickAxis(0, normX);
        SDLActivity.setVirtualJoystickAxis(1, normY);
    }
}
