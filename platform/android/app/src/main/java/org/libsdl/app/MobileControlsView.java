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
        buttons.add(new ActionButton(w * 0.95f, h * 0.12f, normalRad * 0.7f, "Pause", "Pause", 7, 7, Color.argb(80, 50, 50, 50), borderCol));

        textPaint.setTextSize(normalRad * 0.45f);
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
                            SDLActivity.setVirtualJoystickButton(0, true); // Button A = Activate
                        }
                        // ESCAPE (Top)
                        else if (Math.hypot(x - btnBaseX, y - (btnBaseY - btnSpacing * 0.8f)) < btnRad * 2.5f) {
                            SDLActivity.setVirtualJoystickButton(1, true); // Button B = Escape
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
                        SDLActivity.setVirtualJoystickButton(0, false);
                        SDLActivity.setVirtualJoystickButton(1, false);
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
                            SDLActivity.setVirtualJoystickButton(isAttacking ? btn.keycodeOffense : btn.keycodeDefense, true);
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
                break;
        }

        invalidate();
        return true;
    }
    
    private void updateMenuDpad(float dx, float dy, float maxDist) {
        if (maxDist > 0 && Math.hypot(dx, dy) > maxDist * 0.2f) {
            float normX = dx / maxDist;
            float normY = dy / maxDist;
            SDLActivity.setVirtualJoystickAxis(0, normX);
            SDLActivity.setVirtualJoystickAxis(1, normY);
        } else {
            SDLActivity.setVirtualJoystickAxis(0, 0f);
            SDLActivity.setVirtualJoystickAxis(1, 0f);
        }
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
            SDLActivity.setVirtualJoystickAxis(0, 0f);
            SDLActivity.setVirtualJoystickAxis(1, 0f);
        } else {
            SDLActivity.setVirtualJoystickAxis(0, dx / joyRadius);
            SDLActivity.setVirtualJoystickAxis(1, dy / joyRadius);
        }
    }
    
    private void updateJoystickKeys(float dx, float dy) {
        // Obsolete, removed.
    }
}
