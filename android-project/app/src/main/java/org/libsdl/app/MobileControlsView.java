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

    // Active keys from joystick
    private boolean joyUp, joyDown, joyLeft, joyRight;

    // Buttons
    private class ActionButton {
        float cx, cy, radius;
        String labelOffense;
        String labelDefense;
        int keycode;
        int pointerId = -1;
        boolean isPressed = false;
        int colorBase;
        int colorBorder;

        ActionButton(float cx, float cy, float radius, String labelOffense, String labelDefense, int keycode, int colorBase, int colorBorder) {
            this.cx = cx;
            this.cy = cy;
            this.radius = radius;
            this.labelOffense = labelOffense;
            this.labelDefense = labelDefense;
            this.keycode = keycode;
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
        bgPaint.setColor(Color.argb(70, 0, 0, 0));
        bgPaint.setStyle(Paint.Style.FILL);

        handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handlePaint.setColor(Color.argb(180, 50, 200, 255));
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
        joyBaseX = w * 0.15f;
        joyBaseY = h * 0.75f;
        joyHandleX = joyBaseX;
        joyHandleY = joyBaseY;

        joyRadius = h * 0.16f; // Smaller
        joyHandleRadius = joyRadius * 0.4f;

        // Buttons on the right (Diamond layout)
        buttons.clear();
        float btnBaseX = w * 0.85f;
        float btnBaseY = h * 0.75f;
        float btnSpacing = joyRadius * 1.3f;
        float btnRad = h * 0.08f; // Smaller

        // BOTTOM: PASS (S) / PRESS
        buttons.add(new ActionButton(btnBaseX, btnBaseY + btnSpacing * 0.8f, btnRad, "PASS", "PRESS", 115, Color.argb(100, 0, 150, 50), Color.rgb(50, 240, 110)));
        // LEFT: THRU (W) / SWITCH
        buttons.add(new ActionButton(btnBaseX - btnSpacing, btnBaseY, btnRad, "THRU", "SWITCH", 119, Color.argb(100, 0, 100, 200), Color.rgb(40, 210, 250)));
        // RIGHT: SHOOT (D) / TACKLE
        buttons.add(new ActionButton(btnBaseX + btnSpacing * 0.8f, btnBaseY - btnSpacing * 0.2f, btnRad * 1.15f, "SHOOT", "TACKLE", 100, Color.argb(100, 200, 20, 20), Color.rgb(250, 60, 80)));
        // TOP: SPRINT (E) / DASH
        buttons.add(new ActionButton(btnBaseX - btnSpacing * 0.2f, btnBaseY - btnSpacing * 1.1f, btnRad, "SPRINT", "DASH", 101, Color.argb(100, 200, 120, 0), Color.rgb(250, 180, 40)));
        // TOP RIGHT: PAUSE (ESCAPE)
        buttons.add(new ActionButton(w * 0.9f, h * 0.12f, btnRad * 0.8f, "PAUSE", "PAUSE", 27, Color.argb(100, 100, 100, 100), Color.rgb(200, 200, 200)));

        textPaint.setTextSize(btnRad * 0.45f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isMatchMode) {
            // Draw Menu D-PAD
            float dpadX = getWidth() * 0.2f;
            float dpadY = getHeight() * 0.7f;
            float dpadRad = getHeight() * 0.25f;
            
            bgPaint.setShader(new RadialGradient(dpadX, dpadY, dpadRad, Color.argb(80, 20, 20, 20), Color.argb(20, 20, 20, 20), Shader.TileMode.CLAMP));
            canvas.drawCircle(dpadX, dpadY, dpadRad, bgPaint);
            
            // Draw D-PAD Arrows/Labels
            textPaint.setTextSize(dpadRad * 0.2f);
            textPaint.setColor(Color.argb(150, 255, 255, 255));
            canvas.drawText("UP", dpadX, dpadY - dpadRad * 0.6f, textPaint);
            canvas.drawText("DOWN", dpadX, dpadY + dpadRad * 0.8f, textPaint);
            canvas.drawText("LEFT", dpadX - dpadRad * 0.6f, dpadY + dpadRad * 0.1f, textPaint);
            canvas.drawText("RIGHT", dpadX + dpadRad * 0.6f, dpadY + dpadRad * 0.1f, textPaint);

            // Draw Menu Buttons (Enter / Back)
            float btnBaseX = getWidth() * 0.85f;
            float btnBaseY = getHeight() * 0.75f;
            float btnRad = getHeight() * 0.08f;
            float btnSpacing = getHeight() * 0.16f;

            // ENTER button (bottom)
            btnBgPaint.setShader(new RadialGradient(btnBaseX, btnBaseY + btnSpacing * 0.5f, btnRad, Color.argb(100, 0, 150, 50), Color.argb(50, 20, 20, 20), Shader.TileMode.CLAMP));
            canvas.drawCircle(btnBaseX, btnBaseY + btnSpacing * 0.5f, btnRad, btnBgPaint);
            canvas.drawText("ENTER", btnBaseX, btnBaseY + btnSpacing * 0.5f + dpadRad * 0.05f, textPaint);

            // ESCAPE button (top)
            btnBgPaint.setShader(new RadialGradient(btnBaseX, btnBaseY - btnSpacing * 0.8f, btnRad, Color.argb(100, 200, 20, 20), Color.argb(50, 20, 20, 20), Shader.TileMode.CLAMP));
            canvas.drawCircle(btnBaseX, btnBaseY - btnSpacing * 0.8f, btnRad, btnBgPaint);
            canvas.drawText("BACK", btnBaseX, btnBaseY - btnSpacing * 0.8f + dpadRad * 0.05f, textPaint);

            return;
        }

        // Draw Match Mode
        // Draw Joystick Base
        bgPaint.setShader(new RadialGradient(joyBaseX, joyBaseY, joyRadius, Color.argb(80, 20, 20, 20), Color.argb(20, 20, 20, 20), Shader.TileMode.CLAMP));
        canvas.drawCircle(joyBaseX, joyBaseY, joyRadius, bgPaint);

        // Draw Joystick Handle
        float hScale = joyPointerId != -1 ? 1.1f : 1.0f;
        handlePaint.setShader(new RadialGradient(joyHandleX, joyHandleY, joyHandleRadius * hScale, Color.argb(220, 100, 220, 255), Color.argb(150, 40, 180, 240), Shader.TileMode.CLAMP));
        canvas.drawCircle(joyHandleX, joyHandleY, joyHandleRadius * hScale, handlePaint);

        // Draw Buttons
        for (ActionButton btn : buttons) {
            float scale = btn.isPressed ? 0.85f : 1.0f;
            float r = btn.radius * scale;

            btnBgPaint.setShader(new RadialGradient(btn.cx, btn.cy, r, btn.colorBase, Color.argb(50, 20, 20, 20), Shader.TileMode.CLAMP));
            canvas.drawCircle(btn.cx, btn.cy, r, btnBgPaint);

            btnBorderPaint.setColor(btn.colorBorder);
            btnBorderPaint.setAlpha(btn.isPressed ? 255 : 180);
            canvas.drawCircle(btn.cx, btn.cy, r, btnBorderPaint);

            // Draw Offense label (Main)
            textPaint.setTextSize(r * 0.45f);
            textPaint.setColor(Color.WHITE);
            canvas.drawText(btn.labelOffense, btn.cx, btn.cy - (r * 0.1f), textPaint);
            
            // Draw Defense label (Alternative, slightly smaller below)
            textPaint.setTextSize(r * 0.35f);
            textPaint.setColor(Color.argb(200, 255, 255, 255));
            canvas.drawText(btn.labelDefense, btn.cx, btn.cy + (r * 0.4f), textPaint);
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
            // MENU MODE (D-PAD + ENTER/BACK)
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    // D-PAD Left (Up, Down, Left, Right)
                    float dpadX = getWidth() * 0.2f;
                    float dpadY = getHeight() * 0.7f;
                    float dpadRad = getHeight() * 0.25f;
                    if (x < getWidth() * 0.5f) {
                        joyPointerId = pointerId;
                        updateMenuDpad(x - dpadX, y - dpadY, dpadRad);
                    } else {
                        // Buttons Right (Enter, Esc)
                        float btnBaseX = getWidth() * 0.85f;
                        float btnBaseY = getHeight() * 0.75f;
                        float btnRad = getHeight() * 0.08f;
                        float btnSpacing = getHeight() * 0.16f;
                        
                        // ENTER (Bottom)
                        if (Math.hypot(x - btnBaseX, y - (btnBaseY + btnSpacing * 0.5f)) < btnRad * 2.5f) {
                            SDLActivity.setVirtualKey(13, true); // SDLK_RETURN
                        }
                        // ESCAPE (Top)
                        else if (Math.hypot(x - btnBaseX, y - (btnBaseY - btnSpacing * 0.8f)) < btnRad * 2.5f) {
                            SDLActivity.setVirtualKey(27, true); // SDLK_ESCAPE
                        }
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    for (int i = 0; i < event.getPointerCount(); i++) {
                        int pId = event.getPointerId(i);
                        if (pId == joyPointerId) {
                            updateMenuDpad(event.getX(i) - getWidth() * 0.2f, event.getY(i) - getHeight() * 0.7f, getHeight() * 0.25f);
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (pointerId == joyPointerId) {
                        joyPointerId = -1;
                        updateMenuDpad(0, 0, 0);
                    } else {
                        SDLActivity.setVirtualKey(13, false); // SDLK_RETURN
                        SDLActivity.setVirtualKey(27, false); // SDLK_ESCAPE
                    }
                    break;
            }
            invalidate();
            return true;
        }

        // MATCH MODE
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (x < getWidth() * 0.4f && joyPointerId == -1) {
                    // Joystick area
                    joyPointerId = pointerId;
                    updateJoystick(x, y);
                } else {
                    // Button area
                    for (ActionButton btn : buttons) {
                        float dx = x - btn.cx;
                        float dy = y - btn.cy;
                        if (dx * dx + dy * dy <= btn.radius * btn.radius * 2.5f && btn.pointerId == -1) {
                            btn.pointerId = pointerId;
                            btn.isPressed = true;
                            SDLActivity.setVirtualKey(btn.keycode, true);
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
                    joyHandleX = joyBaseX;
                    joyHandleY = joyBaseY;
                    updateJoystickKeys(0, 0); // Release all
                } else {
                    for (ActionButton btn : buttons) {
                        if (btn.pointerId == pointerId) {
                            btn.pointerId = -1;
                            btn.isPressed = false;
                            SDLActivity.setVirtualKey(btn.keycode, false);
                        }
                    }
                }
                break;
        }

        invalidate();
        return true;
    }
    
    private void updateMenuDpad(float dx, float dy, float maxDist) {
        boolean up = false, down = false, left = false, right = false;
        if (maxDist > 0 && Math.hypot(dx, dy) > maxDist * 0.2f) {
            float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
            if (angle > -45 && angle <= 45) right = true;
            else if (angle > 45 && angle <= 135) down = true;
            else if (angle > 135 || angle <= -135) left = true;
            else if (angle > -135 && angle <= -45) up = true;
        }
        if (up != joyUp) { joyUp = up; SDLActivity.setVirtualKey(1073741906, up); }
        if (down != joyDown) { joyDown = down; SDLActivity.setVirtualKey(1073741905, down); }
        if (left != joyLeft) { joyLeft = left; SDLActivity.setVirtualKey(1073741904, left); }
        if (right != joyRight) { joyRight = right; SDLActivity.setVirtualKey(1073741903, right); }
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
