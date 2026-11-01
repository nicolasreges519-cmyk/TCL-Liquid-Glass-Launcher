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
    private int tileSize;
    private int glassStyle;
    private boolean clockWidget;
    private boolean shortcutsWidget;
    private int widgetSize;

    private int dp(int n) { return (int)(getResources().getDisplayMetrics().density * n + 0.5f); }

    private GradientDrawable panel(int radius) {
        GradientDrawable bg = new GradientDrawable();
        int[] shades = {Color.argb(115, 110, 110, 115), Color.argb(65, 145, 145, 145), Color.argb(115, 155, 100, 125), Color.argb(210, 30, 30, 34), Color.rgb(43, 43, 46)};
        bg.setColor(glassEnabled ? shades[Math.max(0, Math.min(4, glassStyle))] : shades[4]);
        bg.setCornerRadius(dp(radius));
        bg.setStroke(dp(1), glassEnabled ? Color.argb(145, 255, 255, 255) : Color.rgb(95, 105, 120));
        return bg;
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
        t.setOnFocusChangeListener((v, focused) -> {
            v.setScaleX(focused ? 1.04f : 1f);
            v.setScaleY(focused ? 1.04f : 1f);
            v.setAlpha(focused ? 1f : 0.88f);
        });
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
        tileSize = prefs.getInt("tile", 115);
        glassStyle = prefs.getInt("glassStyle", 0);
        clockWidget = prefs.getBoolean("clockWidget", true);
        shortcutsWidget = prefs.getBoolean("shortcutsWidget", true);
        widgetSize = prefs.getInt("widgetSize", 1);
        showHome();
    }

    private void base(String heading) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setPadding(dp(22), dp(22), dp(22), dp(22));
        root.setBackgroundColor(Color.rgb(17, 24, 38));
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
        base("TCL Google TV  •  Liquid Glass");
        TextView feature = label("Bem-vindo!\n\nEscolha um aplicativo para começar", 23);
        feature.setGravity(Gravity.CENTER);
        feature.setBackground(panel(25));
        content.addView(feature, new LinearLayout.LayoutParams(-1, 0, 1));
        if (clockWidget) {
            TextView clock = label("Relógio  •  " + new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(new java.util.Date()), 21);
            clock.setBackground(panel(20));
            content.addView(clock, new LinearLayout.LayoutParams(-1, dp(54 + widgetSize * 18)));
        }
        if (shortcutsWidget) addAction(content, "Atalho: meus aplicativos", this::showApps);
        addAction(content, "Abrir aplicativos instalados", this::showApps);
        addAction(content, "Configurar aparência e papel de parede", this::showSettings);
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
            icon.setImageDrawable(info.loadIcon(pm));
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
            line.setOnFocusChangeListener((v, focused) -> v.setAlpha(focused ? 1f : 0.75f));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(8);
            list.addView(line, lp);
        }
        if (list.getChildCount() == 0) list.addView(label("Nenhum aplicativo encontrado.", 18));
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
        addAction(options, "Papel de parede  ›", () -> {
            try { startActivity(new Intent(Intent.ACTION_SET_WALLPAPER)); }
            catch (Exception e) { android.widget.Toast.makeText(this, "Seletor indisponível nesta TV", 0).show(); }
        });
        String[] styles = {"Padrão (cinza)", "Transparente", "Colorido", "Escuro", "Sem efeito"};
        addAction(options, "Estilo Liquid Glass: " + styles[glassStyle] + "  ›", () -> {
            glassStyle = (glassStyle + 1) % styles.length;
            glassEnabled = glassStyle != 4;
            prefs.edit().putInt("glassStyle", glassStyle).putBoolean("glass", glassEnabled).apply();
            showSettings();
        });
        options.addView(label("Widgets", 21));
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
        addAction(options, "Tamanho dos aplicativos: " + tileSize + " dp", () -> {
            tileSize = tileSize >= 155 ? 85 : tileSize + 35;
            prefs.edit().putInt("tile", tileSize).apply();
            showSettings();
        });
        addAction(options, "Aplicativos instalados  ›", this::showApps);
        addAction(options, "Configurações do sistema  ›", () -> {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (Exception e) { android.widget.Toast.makeText(this, "Configurações indisponíveis", 0).show(); }
        });
        options.addView(label("Mais widgets e organização personalizada: em desenvolvimento.", 14));
        scroll.addView(options);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    @Override public void onBackPressed() { showHome(); }
}
