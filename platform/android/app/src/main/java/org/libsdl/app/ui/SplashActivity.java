package org.libsdl.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.util.TypedValue;

import org.libsdl.app.R;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BlurzTheme.applyImmersive(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#0A0E1A")); // Navy/dark background

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setGravity(Gravity.CENTER);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.blurz_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        
        // Add logo with reasonable size constraints
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(
            BlurzTheme.dpToPx(this, 250), BlurzTheme.dpToPx(this, 250)
        );
        main.addView(logo, logoParams);

        TextView title = new TextView(this);
        title.setText("BLURZ FOOTBALL");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        title.setTypeface(BlurzTheme.FONT_BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, BlurzTheme.dpToPx(this, 16), 0, 0);
        main.addView(title);

        root.addView(main, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);

        // Wait 2.5 seconds, then go to Main Menu
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainMenuActivity.class));
            finish();
        }, 2500);
    }
}
