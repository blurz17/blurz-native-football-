package org.libsdl.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MobileControlsView extends View {

    private Paint bgPaint;
    private Paint handlePaint;
    private Paint btnBgPaint;
    private Paint btnBorderPaint;
    private Paint textPaint;

    // Joystick
    private float joyBaseX, joyBaseY;
    private float joyHandleX, joyHandleY;
    private float joyRadius = 200f;
    private float joyHandleRadius = 80f;
    private int joyPointerId = -1;
    private boolean isJoyFloating = false;
    private float defaultJoyBaseX, defaultJoyBaseY;
    
    private int pressedMenuIndex = -1;

    // Game Over State
    private boolean isGameOver = false;
    private int goT1Score, goT2Score;
    private int goT1Poss, goT2Poss;
    private int goT1Shots, goT2Shots;
    private int gameOverButtonPointerId = -1;
    private boolean gameOverButtonPressed = false;

    // Active keys from joystick
    private boolean joyUp, joyDown, joyLeft, joyRight;

    // Buttons
    private class ActionButton {
        float cx, cy, radius;
        String labelOffense;
        String labelDefense;
        int keycodeOffense;
        int keycodeDefense;
        int pointerId = -1;
        boolean isPressed = false;
        int colorBase;
        int colorBorder;

        ActionButton(float cx, float cy, float radius, String labelOffense, String labelDefense, int keycodeOffense, int keycodeDefense, int colorBase, int colorBorder) {
            this.cx = cx;
            this.cy = cy;
            this.radius = radius;
            this.labelOffense = labelOffense;
            this.labelDefense = labelDefense;
            this.keycodeOffense = keycodeOffense;
            this.keycodeDefense = keycodeDefense;
            this.colorBase = colorBase;
            this.colorBorder = colorBorder;
        }
    }

    private List<ActionButton> buttons = new ArrayList<>();

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
        bgPaint.setColor(Color.argb(80, 20, 20, 20));
        bgPaint.setStyle(Paint.Style.FILL);

        handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handlePaint.setColor(Color.argb(200, 220, 220, 220));
        handlePaint.setStyle(Paint.Style.FILL);

        btnBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnBorderPaint.setStyle(Paint.Style.STROKE);
        btnBorderPaint.setStrokeWidth(8f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Joystick on the left
        defaultJoyBaseX = w * 0.15f;
        defaultJoyBaseY = h * 0.75f;
        joyBaseX = defaultJoyBaseX;
        joyBaseY = defaultJoyBaseY;
        joyHandleX = joyBaseX;
        joyHandleY = joyBaseY;

        joyRadius = h * 0.22f; // Increased for better accuracy
        joyHandleRadius = joyRadius * 0.4f;

        // Buttons on the right (eFootball Layout)
        buttons.clear();
        
        float dashX = w * 0.88f;
        float dashY = h * 0.80f;
        float dashRad = h * 0.11f; // Dash is the biggest
        
        float normalRad = h * 0.08f;
        float spacingX = w * 0.13f;
        float spacingY = h * 0.22f;

        int baseCol = Color.argb(110, 20, 25, 35); // Dark navy/glassmorphic
        int borderCol = Color.argb(150, 0, 229, 255); // Neon cyan border
        
        // PASS (Left of Dash)
        buttons.add(new ActionButton(dashX - spacingX, dashY + h * 0.02f, normalRad, "Pass", "Switch", 0, 4, baseCol, borderCol));
        
        // DASH (Bottom Right)
        buttons.add(new ActionButton(dashX, dashY, dashRad, "Dash", "Dash", 5, 5, baseCol, borderCol));
        
        // THROUGH (Left of Shoot, Above Pass)
        buttons.add(new ActionButton(dashX - spacingX * 0.9f, dashY - spacingY, normalRad, "Through", "Press", 3, 0, baseCol, borderCol));
        
        // SHOOT (Above Dash)
        buttons.add(new ActionButton(dashX - w * 0.02f, dashY - spacingY - h * 0.02f, normalRad, "Shoot", "Clear", 2, 1, baseCol, borderCol));
        
        // TOP RIGHT CORNER: PAUSE (using sleek neon outline)
        buttons.add(new ActionButton(w * 0.95f, h * 0.06f, normalRad * 0.45f, "||", "||", 7, 7, Color.argb(90, 20, 25, 35), borderCol));

        textPaint.setTextSize(normalRad * 0.45f);
    }

    public void showGameOver(int t1s, int t2s, int t1p, int t2p, int t1sh, int t2sh) {
        isGameOver = true;
        goT1Score = t1s;
        goT2Score = t2s;
        goT1Poss = t1p;
        goT2Poss = t2p;
        goT1Shots = t1sh;
        goT2Shots = t2sh;
        isMatchMode = false;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isMatchMode) {
            if (isGameOver) {
                // GAME OVER SCREEN
                Paint overlay = new Paint(Paint.ANTI_ALIAS_FLAG);
                overlay.setColor(Color.argb(230, 10, 14, 26)); // Deep dark navy tint
                canvas.drawRect(0, 0, getWidth(), getHeight(), overlay);
                
                textPaint.setColor(Color.WHITE);
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setTextSize(getHeight() * 0.12f);
                canvas.drawText("FULL TIME", getWidth() / 2f, getHeight() * 0.2f, textPaint);
                
                // Score
                textPaint.setColor(Color.parseColor("#00E5FF"));
                textPaint.setTextSize(getHeight() * 0.18f);
                canvas.drawText(goT1Score + " - " + goT2Score, getWidth() / 2f, getHeight() * 0.45f, textPaint);
                
                // Stats
                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(getHeight() * 0.05f);
                canvas.drawText("POSSESSION", getWidth() / 2f, getHeight() * 0.60f, textPaint);
                textPaint.setColor(Color.LTGRAY);
                canvas.drawText(goT1Poss + "% - " + goT2Poss + "%", getWidth() / 2f, getHeight() * 0.67f, textPaint);
                
                textPaint.setColor(Color.WHITE);
                canvas.drawText("SHOTS", getWidth() / 2f, getHeight() * 0.77f, textPaint);
                textPaint.setColor(Color.LTGRAY);
                canvas.drawText(goT1Shots + " - " + goT2Shots, getWidth() / 2f, getHeight() * 0.84f, textPaint);

                // OK Button
                float bw = getWidth() * 0.2f;
                float bh = getHeight() * 0.12f;
                float bx = getWidth() * 0.85f - bw/2f;
                float by = getHeight() * 0.85f - bh/2f;
                Paint btnBg = new Paint(Paint.ANTI_ALIAS_FLAG);
                btnBg.setColor(gameOverButtonPressed ? Color.argb(180, 0, 180, 216) : Color.argb(110, 20, 30, 48));
                canvas.drawRoundRect(bx, by, bx+bw, by+bh, 16f, 16f, btnBg);
                Paint btnBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
                btnBorder.setStyle(Paint.Style.STROKE);
                btnBorder.setStrokeWidth(4f);
                btnBorder.setColor(Color.parseColor("#00E5FF"));
                canvas.drawRoundRect(bx, by, bx+bw, by+bh, 16f, 16f, btnBorder);
                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(bh * 0.4f);
                canvas.drawText("CONTINUE", bx + bw/2f, by + bh*0.65f, textPaint);
                
                return;
            }

            // MODERN FC MOBILE / PES 21 STYLE GLASSMORPHIC PAUSE MENU
            Paint overlay = new Paint(Paint.ANTI_ALIAS_FLAG);
            overlay.setColor(Color.argb(215, 10, 14, 26)); // Deep dark navy tint
            canvas.drawRect(0, 0, getWidth(), getHeight(), overlay);
            
            // Header Title
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(getHeight() * 0.08f);
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText("MATCH PAUSED", getWidth() * 0.10f, getHeight() * 0.18f, textPaint);
            
            // Subheader
            Paint subText = new Paint(Paint.ANTI_ALIAS_FLAG);
            subText.setColor(Color.parseColor("#00E5FF"));
            subText.setTextSize(getHeight() * 0.032f);
            subText.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText("SELECT AN OPTION TO CONTINUE", getWidth() * 0.10f, getHeight() * 0.24f, subText);
            
            float btnW = getWidth() * 0.42f;
            float btnH = getHeight() * 0.10f;
            float startX = getWidth() * 0.52f;
            float startY = getHeight() * 0.12f;
            float spacing = getHeight() * 0.125f;
            
            String[] menuItems = {"RESUME MATCH", "SQUAD MANAGEMENT", "MATCH SETTINGS", "CAMERA SETTINGS", "COMMAND LIST", "FORFEIT MATCH"};
            
            for (int i = 0; i < menuItems.length; i++) {
                float y = startY + (i * spacing);
                boolean isPressed = (pressedMenuIndex == i);
                
                // Glass Card Background
                Paint cardBg = new Paint(Paint.ANTI_ALIAS_FLAG);
                cardBg.setColor(Color.argb(isPressed ? 180 : 100, 26, 35, 50));
                canvas.drawRoundRect(startX, y, startX + btnW, y + btnH, 20f, 20f, cardBg);
                
                // Glass Border
                Paint cardBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
                cardBorder.setStyle(Paint.Style.STROKE);
                cardBorder.setStrokeWidth(isPressed ? 5f : 3f);
                cardBorder.setColor(isPressed ? Color.parseColor("#00E5FF") : Color.argb(120, 42, 58, 78));
                canvas.drawRoundRect(startX, y, startX + btnW, y + btnH, 20f, 20f, cardBorder);
                
                // Left Accent Indicator Bar
                Paint accentBar = new Paint(Paint.ANTI_ALIAS_FLAG);
                accentBar.setColor(i == 5 ? Color.parseColor("#FF1744") : (isPressed ? Color.parseColor("#00E5FF") : Color.parseColor("#00E676")));
                canvas.drawRoundRect(startX, y + btnH * 0.2f, startX + 12f, y + btnH * 0.8f, 6f, 6f, accentBar);
                
                // Text
                textPaint.setTextSize(btnH * 0.40f);
                textPaint.setTextAlign(Paint.Align.LEFT);
                textPaint.setColor(isPressed ? Color.parseColor("#00E5FF") : Color.WHITE);
                canvas.drawText(menuItems[i], startX + 32f, y + btnH * 0.62f, textPaint);
            }
            return;
        }

        // ── Draw Match Mode HUD Controls ──
        
        // 1. Draw Virtual Joystick Outer Base (Frosted Radial Glass)
        bgPaint.setShader(new RadialGradient(joyBaseX, joyBaseY, joyRadius,
            Color.argb(90, 40, 50, 70), Color.argb(20, 10, 15, 25), Shader.TileMode.CLAMP));
        canvas.drawCircle(joyBaseX, joyBaseY, joyRadius, bgPaint);
        
        // Outer Glass Border
        btnBorderPaint.setColor(joyPointerId != -1 ? Color.parseColor("#00E5FF") : Color.argb(80, 200, 220, 255));
        btnBorderPaint.setStrokeWidth(joyPointerId != -1 ? 6f : 3f);
        canvas.drawCircle(joyBaseX, joyBaseY, joyRadius, btnBorderPaint);

        // 8 Directional Compass Ticks
        Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setColor(Color.argb(120, 255, 255, 255));
        tickPaint.setStrokeWidth(4f);
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            float innerR = joyRadius * 0.82f;
            float outerR = joyRadius * 0.96f;
            float x1 = joyBaseX + (float)(Math.cos(angle) * innerR);
            float y1 = joyBaseY + (float)(Math.sin(angle) * innerR);
            float x2 = joyBaseX + (float)(Math.cos(angle) * outerR);
            float y2 = joyBaseY + (float)(Math.sin(angle) * outerR);
            canvas.drawLine(x1, y1, x2, y2, tickPaint);
        }

        // Active Vector Guide Line
        if (joyPointerId != -1) {
            Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            linePaint.setColor(Color.argb(160, 0, 229, 255));
            linePaint.setStrokeWidth(6f);
            canvas.drawLine(joyBaseX, joyBaseY, joyHandleX, joyHandleY, linePaint);
        }

        // 2. Draw Joystick Center Handle
        float hScale = joyPointerId != -1 ? 1.12f : 1.0f;
        handlePaint.setShader(new RadialGradient(joyHandleX, joyHandleY, joyHandleRadius * hScale,
            joyPointerId != -1 ? Color.parseColor("#00E5FF") : Color.argb(240, 240, 245, 255),
            Color.argb(180, 100, 140, 180), Shader.TileMode.CLAMP));
        canvas.drawCircle(joyHandleX, joyHandleY, joyHandleRadius * hScale, handlePaint);
        
        // Handle Border & Center Gem
        btnBorderPaint.setColor(joyPointerId != -1 ? Color.WHITE : Color.argb(200, 160, 200, 240));
        btnBorderPaint.setStrokeWidth(4f);
        canvas.drawCircle(joyHandleX, joyHandleY, joyHandleRadius * hScale, btnBorderPaint);

        Paint gemPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gemPaint.setColor(Color.parseColor("#00E5FF"));
        canvas.drawCircle(joyHandleX, joyHandleY, joyHandleRadius * 0.25f, gemPaint);

        // 3. Draw Action Buttons
        for (ActionButton btn : buttons) {
            float scale = btn.isPressed ? 0.90f : 1.0f;
            float r = btn.radius * scale;

            if (btn.keycodeOffense == 7) {
                // Modern Pause Button Card
                float bw = r * 2.2f;
                float bh = r * 1.5f;
                float left = btn.cx - bw / 2f;
                float top = btn.cy - bh / 2f;
                float right = btn.cx + bw / 2f;
                float bottom = btn.cy + bh / 2f;
                
                Paint pauseBg = new Paint(Paint.ANTI_ALIAS_FLAG);
                pauseBg.setColor(Color.argb(btn.isPressed ? 220 : 140, 20, 30, 45));
                canvas.drawRoundRect(left, top, right, bottom, r * 0.35f, r * 0.35f, pauseBg);
                
                Paint pauseBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
                pauseBorder.setStyle(Paint.Style.STROKE);
                pauseBorder.setStrokeWidth(3f);
                pauseBorder.setColor(btn.isPressed ? Color.parseColor("#00E5FF") : Color.argb(120, 0, 229, 255));
                canvas.drawRoundRect(left, top, right, bottom, r * 0.35f, r * 0.35f, pauseBorder);
                
                // Pause Icon Bars
                Paint pauseBars = new Paint(Paint.ANTI_ALIAS_FLAG);
                pauseBars.setColor(btn.isPressed ? Color.parseColor("#00E5FF") : Color.WHITE);
                float barW = r * 0.16f;
                float barH = r * 0.50f;
                float barSpacing = r * 0.16f;
                float barTop = btn.cy - barH / 2f;
                float barBottom = btn.cy + barH / 2f;
                
                canvas.drawRoundRect(btn.cx - barSpacing - barW, barTop, btn.cx - barSpacing, barBottom, 4f, 4f, pauseBars);
                canvas.drawRoundRect(btn.cx + barSpacing, barTop, btn.cx + barSpacing + barW, barBottom, 4f, 4f, pauseBars);
                
            } else {
                // Glassmorphic Action Buttons
                int baseCol = btn.isPressed ? Color.argb(180, 0, 180, 216) : Color.argb(110, 20, 30, 48);
                int borderCol = btn.isPressed ? Color.parseColor("#00E5FF") : Color.argb(160, 80, 140, 200);

                btnBgPaint.setShader(new RadialGradient(btn.cx, btn.cy, r, baseCol, Color.argb(60, 10, 15, 25), Shader.TileMode.CLAMP));
                canvas.drawCircle(btn.cx, btn.cy, r, btnBgPaint);
                
                btnBorderPaint.setColor(borderCol);
                btnBorderPaint.setStrokeWidth(btn.isPressed ? 6f : 3.5f);
                canvas.drawCircle(btn.cx, btn.cy, r, btnBorderPaint);
                
                // Action Labels are REMOVED entirely per user request for clean UI
                /*
                // Primary Action Label
                String mainLabel = isAttacking ? btn.labelOffense : btn.labelDefense;
                textPaint.setTextSize(r * 0.38f);
                textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                textPaint.setColor(btn.isPressed ? Color.parseColor("#00E5FF") : Color.WHITE);
                canvas.drawText(mainLabel.toUpperCase(), btn.cx, btn.cy - (r * 0.05f), textPaint);

                // Secondary Sub-label (Mode Indicator)
                String subLabel = isAttacking ? btn.labelDefense : btn.labelOffense;
                if (!subLabel.equalsIgnoreCase(mainLabel)) {
                    Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    subPaint.setTextAlign(Paint.Align.CENTER);
                    subPaint.setTextSize(r * 0.22f);
                    subPaint.setTypeface(Typeface.DEFAULT);
                    subPaint.setColor(Color.argb(160, 0, 229, 255));
                    canvas.drawText(subLabel.toUpperCase(), btn.cx, btn.cy + (r * 0.35f), subPaint);
                }
                */
            }
        }
    }

    public boolean isAttacking = true;

    public void setAttacking(boolean attacking) {
        if (this.isAttacking != attacking) {
            this.isAttacking = attacking;
            postInvalidate();
        }
    }

    private boolean isMatchMode = false;
    
    public void setMatchMode(boolean matchMode) {
        this.isMatchMode = matchMode;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        if (!isMatchMode) {
            if (isGameOver) {
                float bw = getWidth() * 0.2f;
                float bh = getHeight() * 0.12f;
                float bx = getWidth() * 0.85f - bw/2f;
                float by = getHeight() * 0.85f - bh/2f;
                
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        if (x >= bx && x <= bx + bw && y >= by && y <= by + bh) {
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
                            isGameOver = false; // exit game over overlay
                            invalidate();
                            
                            // Send Enter key (13) to trigger C++ Gui2Button OnClick
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

            float btnW = getWidth() * 0.4f;
            float btnH = getHeight() * 0.11f;
            float startX = getWidth() * 0.55f;
            float startY = getHeight() * 0.12f;
            float spacing = getHeight() * 0.13f;

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
                            // Resume Match
                            SDLActivity.setVirtualKey(27, true);
                            postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    SDLActivity.setVirtualKey(27, false);
                                }
                            }, 50);
                        } else if (index == 1) {
                            // Squad Management
                            getContext().startActivity(new android.content.Intent(getContext(), org.libsdl.app.ui.GamePlanActivity.class));
                        } else if (index == 2) {
                            // Match Settings
                            getContext().startActivity(new android.content.Intent(getContext(), org.libsdl.app.ui.SettingsActivity.class));
                        } else if (index == 3) {
                            // Camera Settings
                            getContext().startActivity(new android.content.Intent(getContext(), org.libsdl.app.ui.CameraSettingsActivity.class));
                        } else if (index == 4) {
                            // Command List
                            getContext().startActivity(new android.content.Intent(getContext(), org.libsdl.app.ui.CommandListActivity.class));
                        } else if (index == 5) {
                            // Forfeit Match Dialog
                            new android.app.AlertDialog.Builder(getContext())
                                .setTitle("FORFEIT MATCH")
                                .setMessage("Are you sure you want to forfeit this match?")
                                .setPositiveButton("YES, FORFEIT", new android.content.DialogInterface.OnClickListener() {
                                    public void onClick(android.content.DialogInterface dialog, int which) {
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

        // MATCH MODE
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (x < getWidth() * 0.45f && joyPointerId == -1) {
                    // Joystick area - make it floating!
                    joyPointerId = pointerId;
                    isJoyFloating = true;
                    joyBaseX = x;
                    joyBaseY = y;
                    joyHandleX = x;
                    joyHandleY = y;
                    updateJoystick(x, y);
                } else {
                    // Button area
                    for (ActionButton btn : buttons) {
                        float dx = x - btn.cx;
                        float dy = y - btn.cy;
                        if (dx * dx + dy * dy <= btn.radius * btn.radius * 2.5f && btn.pointerId == -1) {
                            btn.pointerId = pointerId;
                            btn.isPressed = true;
                            if (btn.keycodeOffense == 7) {
                                SDLActivity.setVirtualKey(27, true);
                            } else {
                                SDLActivity.setVirtualJoystickButton(isAttacking ? btn.keycodeOffense : btn.keycodeDefense, true);
                            }
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
                            final int keycode = isAttacking ? btn.keycodeOffense : btn.keycodeDefense;
                            if (btn.keycodeOffense == 7) {
                                postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        SDLActivity.setVirtualKey(27, false);
                                    }
                                }, 200);
                            } else {
                                postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        SDLActivity.setVirtualJoystickButton(keycode, false);
                                        // Also clear both just in case state changed while held
                                        SDLActivity.setVirtualJoystickButton(btn.keycodeOffense, false);
                                        SDLActivity.setVirtualJoystickButton(btn.keycodeDefense, false);
                                    }
                                }, 50);
                            }
                        }
                    }
                }
                break;
        }

        invalidate();
        return true;
    }
    
    private void updateMenuDpad(float dx, float dy, float maxDist) {
        // Obsolete, removed.
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

        // Deadzone 15%
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
