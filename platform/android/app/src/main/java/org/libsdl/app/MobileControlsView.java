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

        int baseCol = Color.argb(120, 30, 80, 30); // Dark green
        int borderCol = Color.argb(150, 150, 200, 150);
        
        // PASS (Left of Dash)
        buttons.add(new ActionButton(dashX - spacingX, dashY + h * 0.02f, normalRad, "Pass", "Switch", 0, 4, baseCol, borderCol));
        
        // DASH (Bottom Right)
        buttons.add(new ActionButton(dashX, dashY, dashRad, "Dash", "Dash", 5, 5, baseCol, borderCol));
        
        // THROUGH (Left of Shoot, Above Pass)
        buttons.add(new ActionButton(dashX - spacingX * 0.9f, dashY - spacingY, normalRad, "Through", "Press", 3, 0, baseCol, borderCol));
        
        // SHOOT (Above Dash)
        buttons.add(new ActionButton(dashX - w * 0.02f, dashY - spacingY - h * 0.02f, normalRad, "Shoot", "Clear", 2, 1, baseCol, borderCol));
        
        // TOP RIGHT CORNER: PAUSE
        buttons.add(new ActionButton(w * 0.95f, h * 0.06f, normalRad * 0.4f, "Pause", "Pause", 7, 7, Color.argb(80, 50, 50, 50), borderCol));

        textPaint.setTextSize(normalRad * 0.45f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isMatchMode) {
            // FULL TOUCH PES 21 STYLE PAUSE MENU
            Paint overlay = new Paint(Paint.ANTI_ALIAS_FLAG);
            overlay.setColor(Color.argb(190, 10, 15, 20));
            canvas.drawRect(0, 0, getWidth(), getHeight(), overlay);
            
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(getHeight() * 0.1f);
            textPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("MATCH PAUSED", getWidth() * 0.12f, getHeight() * 0.2f, textPaint);
            
            float btnW = getWidth() * 0.4f;
            float btnH = getHeight() * 0.11f;
            float startX = getWidth() * 0.55f;
            float startY = getHeight() * 0.12f;
            float spacing = getHeight() * 0.13f;
            
            String[] menuItems = {"RESUME MATCH", "SQUAD MANAGEMENT", "MATCH SETTINGS", "CAMERA SETTINGS", "COMMAND LIST", "FORFEIT MATCH"};
            
            for (int i = 0; i < menuItems.length; i++) {
                float y = startY + (i * spacing);
                
                Paint btnBg = new Paint(Paint.ANTI_ALIAS_FLAG);
                btnBg.setColor(Color.argb(pressedMenuIndex == i ? 180 : 80, 40, 80, 140));
                canvas.drawRect(startX, y, startX + btnW, y + btnH, btnBg);
                
                Paint btnBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
                btnBorder.setStyle(Paint.Style.STROKE);
                btnBorder.setStrokeWidth(3f);
                btnBorder.setColor(Color.argb(200, 100, 150, 255));
                canvas.drawRect(startX, y, startX + btnW, y + btnH, btnBorder);
                
                textPaint.setTextSize(btnH * 0.45f);
                textPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(menuItems[i], startX + btnW / 2, y + btnH * 0.65f, textPaint);
            }
            return;
        }

        // Draw Match Mode
        // Draw Joystick Base (More visible!)
        bgPaint.setShader(new RadialGradient(joyBaseX, joyBaseY, joyRadius, Color.argb(120, 60, 60, 60), Color.argb(40, 30, 30, 30), Shader.TileMode.CLAMP));
        canvas.drawCircle(joyBaseX, joyBaseY, joyRadius, bgPaint);
        
        // Draw Joystick Base Border
        btnBorderPaint.setColor(Color.argb(100, 200, 200, 200));
        btnBorderPaint.setAlpha(100);
        canvas.drawCircle(joyBaseX, joyBaseY, joyRadius, btnBorderPaint);

        // Draw Joystick Handle (More visible!)
        float hScale = joyPointerId != -1 ? 1.15f : 1.0f;
        handlePaint.setShader(new RadialGradient(joyHandleX, joyHandleY, joyHandleRadius * hScale, Color.argb(255, 255, 255, 255), Color.argb(200, 200, 200, 200), Shader.TileMode.CLAMP));
        canvas.drawCircle(joyHandleX, joyHandleY, joyHandleRadius * hScale, handlePaint);
        
        // Draw a slight border for the handle to make it pop
        btnBorderPaint.setColor(Color.argb(200, 100, 100, 100));
        btnBorderPaint.setAlpha(150);
        canvas.drawCircle(joyHandleX, joyHandleY, joyHandleRadius * hScale, btnBorderPaint);

        // Draw Buttons
        for (ActionButton btn : buttons) {
            float scale = btn.isPressed ? 0.85f : 1.0f;
            float r = btn.radius * scale;

            if (btn.keycodeOffense == 7) {
                // Custom drawing for the Pause button based on the reference image
                float bw = r * 2.2f;
                float bh = r * 1.5f;
                float left = btn.cx - bw / 2f;
                float top = btn.cy - bh / 2f;
                float right = btn.cx + bw / 2f;
                float bottom = btn.cy + bh / 2f;
                
                // Outer rounded rectangle (semi-transparent white)
                Paint pauseBg = new Paint(Paint.ANTI_ALIAS_FLAG);
                pauseBg.setColor(Color.argb(160, 240, 255, 240)); // Slightly greenish white tint like image
                pauseBg.setStyle(Paint.Style.FILL);
                if (btn.isPressed) pauseBg.setColor(Color.argb(200, 200, 255, 200));
                float cornerRadius = r * 0.3f;
                canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, pauseBg);
                
                // Inner dark green circle
                Paint pauseCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
                pauseCircle.setColor(Color.argb(150, 60, 90, 60)); // Dark green/grey tint
                pauseCircle.setStyle(Paint.Style.FILL);
                canvas.drawCircle(btn.cx, btn.cy, r * 0.7f, pauseCircle);
                
                // Two vertical black bars (Pause Icon)
                Paint pauseBars = new Paint(Paint.ANTI_ALIAS_FLAG);
                pauseBars.setColor(Color.argb(220, 20, 30, 20));
                pauseBars.setStyle(Paint.Style.FILL);
                
                float barW = r * 0.15f;
                float barH = r * 0.5f;
                float barSpacing = r * 0.18f;
                
                float barTop = btn.cy - barH / 2f;
                float barBottom = btn.cy + barH / 2f;
                
                // Left bar
                canvas.drawRoundRect(btn.cx - barSpacing - barW, barTop, btn.cx - barSpacing, barBottom, barW/3, barW/3, pauseBars);
                // Right bar
                canvas.drawRoundRect(btn.cx + barSpacing, barTop, btn.cx + barSpacing + barW, barBottom, barW/3, barW/3, pauseBars);
                
                // Draw a very thin horizontal line passing across the button like in the image
                Paint thinLine = new Paint(Paint.ANTI_ALIAS_FLAG);
                thinLine.setColor(Color.argb(200, 255, 255, 255));
                thinLine.setStrokeWidth(2f);
                canvas.drawLine(left - r*0.5f, btn.cy, btn.cx - barSpacing - barW - 2f, btn.cy, thinLine);
                canvas.drawLine(btn.cx + barSpacing + barW + 2f, btn.cy, right + r*0.5f, btn.cy, thinLine);
                
            } else {
                btnBgPaint.setShader(new RadialGradient(btn.cx, btn.cy, r, btn.colorBase, Color.argb(50, 20, 20, 20), Shader.TileMode.CLAMP));
                canvas.drawCircle(btn.cx, btn.cy, r, btnBgPaint);
                
                btnBorderPaint.setColor(btn.colorBorder);
                btnBorderPaint.setAlpha(btn.isPressed ? 255 : 180);
                canvas.drawCircle(btn.cx, btn.cy, r, btnBorderPaint);
                
                // Draw Label
                String label = isAttacking ? btn.labelOffense : btn.labelDefense;
                textPaint.setTextSize(r * 0.35f);
                textPaint.setColor(btn.isPressed ? Color.rgb(200, 200, 200) : Color.WHITE);
                canvas.drawText(label, btn.cx, btn.cy + (r * 0.1f), textPaint);
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
                    updateJoystickKeys(0, 0);
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

        // Deadzone 20%
        if (dist < joyRadius * 0.2f) {
            updateJoystickKeys(0, 0);
        } else {
            updateJoystickKeys(dx, dy);
        }
    }
    
    private void updateJoystickKeys(float dx, float dy) {
        boolean up = false, down = false, left = false, right = false;

        if (dx != 0 || dy != 0) {
            float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
            // 8-way directional mapping
            if (angle > -22.5 && angle <= 22.5) right = true;
            else if (angle > 22.5 && angle <= 67.5) { right = true; down = true; }
            else if (angle > 67.5 && angle <= 112.5) down = true;
            else if (angle > 112.5 && angle <= 157.5) { down = true; left = true; }
            else if (angle > 157.5 || angle <= -157.5) left = true;
            else if (angle > -157.5 && angle <= -112.5) { up = true; left = true; }
            else if (angle > -112.5 && angle <= -67.5) up = true;
            else if (angle > -67.5 && angle <= -22.5) { up = true; right = true; }
        }

        // SDLK_UP = 1073741906, SDLK_DOWN = 1073741905, SDLK_RIGHT = 1073741903, SDLK_LEFT = 1073741904
        if (up != joyUp) { joyUp = up; SDLActivity.setVirtualKey(1073741906, up); }
        if (down != joyDown) { joyDown = down; SDLActivity.setVirtualKey(1073741905, down); }
        if (left != joyLeft) { joyLeft = left; SDLActivity.setVirtualKey(1073741904, left); }
        if (right != joyRight) { joyRight = right; SDLActivity.setVirtualKey(1073741903, right); }
    }
}
