package com.meulauncher.glass;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.AtomicFile;
import android.graphics.drawable.Drawable;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {
    private LinearLayout content;
    private ScrollView scroller;
    private TextView homeMenu;
    private int sidebarWidth;
    private boolean useAppBanners;
    private String pendingCover;
    private boolean writingCover;
    private static final int PICK_COVER = 1;
    private List<ApplicationInfo> installedApps;
    private SharedPreferences prefs;
    private boolean compact;
    private boolean showLabels;
    private int page = 0;
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
        d.setColors(new int[]{Color.argb(203, 70, 77, 102), Color.argb(166, 30, 37, 62)});
        d.setOrientation(GradientDrawable.Orientation.TL_BR);
        d.setCornerRadius(dp(23));
        d.setStroke(dp(1), Color.argb(130, 225, 232, 255));
        return d;
    }
    private GradientDrawable selectedGlass() {
        GradientDrawable d = glass();
        d.setStroke(dp(2), Color.rgb(149, 213, 255));
        d.setColors(new int[]{Color.argb(225, 91, 111, 151), Color.argb(203, 49, 64, 98)});
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
        v.setBackground(glass());
        v.setOnFocusChangeListener((view, selected) -> {
            view.setBackground(selected ? selectedGlass() : glass());
            if (view instanceof FrameLayout) {
                GradientDrawable outline = selectedGlass();
                outline.setColor(Color.TRANSPARENT);
                view.setForeground(selected ? outline : null);
            }
            view.setAlpha(selected ? 1f : .90f);
            view.setElevation(selected ? dp(10) : 0);
            view.animate().setDuration(135).scaleX(selected ? 1.045f : 1f).scaleY(selected ? 1.045f : 1f).start();
        });
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = getSharedPreferences("launcher", MODE_PRIVATE);
        compact = prefs.getBoolean("compact", false);
        showLabels = prefs.getBoolean("show_labels", false);
        useAppBanners = prefs.getBoolean("app_banners", true);
        sidebarWidth = Math.min(204, Math.max(146, getResources().getConfiguration().screenWidthDp / 5));
        installedApps = queryApps();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setPadding(dp(22), dp(18), dp(22), dp(18));
        GradientDrawable backdrop = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(48, 34, 73), Color.rgb(18, 34, 64), Color.rgb(16, 20, 37)});
        root.setBackground(prefs.getBoolean("photo", true) ? wallpaper(backdrop) : new SpaceWallpaper());

        LinearLayout sidebar = new LinearLayout(this);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams sideParams = new LinearLayout.LayoutParams(dp(sidebarWidth), ViewGroup.LayoutParams.MATCH_PARENT);
        sideParams.rightMargin = dp(18);
        root.addView(sidebar, sideParams);

        clock = text("", 43);
        clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        sidebar.addView(clock);
        date = text("", 14);
        date.setAlpha(.84f);
        sidebar.addView(date);
        ScrollView menuScroller = new ScrollView(this);
        menuScroller.setFillViewport(true);
        menuScroller.setVerticalScrollBarEnabled(false);
        menuScroller.setFocusable(false);
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.VERTICAL);
        nav.setPadding(dp(10), dp(12), dp(10), dp(12));
        nav.setBackground(glass());
        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        navParams.topMargin = dp(16);
        sidebar.addView(menuScroller, navParams);
        menuScroller.addView(nav);
        String[] names = {"⌂  Início", "▦  Apps", "▣  Filmes e Séries", "▣  TV ao Vivo", "◈  Jogos", "♫  Música", "⚙  Ajustes da TV", "✦  Personalizar"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            TextView item = text(names[i], 16);
            item.setPadding(dp(12), dp(9), dp(6), dp(9));
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
            if (index == 0) homeMenu = item;
        }
        scroller = new ScrollView(this);
        scroller.setFocusable(false);
        scroller.setFillViewport(true);
        scroller.setClipToPadding(false);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setSmoothScrollingEnabled(true);
        root.addView(scroller, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(8), dp(5), dp(8), dp(18));
        scroller.addView(content);
        setContentView(root);
        int restoredPage = saved == null ? 0 : saved.getInt("page");
        if (saved != null) pendingCover = saved.getString("pending_cover");
        if (restoredPage == 8) showCovers();
        else if (restoredPage == 7) showPersonalize();
        else if (restoredPage == 1) showAllApps();
        else if (restoredPage >= 2 && restoredPage <= 5) showCategory(restoredPage);
        else showHome();
    }
    private Drawable wallpaper(Drawable fallback) {
        try {
            java.io.InputStream stream = getAssets().open("spiderman_wallpaper.jpg");
            android.graphics.Bitmap bitmap;
            try { bitmap = BitmapFactory.decodeStream(stream); }
            finally { stream.close(); }
            if (bitmap == null) return fallback;
            return new PhotoWallpaper(bitmap);
        } catch (Exception ignored) {
            return prefs.getBoolean("space", true) ? new SpaceWallpaper() : fallback;
        }
    }
    private void heading(String value) {
        TextView title = text(value, 21);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(43));
        p.topMargin = dp(10);
        content.addView(title, p);
    }
    private List<ApplicationInfo> queryApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> all = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        seen.add(getPackageName());
        seen.add("com.meulauncher.glass");
        seen.add("com.meulauncher.glass.teste");
        for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo result : pm.queryIntentActivities(query, 0)) {
                ApplicationInfo info = result.activityInfo.applicationInfo;
                if (seen.add(info.packageName)) all.add(info);
            }
        }
        Collections.sort(all, Comparator.comparing(a -> pm.getApplicationLabel(a).toString().toLowerCase(Locale.ROOT)));
        return all;
    }
    private void grid(List<ApplicationInfo> items, int limit) {
        PackageManager pm = getPackageManager();
        int width = getResources().getConfiguration().screenWidthDp - sidebarWidth - 78;
        int columns = Math.max(1, Math.min(compact ? 7 : 6, width / (compact ? 95 : 112)));
        int tileWidth = Math.max(64, Math.min(compact ? 105 : 124, width / columns - 8));
        LinearLayout row = null;
        int count = 0;
        for (ApplicationInfo info : items) {
            if (limit > 0 && count >= limit) break;
            if (count % columns == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                content.addView(row);
            }
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(5), dp(8), dp(5), dp(5));
            tile.setBackground(glass());
            LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(dp(tileWidth), dp(compact ? 90 : 102));
            tileParams.setMargins(dp(4), dp(4), dp(4), dp(4));
            row.addView(tile, tileParams);
            Drawable banner = useAppBanners || page == 8 ? appBanner(info) : null;
            ImageView image = new ImageView(this);
            image.setImageDrawable(banner != null ? banner : pm.getApplicationIcon(info));
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int imageWidth = banner != null ? tileWidth - 10 : compact ? 43 : 54;
            tile.addView(image, new LinearLayout.LayoutParams(dp(imageWidth), dp(compact ? 45 : 55)));
            String name = pm.getApplicationLabel(info).toString();
            tile.setContentDescription(name);
            if (showLabels || page == 8) {
                TextView label = text(name, 12);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(2);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                tile.addView(label);
            }
            focus(tile);
            tile.setOnClickListener(v -> {
                if (page == 8) pickCover(info.packageName);
                else launchApp(info);
            });
            tile.setOnLongClickListener(v -> {
                if (page == 8) {
                    if (writingCover) return true;
                    coverFile(info.packageName).delete();
                    Drawable originalBanner = pm.getApplicationBanner(info);
                    image.setImageDrawable(originalBanner != null ? originalBanner : pm.getApplicationIcon(info));
                    ViewGroup.LayoutParams size = image.getLayoutParams();
                    size.width = dp(originalBanner != null ? tileWidth - 10 : compact ? 43 : 54);
                    image.setLayoutParams(size);
                    Toast.makeText(this, "Capa personalizada removida", Toast.LENGTH_SHORT).show();
                    return true;
                }
                String key = "fav_" + info.packageName;
                boolean old = prefs.getBoolean(key, false);
                prefs.edit().putBoolean(key, !old).apply();
                Toast.makeText(this, old ? "Removido dos favoritos" : "Adicionado aos favoritos",
                    Toast.LENGTH_SHORT).show();
                if (page == 0) showHome();
                return true;
            });
            count++;
        }
        if (count == 0) content.addView(text("Nenhum aplicativo disponível nesta categoria.", 16));
    }

    // Rotating poster-style recommendations sourced from apps actually installed on the TV.
    // This avoids displaying fictional movie titles or links to unavailable content.
    private void posters() {
        List<ApplicationInfo> streaming = new ArrayList<>();
        PackageManager pm = getPackageManager();
        for (ApplicationInfo app : installedApps) {
            String name = pm.getApplicationLabel(app).toString().toLowerCase(Locale.ROOT);
            if (name.matches(".*(netflix|prime video|disney|globoplay|stremio|plex|youtube|pluto|hbo|max|crunchyroll|apple tv).*"))
                streaming.add(app);
        }
        if (streaming.isEmpty()) {
            TextView empty = text("Instale um aplicativo de streaming para ver sugestões aqui.", 15);
            empty.setPadding(dp(18), dp(15), dp(18), dp(15));
            empty.setBackground(glass());
            content.addView(empty);
            return;
        }
        int offset = (int) Math.floorMod(java.time.LocalDate.now().toEpochDay(), (long) streaming.size());
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams stripParams = new LinearLayout.LayoutParams(-1, dp(170));
        stripParams.bottomMargin = dp(12);
        content.addView(strip, stripParams);
        for (int n = 0; n < Math.min(3, streaming.size()); n++) {
            ApplicationInfo app = streaming.get((offset + n) % streaming.size());
            String label = pm.getApplicationLabel(app).toString();
            FrameLayout poster = new FrameLayout(this);
            poster.setClipToOutline(true);
            poster.setContentDescription("Abrir " + label);
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0, -1, 1f);
            pp.setMargins(dp(5), 0, dp(5), 0);
            strip.addView(poster, pp);
            ImageView art = new ImageView(this);
            Drawable banner = useAppBanners ? appBanner(app) : null;
            art.setImageDrawable(banner != null ? banner : pm.getApplicationIcon(app));
            if (banner != null) {
                art.setScaleType(ImageView.ScaleType.CENTER_CROP);
                poster.addView(art, new FrameLayout.LayoutParams(-1, -1));
            } else {
                art.setScaleType(ImageView.ScaleType.FIT_CENTER);
                FrameLayout.LayoutParams icon = new FrameLayout.LayoutParams(dp(78), dp(78), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                icon.topMargin = dp(12);
                poster.addView(art, icon);
            }
            TextView title = text(label, 16);
            title.setGravity(Gravity.BOTTOM | Gravity.START);
            title.setPadding(dp(12), 0, dp(12), dp(12));
            title.setSingleLine(true);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            title.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, Color.argb(235, 12, 16, 25)}));
            poster.addView(title, new FrameLayout.LayoutParams(-1, dp(64), Gravity.BOTTOM));
            focus(poster);
            poster.setOnClickListener(v -> launchApp(app));
        }
    }
    private void launchApp(ApplicationInfo info) {
        PackageManager pm = getPackageManager();
        try {
            Intent intent = pm.getLeanbackLaunchIntentForPackage(info.packageName);
            if (intent == null) intent = pm.getLaunchIntentForPackage(info.packageName);
            if (intent != null) startActivity(intent);
            else Toast.makeText(this, "Aplicativo indisponível", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir " + pm.getApplicationLabel(info), Toast.LENGTH_SHORT).show();
        }
    }
    private void showHome() {
        openPage(0);
        heading("Para você");
        posters();
        heading("Seus aplicativos");
        List<ApplicationInfo> favorites = new ArrayList<>();
        for (ApplicationInfo app : installedApps) {
            if (prefs.getBoolean("fav_" + app.packageName, false)) favorites.add(app);
        }
        grid(favorites.isEmpty() ? installedApps : favorites, 12);
        TextView browse = text("Ver todos os aplicativos", 16);
        browse.setPadding(dp(18), dp(14), dp(18), dp(14));
        focus(browse);
        browse.setOnClickListener(v -> showAllApps());
        content.addView(browse);
    }

    private void openPage(int nextPage) {
        page = nextPage;
        if (content.hasFocus()) homeMenu.requestFocus();
        content.removeAllViews();
        scroller.scrollTo(0, 0);
    }

    private void showAllApps() {
        openPage(1);
        heading("Todos os apps");
        grid(installedApps, 0);
    }
    private void showPersonalize() {
        openPage(7);
        heading("Personalizar");
        TextView option = text((compact ? "☑" : "☐") + "  Ícones compactos", 19);
        option.setPadding(dp(22), dp(18), dp(22), dp(18));
        option.setBackground(glass());
        focus(option);
        option.setOnClickListener(v -> {
            compact = !compact;
            prefs.edit().putBoolean("compact", compact).apply();
            option.setText((compact ? "☑" : "☐") + "  Ícones compactos");
        });
        content.addView(option);
        TextView labels = text((showLabels ? "☑" : "☐") + "  Mostrar nomes dos aplicativos", 19);
        labels.setPadding(dp(22), dp(18), dp(22), dp(18));
        labels.setBackground(glass());
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = dp(12);
        content.addView(labels, labelParams);
        focus(labels);
        labels.setOnClickListener(v -> {
            showLabels = !showLabels;
            prefs.edit().putBoolean("show_labels", showLabels).apply();
            labels.setText((showLabels ? "☑" : "☐") + "  Mostrar nomes dos aplicativos");
        });
        TextView banners = text((useAppBanners ? "☑" : "☐") + "  Usar capas dos apps de TV", 19);
        banners.setPadding(dp(22), dp(18), dp(22), dp(18));
        LinearLayout.LayoutParams bannerParams = new LinearLayout.LayoutParams(-1, -2);
        bannerParams.topMargin = dp(12);
        content.addView(banners, bannerParams);
        focus(banners);
        banners.setOnClickListener(v -> {
            useAppBanners = !useAppBanners;
            prefs.edit().putBoolean("app_banners", useAppBanners).apply();
            banners.setText((useAppBanners ? "☑" : "☐") + "  Usar capas dos apps de TV");
        });
        TextView wallpaper = text((prefs.getBoolean("photo", true) ? "☑" : "☐") + "  Papel de parede Homem-Aranha", 19);
        wallpaper.setPadding(dp(22), dp(18), dp(22), dp(18));
        wallpaper.setBackground(glass());
        LinearLayout.LayoutParams wallpaperParams = new LinearLayout.LayoutParams(-1, -2);
        wallpaperParams.topMargin = dp(12);
        content.addView(wallpaper, wallpaperParams);
        focus(wallpaper);
        wallpaper.setOnClickListener(v -> {
            boolean next = !prefs.getBoolean("photo", true);
            prefs.edit().putBoolean("photo", next).apply();
            recreate();
        });
        TextView covers = text("  Personalizar capas dos aplicativos", 19);
        covers.setPadding(dp(22), dp(18), dp(22), dp(18));
        LinearLayout.LayoutParams coversParams = new LinearLayout.LayoutParams(-1, -2);
        coversParams.topMargin = dp(12);
        content.addView(covers, coversParams);
        focus(covers);
        covers.setOnClickListener(v -> showCovers());

        TextView help = text("Segure OK sobre um aplicativo para adicionar ou remover dos favoritos.", 16);
        help.setPadding(dp(12), dp(28), dp(12), dp(12));
        content.addView(help);
    }
    private void showCovers() {
        openPage(8);
        heading("Capas dos aplicativos");
        content.addView(text("OK para escolher uma imagem. Segure OK para restaurar a capa do app.", 15));
        grid(installedApps, 0);
    }

    private AtomicFile coverFile(String packageName) {
        return new AtomicFile(new File(getFilesDir(), "cover_" + packageName + ".jpg"));
    }

    private Drawable appBanner(ApplicationInfo info) {
        AtomicFile file = coverFile(info.packageName);
        if (file.getBaseFile().exists()) {
            try (InputStream stream = file.openRead()) {
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                if (bitmap != null) return new BitmapDrawable(getResources(), bitmap);
            } catch (IOException ignored) {
                // Fall back to the banner supplied by the app.
            }
        }
        return getPackageManager().getApplicationBanner(info);
    }

    private void pickCover(String packageName) {
        if (writingCover) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        try {
            pendingCover = packageName;
            startActivityForResult(intent, PICK_COVER);
        } catch (android.content.ActivityNotFoundException e) {
            pendingCover = null;
            Toast.makeText(this, "A TV não oferece um seletor de imagens", Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_COVER) return;
        String packageName = pendingCover;
        pendingCover = null;
        if (resultCode != RESULT_OK || packageName == null || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        writingCover = true;
        new Thread(() -> {
            boolean saved = saveCover(packageName, uri);
            runOnUiThread(() -> {
                writingCover = false;
                if (isDestroyed() || isFinishing()) return;
                if (saved) {
                    useAppBanners = true;
                    prefs.edit().putBoolean("app_banners", true).apply();
                    if (page == 8) showCovers();
                }
                Toast.makeText(this, saved ? "Capa atualizada" : "Não foi possível ler a imagem",
                    Toast.LENGTH_SHORT).show();
            });
        }, "launcher-cover").start();
    }

    private boolean saveCover(String packageName, Uri uri) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream stream = getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(stream, null, options);
        } catch (IOException | SecurityException e) {
            return false;
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) return false;
        options.inJustDecodeBounds = false;
        options.inSampleSize = 1;
        while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 640) {
            options.inSampleSize *= 2;
        }
        Bitmap bitmap;
        try (InputStream stream = getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(stream, null, options);
        } catch (IOException | SecurityException e) {
            return false;
        }
        if (bitmap == null) return false;
        AtomicFile file = coverFile(packageName);
        FileOutputStream output = null;
        try {
            output = file.startWrite();
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) {
                file.failWrite(output);
                return false;
            }
            file.finishWrite(output);
            return true;
        } catch (IOException e) {
            file.failWrite(output);
            return false;
        } finally {
            bitmap.recycle();
        }
    }

    private void showCategory(int index) {
        openPage(index);
        String[] titles = {"", "", "Filmes e Séries", "TV ao Vivo", "Jogos", "Música"};
        heading(titles[index]);
        List<ApplicationInfo> matching = new ArrayList<>();
        for (ApplicationInfo app : installedApps) {
            String name = getPackageManager().getApplicationLabel(app).toString().toLowerCase(Locale.ROOT);
            boolean ok = index == 2 ? name.matches(".*(netflix|prime|disney|max|globoplay|stremio|plex|youtube|filme).*")
                : index == 3 ? name.matches(".*(tv|pluto|canal|iptv).*")
                : index == 4 ? (app.category == ApplicationInfo.CATEGORY_GAME || name.matches(".*(game|jogo|xbox|steam|geforce).*"))
                : name.matches(".*(music|música|spotify|deezer|tidal|sound).*");
            if (ok) matching.add(app);
        }
        grid(matching, 0);
    }
    @Override public void onBackPressed() {
        if (page == 8) showPersonalize();
        else if (page != 0) showHome();
        else super.onBackPressed();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("page", page);
        state.putString("pending_cover", pendingCover);
        super.onSaveInstanceState(state);
    }

    @Override protected void onStart() {
        super.onStart();
        List<ApplicationInfo> currentApps = queryApps();
        boolean appsChanged = currentApps.size() != installedApps.size();
        for (int i = 0; !appsChanged && i < currentApps.size(); i++) {
            appsChanged = !currentApps.get(i).packageName.equals(installedApps.get(i).packageName);
        }
        if (appsChanged) {
            installedApps = currentApps;
            if (page == 0) showHome();
            else if (page == 1) showAllApps();
            else if (page >= 2 && page <= 5) showCategory(page);
            else if (page == 8) showCovers();
        }
        handler.post(tick);
    }

    @Override protected void onStop() {
        handler.removeCallbacks(tick);
        super.onStop();
    }
}
