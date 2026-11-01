package com.meulauncher.glass;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.app.WallpaperManager;
import android.view.Gravity;
import android.view.KeyEvent;
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
    private LinearLayout root, content, menu;
    private SharedPreferences prefs;
    private int accent = Color.rgb(180, 205, 225);
    private boolean glassEnabled;
    private boolean glassMotion;
    private int tileSize;
    private int glassStyle;
    private boolean clockWidget;
    private boolean shortcutsWidget;
    private int widgetSize;
    private boolean weatherWidget;
    private boolean dateWidget, greetingWidget;
    private int clockFont, clockFormat;
    private static final String[] WIDGET_IDS = {"clock", "weather", "date", "greeting", "shortcuts"};
    private static final String[] WIDGET_NAMES = {"Relógio", "Clima de Campinas", "Data", "Boas-vindas", "Atalhos"};
    private TextView weatherView;
    private static final int PICK_WALLPAPER = 701;
    private static final int PICK_APP_ICON = 702;
    private String pendingIconPackage;
    private android.graphics.Bitmap customWallpaper;

    private int dp(int n) { return (int)(getResources().getDisplayMetrics().density * n + 0.5f); }

    private GradientDrawable panel(int radius) {
        GradientDrawable bg = new GradientDrawable();
        int[] shades = {Color.argb(115, 110, 110, 115), Color.argb(65, 145, 145, 145), Color.argb(115, 155, 100, 125), Color.argb(210, 30, 30, 34), Color.rgb(43, 43, 46)};
        bg.setColor(glassEnabled ? shades[Math.max(0, Math.min(4, glassStyle))] : shades[4]);
        bg.setCornerRadius(dp(radius));
        bg.setStroke(dp(1), glassEnabled ? Color.argb(145, 255, 255, 255) : Color.rgb(95, 105, 120));
        return bg;
    }

    // Reflexo discreto na borda do foco, sem faixas atravessando a tela.
    private GradientDrawable focusGlass(int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.argb(190, 225, 235, 245), Color.argb(115, 130, 150, 170),
                        Color.argb(165, 245, 230, 240)});
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(2), Color.argb(225, 245, 250, 255));
        return d;
    }

    private void glassFocus(View v, boolean focused, int radius) {
        v.animate().cancel();
        if (glassEnabled && glassMotion) {
            v.setBackground(focused ? focusGlass(radius) : panel(radius));
            v.animate().scaleX(focused ? 1.045f : 1f)
                    .scaleY(focused ? 1.045f : 1f)
                    .alpha(focused ? 1f : 0.92f)
                    .setDuration(180)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        } else {
            v.setBackground(focused ? focusGlass(radius) : panel(radius));
            v.setScaleX(1f);
            v.setScaleY(1f);
            v.setAlpha(1f);
        }
    }

    private TextView label(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(Color.WHITE);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(15), dp(10), dp(15), dp(10));
        return t;
    }

    private TextView action(String name, Runnable runnable) {
        TextView t = label(name, 17);
        t.setFocusable(true);
        t.setBackground(panel(16));
        t.setOnClickListener(v -> runnable.run());
        t.setOnFocusChangeListener((v, focused) -> glassFocus(v, focused, 16));
        return t;
    }

    private void addAction(LinearLayout parent, String name, Runnable runnable) {
        TextView b = action(name, runnable);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52));
        p.bottomMargin = dp(9);
        parent.addView(b, p);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("launcher", MODE_PRIVATE);
        glassEnabled = prefs.getBoolean("glass", true);
        glassMotion = prefs.getBoolean("glassMotion", true);
        tileSize = prefs.getInt("tile", 115);
        glassStyle = prefs.getInt("glassStyle", 0);
        clockWidget = prefs.getBoolean("clockWidget", true);
        shortcutsWidget = prefs.getBoolean("shortcutsWidget", true);
        widgetSize = prefs.getInt("widgetSize", 1);
        weatherWidget = prefs.getBoolean("weatherWidget", true);
        dateWidget = prefs.getBoolean("dateWidget", true);
        greetingWidget = prefs.getBoolean("greetingWidget", false);
        clockFont = prefs.getInt("clockFont", 0);
        clockFormat = prefs.getInt("clockFormat", 0);
        loadSavedWallpaper();
        showHome();
    }

    private void base(String heading) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setPadding(dp(22), dp(22), dp(22), dp(22));
        if (customWallpaper != null) {
            root.setBackground(new android.graphics.drawable.BitmapDrawable(getResources(), customWallpaper));
        } else {
            root.setBackgroundColor(Color.rgb(17, 24, 38));
        }
        menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(10), dp(10), dp(10), dp(10));
        menu.setBackground(panel(26));
        root.addView(menu, new LinearLayout.LayoutParams(dp(230), -1));
        addAction(menu, "Início", this::showHome);
        addAction(menu, "Meus Apps", this::showApps);
        addAction(menu, "Favoritos", this::showApps);
        addAction(menu, "Configurações", this::showSettings);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), 0, 0, 0);
        root.addView(content, new LinearLayout.LayoutParams(0, -1, 1));
        TextView title = label(heading, 26);
        title.setTypeface(null, Typeface.BOLD);
        content.addView(title);
        setContentView(root);
    }

    private void showHome() {
        base("TCL Google TV");
        ScrollView scroll = new ScrollView(this);
        LinearLayout home = new LinearLayout(this);
        home.setOrientation(LinearLayout.VERTICAL);
        home.setPadding(0, dp(8), dp(10), dp(10));
        TextView feature = label("Sua TV, do seu jeito\\nLiquid Glass", 27);
        feature.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        feature.setBackground(panel(24));
        LinearLayout.LayoutParams featureParams = new LinearLayout.LayoutParams(-1, dp(120));
        featureParams.bottomMargin = dp(14);
        home.addView(feature, featureParams);

        home.addView(label("Widgets", 20));
        for (String id : widgetOrder()) {
            if (!isWidgetVisible(id)) continue;
            if (id.equals("clock")) {
                TextView clock = label("", 28);
                Typeface[] fonts = {Typeface.DEFAULT, Typeface.SERIF, Typeface.MONOSPACE,
                        Typeface.create("sans-serif-light", 0), Typeface.create("sans-serif-condensed", 1)};
                clock.setTypeface(fonts[Math.max(0, Math.min(4, clockFont))]);
                clock.setBackground(panel(20));
                Runnable updateClock = new Runnable() {
                    @Override public void run() {
                        if (clock.getParent() == null) return;
                        clock.setText(new java.text.SimpleDateFormat(clockFormat == 0 ? "HH:mm" : "hh:mm a",
                                java.util.Locale.getDefault()).format(new java.util.Date()));
                        clock.postDelayed(this, 30000);
                    }
                };
                home.addView(clock, widgetParams());
                clock.post(updateClock);
            } else if (id.equals("weather")) {
                weatherView = label("Campinas, SP  •  Carregando clima…", 17);
                weatherView.setBackground(panel(20));
                home.addView(weatherView, widgetParams());
                loadCampinasWeather();
            } else if (id.equals("date")) {
                TextView date = label(new java.text.SimpleDateFormat("EEEE, dd 'de' MMMM",
                        new java.util.Locale("pt", "BR")).format(new java.util.Date()), 18);
                date.setBackground(panel(20));
                home.addView(date, widgetParams());
            } else if (id.equals("greeting")) {
                TextView welcome = label("Bem-vindo à sua TCL Google TV", 18);
                welcome.setBackground(panel(20));
                home.addView(welcome, widgetParams());
            } else if (id.equals("shortcuts")) {
                addAction(home, "Ver todos os aplicativos  ›", this::showApps);
            }
        }
        home.addView(label("Seus aplicativos", 22));
        android.widget.HorizontalScrollView appScroll = new android.widget.HorizontalScrollView(this);
        appScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout appRow = new LinearLayout(this);
        appRow.setOrientation(LinearLayout.HORIZONTAL);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> apps = installed();
        int count = Math.min(apps.size(), 12);
        for (int i = 0; i < count; i++) {
            ResolveInfo info = apps.get(i);
            String appName = info.loadLabel(pm).toString();
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(8), dp(8), dp(8), dp(8));
            tile.setBackground(panel(18));
            ImageView image = new ImageView(this);
            image.setImageDrawable(loadAppIcon(info.activityInfo.packageName, info.loadIcon(pm)));
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            tile.addView(image, new LinearLayout.LayoutParams(dp(55), dp(55)));
            TextView name = label(appName, 13);
            name.setGravity(Gravity.CENTER);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tile.addView(name, new LinearLayout.LayoutParams(-1, dp(38)));
            tile.setFocusable(true);
            tile.setOnFocusChangeListener((v, focused) -> glassFocus(v, focused, 18));
            tile.setOnClickListener(v -> launchApp(info, appName));
            tile.setOnLongClickListener(v -> {
                showIconOptions(info.activityInfo.packageName, appName);
                return true;
            });
            LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(dp(125), dp(126));
            tileParams.rightMargin = dp(10);
            appRow.addView(tile, tileParams);
        }
        if (count == 0) appRow.addView(label("Nenhum aplicativo encontrado", 17));
        appScroll.addView(appRow);
        home.addView(appScroll, new LinearLayout.LayoutParams(-1, dp(148)));
        addAction(home, "Personalizar tela inicial  ›", this::showSettings);
        scroll.addView(home);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void launchApp(ResolveInfo info, String name) {
        try {
            Intent launch = new Intent();
            launch.setClassName(info.activityInfo.packageName, info.activityInfo.name);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Não foi possível abrir " + name,
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private List<ResolveInfo> installed() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> result = new ArrayList<>(pm.queryIntentActivities(intent, 0));
        if (result.isEmpty()) {
            intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            result = new ArrayList<>(pm.queryIntentActivities(intent, 0));
        }
        Collections.sort(result, new Comparator<ResolveInfo>() {
            public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString());
            }
        });
        return result;
    }

    private void showApps() {
        base("Aplicativos instalados");
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        PackageManager pm = getPackageManager();
        for (ResolveInfo info : installed()) {
            String name = info.loadLabel(pm).toString();
            LinearLayout line = new LinearLayout(this);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setBackground(panel(18));
            line.setPadding(dp(12), dp(6), dp(12), dp(6));
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(loadAppIcon(info.activityInfo.packageName, info.loadIcon(pm)));
            line.addView(icon, new LinearLayout.LayoutParams(dp(45), dp(45)));
            TextView text = label(name, 18);
            line.addView(text, new LinearLayout.LayoutParams(0, dp(tileSize), 1));
            line.setFocusable(true);
            line.setOnClickListener(v -> {
                try {
                    Intent launch = new Intent();
                    launch.setClassName(info.activityInfo.packageName, info.activityInfo.name);
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launch);
                } catch (Exception e) {
                    android.widget.Toast.makeText(this, "Não foi possível abrir " + name, android.widget.Toast.LENGTH_SHORT).show();
                }
            });
            line.setOnFocusChangeListener((v, focused) -> glassFocus(v, focused, 18));
            line.setOnLongClickListener(v -> {
                showIconOptions(info.activityInfo.packageName, name);
                return true;
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(8);
            list.addView(line, lp);
        }
        if (list.getChildCount() == 0) list.addView(label("Nenhum aplicativo encontrado.", 18));
        scroll.addView(list);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void loadCampinasWeather() {
        final TextView target = weatherView;
        new Thread(() -> {
            String message = "Campinas, SP  •  Clima indisponível";
            java.net.HttpURLConnection connection = null;
            try {
                java.net.URL url = new java.net.URL("https://api.open-meteo.com/v1/forecast?latitude=-22.9056&longitude=-47.0608&current=temperature_2m,relative_humidity_2m,weather_code&timezone=America%2FSao_Paulo");
                connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(7000);
                try (java.io.InputStream input = connection.getInputStream();
                     java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                    org.json.JSONObject current = new org.json.JSONObject(output.toString("UTF-8")).getJSONObject("current");
                    int temperature = (int) Math.round(current.getDouble("temperature_2m"));
                    int humidity = current.getInt("relative_humidity_2m");
                    int code = current.getInt("weather_code");
                    String condition = code == 0 ? "Céu limpo" : code <= 3 ? "Parcialmente nublado" : code <= 48 ? "Neblina" : code <= 67 ? "Chuva" : code <= 77 ? "Neve" : code <= 82 ? "Pancadas de chuva" : code <= 86 ? "Neve" : code <= 99 ? "Tempestade" : "Condição variável";
                    message = "Campinas, SP   •   " + temperature + "°C\\n" + condition + "   •   Umidade " + humidity + "%";
                }
            } catch (Exception ignored) {
                // Mantém mensagem de indisponibilidade se não houver internet.
            } finally {
                if (connection != null) connection.disconnect();
            }
            final String result = message;
            runOnUiThread(() -> { if (target.getParent() != null) target.setText(result); });
        }).start();
    }

    private android.graphics.drawable.Drawable loadAppIcon(String packageName, android.graphics.drawable.Drawable fallback) {
        String stored = prefs.getString("appIcon_" + packageName, null);
        if (stored == null) return fallback;
        try {
            android.net.Uri uri = android.net.Uri.parse(stored);
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inSampleSize = 2;
            try (java.io.InputStream input = getContentResolver().openInputStream(uri)) {
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(input, null, options);
                if (bitmap != null) return new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    private void showIconOptions(String packageName, String appName) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Ícone de " + appName)
            .setItems(new String[]{"Escolher imagem da galeria", "Restaurar ícone original"}, (dialog, which) -> {
                if (which == 0) {
                    pendingIconPackage = packageName;
                    try {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.setType("image/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                        startActivityForResult(intent, PICK_APP_ICON);
                    } catch (Exception e) {
                        android.widget.Toast.makeText(this, "Seletor de imagens indisponível", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } else {
                    prefs.edit().remove("appIcon_" + packageName).apply();
                    showApps();
                }
            }).show();
    }

    private void pickWallpaper() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, PICK_WALLPAPER);
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Nenhum seletor de imagens disponível", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSavedWallpaper() {
        String uri = prefs.getString("wallpaperUri", null);
        if (uri == null) return;
        try {
            android.net.Uri selected = android.net.Uri.parse(uri);
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (java.io.InputStream input = getContentResolver().openInputStream(selected)) {
                android.graphics.BitmapFactory.decodeStream(input, null, options);
            }
            int scale = 1;
            while (options.outWidth / scale > 1920 || options.outHeight / scale > 1080) scale *= 2;
            options.inJustDecodeBounds = false;
            options.inSampleSize = scale;
            try (java.io.InputStream input = getContentResolver().openInputStream(selected)) {
                customWallpaper = android.graphics.BitmapFactory.decodeStream(input, null, options);
            }
        } catch (Exception ignored) {
            customWallpaper = null;
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_APP_ICON && resultCode == RESULT_OK && data != null && data.getData() != null && pendingIconPackage != null) {
            try {
                android.net.Uri selected = data.getData();
                getContentResolver().takePersistableUriPermission(selected, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                prefs.edit().putString("appIcon_" + pendingIconPackage, selected.toString()).apply();
                pendingIconPackage = null;
                showApps();
            } catch (Exception e) {
                android.widget.Toast.makeText(this, "Não foi possível salvar o ícone", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == PICK_WALLPAPER && resultCode == RESULT_OK && data != null && data.getData() != null) {
            android.net.Uri selected = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(selected, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                prefs.edit().putString("wallpaperUri", selected.toString()).apply();
                loadSavedWallpaper();
                showSettings();
            } catch (Exception e) {
                android.widget.Toast.makeText(this, "Não foi possível carregar a imagem", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showIconSettings() {
        base("Personalizar ícones");
        content.addView(label("Escolha um aplicativo para trocar ou restaurar seu ícone.", 17));
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        PackageManager pm = getPackageManager();
        for (ResolveInfo info : installed()) {
            String appName = info.loadLabel(pm).toString();
            String packageName = info.activityInfo.packageName;
            addAction(list, appName + "  ›", () -> showIconOptions(packageName, appName));
        }
        scroll.addView(list);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private LinearLayout.LayoutParams widgetParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(55 + widgetSize * 20));
        params.bottomMargin = dp(9);
        return params;
    }

    private List<String> widgetOrder() {
        String saved = prefs.getString("widgetOrder", "clock,weather,date,greeting,shortcuts");
        List<String> order = new ArrayList<>();
        for (String item : saved.split(",")) {
            for (String known : WIDGET_IDS) {
                if (known.equals(item) && !order.contains(item)) order.add(item);
            }
        }
        for (String id : WIDGET_IDS) if (!order.contains(id)) order.add(id);
        return order;
    }

    private boolean isWidgetVisible(String id) {
        switch (id) {
            case "clock": return clockWidget;
            case "weather": return weatherWidget;
            case "date": return dateWidget;
            case "greeting": return greetingWidget;
            case "shortcuts": return shortcutsWidget;
            default: return false;
        }
    }

    private void setWidgetVisible(String id, boolean visible) {
        switch (id) {
            case "clock": clockWidget = visible; prefs.edit().putBoolean("clockWidget", visible).apply(); break;
            case "weather": weatherWidget = visible; prefs.edit().putBoolean("weatherWidget", visible).apply(); break;
            case "date": dateWidget = visible; prefs.edit().putBoolean("dateWidget", visible).apply(); break;
            case "greeting": greetingWidget = visible; prefs.edit().putBoolean("greetingWidget", visible).apply(); break;
            case "shortcuts": shortcutsWidget = visible; prefs.edit().putBoolean("shortcutsWidget", visible).apply(); break;
        }
    }

    private String widgetName(String id) {
        for (int i = 0; i < WIDGET_IDS.length; i++) if (WIDGET_IDS[i].equals(id)) return WIDGET_NAMES[i];
        return id;
    }

    private void showWidgetManager() {
        base("Gerenciar widgets");
        content.addView(label("OK: mostrar ou ocultar. Use as opções para mudar a ordem.", 16));
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        List<String> order = widgetOrder();
        for (int i = 0; i < order.size(); i++) {
            final int position = i;
            String id = order.get(i);
            addAction(list, (isWidgetVisible(id) ? "✓  " : "+  ") + widgetName(id) + "  •  " +
                    (isWidgetVisible(id) ? "Ativado" : "Desativado"), () -> {
                setWidgetVisible(id, !isWidgetVisible(id));
                showWidgetManager();
            });
            if (i > 0) addAction(list, "↑  Subir " + widgetName(id), () -> {
                Collections.swap(order, position, position - 1);
                prefs.edit().putString("widgetOrder", android.text.TextUtils.join(",", order)).apply();
                showWidgetManager();
            });
            if (i < order.size() - 1) addAction(list, "↓  Descer " + widgetName(id), () -> {
                Collections.swap(order, position, position + 1);
                prefs.edit().putString("widgetOrder", android.text.TextUtils.join(",", order)).apply();
                showWidgetManager();
            });
        }
        scroll.addView(list);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void showSettings() {
        base("Personalização  •  TCL Google TV");
        ScrollView scroll = new ScrollView(this);
        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        options.setPadding(dp(8), dp(8), dp(8), dp(12));
        options.addView(label("Deixe o seu launcher do seu jeito", 19));
        addAction(options, "Papel de parede: escolher da galeria  ›", this::pickWallpaper);
        addAction(options, "Restaurar papel de parede padrão", () -> {
            prefs.edit().remove("wallpaperUri").apply();
            customWallpaper = null;
            showSettings();
        });
        String[] styles = {"Padrão (cinza)", "Transparente", "Colorido", "Escuro", "Sem efeito"};
        addAction(options, "Estilo Liquid Glass: " + styles[glassStyle] + "  ›", () -> {
            glassStyle = (glassStyle + 1) % styles.length;
            glassEnabled = glassStyle != 4;
            prefs.edit().putInt("glassStyle", glassStyle).putBoolean("glass", glassEnabled).apply();
            showSettings();
        });
        addAction(options, "Animação Liquid Glass: " + (glassMotion ? "Ativada" : "Desativada"), () -> {
            glassMotion = !glassMotion;
            prefs.edit().putBoolean("glassMotion", glassMotion).apply();
            showSettings();
        });
        options.addView(label("Widgets", 21));
        addAction(options, "Gerenciar, adicionar, remover e organizar widgets  ›", this::showWidgetManager);
        addAction(options, "Clima de Campinas: " + (weatherWidget ? "Exibir" : "Ocultar"), () -> {
            weatherWidget = !weatherWidget;
            prefs.edit().putBoolean("weatherWidget", weatherWidget).apply();
            showSettings();
        });
        addAction(options, "Relógio: " + (clockWidget ? "Exibir" : "Ocultar"), () -> {
            clockWidget = !clockWidget;
            prefs.edit().putBoolean("clockWidget", clockWidget).apply();
            showSettings();
        });
        addAction(options, "Atalho de aplicativos: " + (shortcutsWidget ? "Exibir" : "Ocultar"), () -> {
            shortcutsWidget = !shortcutsWidget;
            prefs.edit().putBoolean("shortcutsWidget", shortcutsWidget).apply();
            showSettings();
        });
        String[] sizes = {"Pequeno", "Médio", "Grande"};
        addAction(options, "Tamanho dos widgets: " + sizes[widgetSize] + "  ›", () -> {
            widgetSize = (widgetSize + 1) % 3;
            prefs.edit().putInt("widgetSize", widgetSize).apply();
            showSettings();
        });
        addAction(options, "Widget de data: " + (dateWidget ? "Exibir" : "Ocultar"), () -> {
            dateWidget = !dateWidget;
            prefs.edit().putBoolean("dateWidget", dateWidget).apply();
            showSettings();
        });
        addAction(options, "Widget de boas-vindas: " + (greetingWidget ? "Exibir" : "Ocultar"), () -> {
            greetingWidget = !greetingWidget;
            prefs.edit().putBoolean("greetingWidget", greetingWidget).apply();
            showSettings();
        });
        String[] fontNames = {"Padrão", "Clássica", "Digital", "Fina", "Condensada"};
        addAction(options, "Fonte do relógio: " + fontNames[clockFont] + "  ›", () -> {
            clockFont = (clockFont + 1) % fontNames.length;
            prefs.edit().putInt("clockFont", clockFont).apply();
            showSettings();
        });
        addAction(options, "Formato do relógio: " + (clockFormat == 0 ? "24 horas" : "12 horas") + "  ›", () -> {
            clockFormat = 1 - clockFormat;
            prefs.edit().putInt("clockFormat", clockFormat).apply();
            showSettings();
        });
        addAction(options, "Tamanho dos aplicativos: " + tileSize + " dp", () -> {
            tileSize = tileSize >= 155 ? 85 : tileSize + 35;
            prefs.edit().putInt("tile", tileSize).apply();
            showSettings();
        });
        addAction(options, "Personalizar ícones dos aplicativos  ›", this::showIconSettings);
        addAction(options, "Aplicativos instalados  ›", this::showApps);
        addAction(options, "Configurações do sistema  ›", () -> {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (Exception e) { android.widget.Toast.makeText(this, "Configurações indisponíveis", 0).show(); }
        });
        options.addView(label("Novos tipos de widgets e arrastar livremente: em desenvolvimento.", 14));
        scroll.addView(options);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    @Override public void onBackPressed() { showHome(); }
}
