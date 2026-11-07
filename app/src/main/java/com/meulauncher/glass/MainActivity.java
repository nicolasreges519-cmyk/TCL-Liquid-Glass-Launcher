package com.meulauncher.glass;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private LinearLayout content;
    private final int white = Color.WHITE;

    private GradientDrawable glass() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.argb(160, 68, 75, 88));
        d.setCornerRadius(24);
        d.setStroke(2, Color.argb(100, 255, 255, 255));
        return d;
    }

    private TextView label(String text, int size) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(white);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(18, 14, 18, 14);
        return t;
    }

    private void focusStyle(View v) {
        v.setFocusable(true);
        v.setOnFocusChangeListener((view, focused) -> {
            view.setAlpha(focused ? 1f : .83f);
            view.setScaleX(focused ? 1.035f : 1f);
            view.setScaleY(focused ? 1.035f : 1f);
        });
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setPadding(20, 20, 20, 20);
        root.setBackgroundColor(Color.rgb(19, 23, 32));

        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setBackground(glass());
        String[] names = {"Início", "Meus Apps", "Configurações"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            TextView b = label(names[i], 18);
            focusStyle(b);
            b.setOnClickListener(v -> {
                if (index == 2) {
                    try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
                    catch (Exception e) { showApps(); }
                } else showApps();
            });
            menu.addView(b);
        }
        root.addView(menu, new LinearLayout.LayoutParams(230, ViewGroup.LayoutParams.MATCH_PARENT));

        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        scrollParams.leftMargin = 20;
        root.addView(scroll, scrollParams);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        setContentView(root);
        showApps();
    }

    private void showApps() {
        content.removeAllViews();
        TextView title = label("TCL Google TV", 28);
        title.setTypeface(null, Typeface.BOLD);
        content.addView(title);
        TextView subtitle = label("Seus aplicativos", 21);
        content.addView(subtitle);

        PackageManager pm = getPackageManager();
        List<ApplicationInfo> installed = new ArrayList<>(pm.getInstalledApplications(0));
        Collections.sort(installed, Comparator.comparing(a -> pm.getApplicationLabel(a).toString().toLowerCase()));
        LinearLayout row = null;
        int count = 0;
        for (ApplicationInfo info : installed) {
            Intent launch = pm.getLeanbackLaunchIntentForPackage(info.packageName);
            if (launch == null) launch = pm.getLaunchIntentForPackage(info.packageName);
            if (launch == null || info.packageName.equals(getPackageName())) continue;
            final Intent target = launch;
            if (count % 4 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                content.addView(row);
            }
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(10, 14, 10, 14);
            tile.setBackground(glass());
            LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, 150, 1);
            tileParams.setMargins(5, 5, 5, 5);
            row.addView(tile, tileParams);
            try {
                ImageView icon = new ImageView(this);
                Drawable drawable = pm.getApplicationIcon(info);
                icon.setImageDrawable(drawable);
                tile.addView(icon, new LinearLayout.LayoutParams(68, 68));
            } catch (Exception ignored) { }
            TextView name = label(pm.getApplicationLabel(info).toString(), 14);
            name.setGravity(Gravity.CENTER);
            name.setMaxLines(2);
            tile.addView(name);
            focusStyle(tile);
            tile.setOnClickListener(v -> {
                try { startActivity(target); }
                catch (Exception e) {
                    android.widget.Toast.makeText(this, "Não foi possível abrir o aplicativo", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
            count++;
        }
        if (count == 0) content.addView(label("Nenhum aplicativo visível. Verifique as permissões do launcher.", 18));
    }
}
