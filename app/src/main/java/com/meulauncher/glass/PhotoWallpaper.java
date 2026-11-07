package com.meulauncher.glass;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Displays a portrait photo on a landscape TV without stretching or cropping it. */
public final class PhotoWallpaper extends Drawable {
    private final Bitmap bitmap;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    public PhotoWallpaper(Bitmap bitmap) { this.bitmap = bitmap; }
    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        canvas.drawColor(Color.rgb(12, 16, 32));
        if (bitmap == null || bitmap.isRecycled()) return;
        float width = bounds.width(), height = bounds.height();
        if (width <= 0 || height <= 0) return;
        // Blurred-looking dim side fill is approximated with a dark background.
        float scale = Math.min(width / bitmap.getWidth(), height / bitmap.getHeight());
        float drawnWidth = bitmap.getWidth() * scale;
        float drawnHeight = bitmap.getHeight() * scale;
        float x = bounds.left + (width - drawnWidth) / 2f;
        float y = bounds.top + (height - drawnHeight) / 2f;
        paint.setAlpha(255);
        canvas.drawBitmap(bitmap, null, new RectF(x, y, x + drawnWidth, y + drawnHeight), paint);
        paint.setColor(Color.argb(45, 0, 0, 18));
        canvas.drawRect(bounds, paint);
        paint.setColor(Color.WHITE);
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}