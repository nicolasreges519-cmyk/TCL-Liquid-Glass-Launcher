package com.meulauncher.glass;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import java.util.Random;

/** Lightweight, offline night-sky landscape; no external assets or permissions. */
public class SpaceWallpaper extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @Override public void draw(Canvas canvas) {
        android.graphics.Rect b = getBounds();
        float w = b.width(), h = b.height();
        paint.setShader(new LinearGradient(0, 0, w, h,
            new int[]{Color.rgb(13, 15, 40), Color.rgb(59, 38, 91), Color.rgb(12, 32, 55)},
            null, Shader.TileMode.CLAMP));
        canvas.drawRect(b, paint);
        paint.setShader(null);
        Random random = new Random(4107);
        for (int i = 0; i < 105; i++) {
            float x = random.nextFloat() * w;
            float y = random.nextFloat() * h * .65f;
            paint.setColor(Color.argb(60 + random.nextInt(155), 240, 244, 255));
            canvas.drawCircle(x, y, .5f + random.nextFloat() * 1.5f, paint);
        }
        paint.setColor(Color.argb(80, 190, 140, 255));
        canvas.drawCircle(w * .75f, h * .28f, Math.min(w, h) * .19f, paint);
        paint.setColor(Color.rgb(25, 28, 59));
        Path mountain = new Path();
        mountain.moveTo(0, h * .80f);
        mountain.lineTo(w * .12f, h * .58f);
        mountain.lineTo(w * .24f, h * .77f);
        mountain.lineTo(w * .44f, h * .48f);
        mountain.lineTo(w * .61f, h * .78f);
        mountain.lineTo(w * .78f, h * .57f);
        mountain.lineTo(w, h * .80f);
        mountain.lineTo(w, h);
        mountain.lineTo(0, h);
        mountain.close();
        canvas.drawPath(mountain, paint);
        paint.setColor(Color.rgb(11, 25, 45));
        canvas.drawRect(0, h * .85f, w, h, paint);
        paint.setColor(Color.argb(55, 183, 135, 220));
        for (int i = 0; i < 18; i++) {
            float y = h * (.86f + i * .008f);
            float span = w * (.11f + i * .025f);
            canvas.drawRect(w * .72f - span / 2, y, w * .72f + span / 2, y + 1.5f, paint);
        }
    }
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(android.graphics.ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
