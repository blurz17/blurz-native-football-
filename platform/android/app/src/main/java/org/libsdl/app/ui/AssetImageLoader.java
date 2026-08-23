package org.libsdl.app.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;

/**
 * Loads PNG images from the assets directory into ImageViews.
 */
public class AssetImageLoader {

    private static final String TAG = "AssetImageLoader";

    /**
     * Load an image from assets into an ImageView.
     * @param context  Android context
     * @param view     Target ImageView
     * @param assetPath  Relative path inside assets/, e.g. "databases/default/images_teams/premierleague/arsenal_logo.png"
     */
    public static void load(Context context, ImageView view, String assetPath) {
        try {
            InputStream is = context.getAssets().open(assetPath);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            if (bmp != null) {
                view.setImageBitmap(bmp);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not load asset image: " + assetPath, e);
            // Leave the ImageView empty or with its default drawable
        }
    }

    /**
     * Load a bitmap from assets (returns null on failure).
     */
    public static Bitmap loadBitmap(Context context, String assetPath) {
        try {
            InputStream is = context.getAssets().open(assetPath);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            return bmp;
        } catch (Exception e) {
            Log.w(TAG, "Could not load asset bitmap: " + assetPath, e);
            return null;
        }
    }
}
