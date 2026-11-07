package com.meulauncher.glass;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private LinearLayout content;
    private SharedPreferences prefs;
    private boolean compact;
    private TextView clock;
    private TextView date;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (clock != null) clock.setText(new SimpleDateFormat("HH:mm", new Locale("pt", "BR")).format(new Date()));
            if (date != null) date.setText(new SimpleDateFormat("EEE, dd 'de' MMMM", new Locale("pt", "BR")).format(new Date()));
            handler.postDelayed(this, 30000);
        }
    };
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable glass() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.argb(194, 52, 57, 77));
        d.setCornerRadius(dp(23));
        d.setStroke(dp(1), Color.argb(115, 225, 232, 255));
        return d;
    }
    private TextView text(String value, int sp) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(Color.WHITE);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }
    private void focus(View v) {
        v.setFocusable(true);
        v.setOnFocusChangeListener((view, selected) -> {
            view.setAlpha(selected ? 1f : .90f);
            view.setScaleX(selected ? 1.045f : 1f);
            view.setScaleY(selected ? 1.045f : 1f);
            view.setElevation(selected ? dp(10) : 0);
        });
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = getSharedPreferences("launcher", MODE_PRIVATE);
        compact = prefs.getBoolean("compact", false);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setPadding(dp(24), dp(18), dp(24), dp(18));
        GradientDrawable backdrop = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(48, 34, 73), Color.rgb(18, 34, 64), Color.rgb(16, 20, 37)});
        root.setBackground(wallpaper(backdrop));

        LinearLayout sidebar = new LinearLayout(this);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams sideParams = new LinearLayout.LayoutParams(dp(218), ViewGroup.LayoutParams.MATCH_PARENT);
        sideParams.rightMargin = dp(18);
        root.addView(sidebar, sideParams);

        clock = text("", 43);
        sidebar.addView(clock);
        date = text("", 14);
        sidebar.addView(date);
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.VERTICAL);
        nav.setPadding(dp(10), dp(12), dp(10), dp(12));
        nav.setBackground(glass());
        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        navParams.topMargin = dp(16);
        sidebar.addView(nav, navParams);
        String[] names = {"⌂  Início", "▦  Apps", "▣  Filmes e Séries", "▣  TV ao Vivo", "◈  Jogos", "♫  Música", "⚙  Ajustes da TV", "✦  Personalizar"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            TextView item = text(names[i], 16);
            item.setPadding(dp(12), dp(11), dp(6), dp(11));
            focus(item);
            item.setOnClickListener(v -> {
                if (index == 7) showPersonalize();
                else if (index == 6) {
                    try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
                    catch (Exception e) { Toast.makeText(this, "Configurações indisponíveis", Toast.LENGTH_SHORT).show(); }
                } else if (index == 0) showHome();
                else if (index == 1) showAllApps();
                else showCategory(index);
            });
            nav.addView(item);
        }
        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setClipToPadding(false);
        root.addView(scroller, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(8), dp(5), dp(8), dp(18));
        scroller.addView(content);
        setContentView(root);
        handler.post(tick);
        showHome();
    }
    private Drawable wallpaper(Drawable fallback) {
        try {
            java.io.InputStream stream = getAssets().open("spiderman_wallpaper.jpg");
            android.graphics.Bitmap bitmap;
            try { bitmap = BitmapFactory.decodeStream(stream); }
            finally { stream.close(); }
            if (bitmap == null) return fallback;
            BitmapDrawable image = new BitmapDrawable(getResources(), bitmap);
            image.setGravity(Gravity.CENTER);
            image.setTileModeX(null);
            image.setTileModeY(null);
            return new LayerDrawable(new Drawable[] {
                new ColorDrawable(Color.rgb(8, 14, 30)), image,
                new ColorDrawable(Color.argb(52, 0, 0, 12))
            });
        } catch (Exception ignored) {
            return prefs.getBoolean("space", true) ? new SpaceWallpaper() : fallback;
        }
    }
    private void heading(String value) {
        TextView title = text(value, 21);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(43));
        p.topMargin = dp(10);
        content.addView(title, p);
    }
    private List<ApplicationInfo> apps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> all = new ArrayList<>();
        for (ApplicationInfo info : pm.getInstalledApplications(0)) {
            if (info.packageName.equals(getPackageName())) continue;
            if (pm.getLeanbackLaunchIntentForPackage(info.packageName) != null ||
                pm.getLaunchIntentForPackage(info.packageName) != null) all.add(info);
        }
        Collections.sort(all, Comparator.comparing(a -> pm.getApplicationLabel(a).toString().toLowerCase(Locale.ROOT)));
        return all;
    }
    private void grid(List<ApplicationInfo> items, int limit) {
        PackageManager pm = getPackageManager();
        LinearLayout row = null;
        int count = 0;
        for (ApplicationInfo info : items) {
            if (limit > 0 && count >= limit) break;
            if (count % (compact ? 7 : 6) == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                content.addView(row);
            }
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(5), dp(8), dp(5), dp(5));
            tile.setBackground(glass());
            LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, dp(compact ? 98 : 116), 1);
            tileParams.setMargins(dp(4), dp(4), dp(4), dp(4));
            row.addView(tile, tileParams);
            ImageView image = new ImageView(this);
            try { image.setImageDrawable(pm.getApplicationIcon(info)); } catch (Exception ignored) { }
            tile.addView(image, new LinearLayout.LayoutParams(dp(compact ? 46 : 59), dp(compact ? 46 : 59)));
            TextView label = text(pm.getApplicationLabel(info).toString(), 12);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            tile.addView(label);
            focus(tile);
            tile.setOnClickListener(v -> {
                Intent intent = pm.getLeanbackLaunchIntentForPackage(info.packageName);
                if (intent == null) intent = pm.getLaunchIntentForPackage(info.packageName);
                try {
                    if (intent != null) startActivity(intent);
                    else Toast.makeText(this, "App indisponível", Toast.LENGTH_SHORT).show();
                } catch (Exception e) { Toast.makeText(this, "Falha ao abrir aplicativo", Toast.LENGTH_SHORT).show(); }
            });
            tile.setOnLongClickListener(v -> { boolean old = prefs.getBoolean("fav_" + info.packageName, false); prefs.edit().putBoolean("fav_" + info.packageName, !old).apply(); Toast.makeText(this, old ? "Removido dos favoritos" : "Adicionado aos favoritos", Toast.LENGTH_SHORT).show(); showHome(); return true; });
            count++;
        }
        if (count == 0) content.addView(text("Nenhum aplicativo disponível nesta categoria.", 16));
    }
    private void showHome() {
        content.removeAllViews();
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(26), dp(16), dp(26), dp(16));
        hero.setBackground(glass());
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, dp(180));
        hp.bottomMargin = dp(12);
        content.addView(hero, hp);
        hero.addView(text("INÍCIO", 13));
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        TextView greeting = text(hour < 12 ? "Bom dia" : hour < 18 ? "Boa tarde" : "Boa noite", 35);
        greeting.setTypeface(null, Typeface.BOLD);
        hero.addView(greeting);
        hero.addView(text("Seus apps, do seu jeito, sem propaganda.", 16));
        TextView button = text("▶  Escolher um app", 15);
        button.setPadding(dp(16), dp(10), dp(16), dp(10));
        button.setBackground(glass());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(210), dp(48));
        bp.topMargin = dp(10);
        hero.addView(button, bp);
        focus(button);
        button.setOnClickListener(v -> showAllApps());
        heading("Seus aplicativos");
        List<ApplicationInfo> favorites = new ArrayList<>();
        for (ApplicationInfo app : apps()) if (prefs.getBoolean("fav_" + app.packageName, false)) favorites.add(app);
        if (favorites.isEmpty()) grid(apps(), 6); else grid(favorites, 12);
        heading("Todos os apps");
        grid(apps(), 18);
    }
    private void showAllApps() {
        content.removeAllViews();
        heading("Todos os apps");
        grid(apps(), 0);
    }
    private void showPersonalize() {
        content.removeAllViews();
        heading("Personalizar");
        TextView option = text((compact ? "☑" : "☐") + "  Ícones compactos", 19);
        option.setPadding(dp(22), dp(18), dp(22), dp(18));
        option.setBackground(glass());
        focus(option);
        option.setOnClickListener(v -> { compact = !compact; prefs.edit().putBoolean("compact", compact).apply(); showPersonalize(); });
        content.addView(option);
        TextView wallpaper = text((prefs.getBoolean("space", true) ? "☑" : "☐") + "  Fundo espacial", 19);
        wallpaper.setPadding(dp(22), dp(18), dp(22), dp(18));
        wallpaper.setBackground(glass());
        LinearLayout.LayoutParams wallpaperParams = new LinearLayout.LayoutParams(-1, -2);
        wallpaperParams.topMargin = dp(12);
        content.addView(wallpaper, wallpaperParams);
        focus(wallpaper);
        wallpaper.setOnClickListener(v -> {
            boolean next = !prefs.getBoolean("space", true);
            prefs.edit().putBoolean("space", next).apply();
            recreate();
        });
        TextView help = text("Segure OK sobre um aplicativo para adicionar ou remover dos favoritos.", 16);
        help.setPadding(dp(12), dp(28), dp(12), dp(12));
        content.addView(help);
    }
    private void showCategory(int index) {
        content.removeAllViews();
        String[] titles = {"", "", "Filmes e Séries", "TV ao Vivo", "Jogos", "Música"};
        heading(titles[index]);
        List<ApplicationInfo> matching = new ArrayList<>();
        for (ApplicationInfo app : apps()) {
            String name = getPackageManager().getApplicationLabel(app).toString().toLowerCase(Locale.ROOT);
            boolean ok = index == 2 ? name.matches(".*(netflix|prime|disney|max|globoplay|stremio|plex|youtube|filme).*")
                : index == 3 ? name.matches(".*(tv|pluto|canal|iptv).*")
                : index == 4 ? (app.category == ApplicationInfo.CATEGORY_GAME || name.matches(".*(game|jogo|xbox|steam|geforce).*"))
                : name.matches(".*(music|música|spotify|deezer|tidal|sound).*");
            if (ok) matching.add(app);
        }
        grid(matching, 0);
    }
    @Override protected void onDestroy() {
        handler.removeCallbacks(tick);
        super.onDestroy();
    }
}
