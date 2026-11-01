package com.meulauncher.glass;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private GradientDrawable glass() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(105, 190, 210, 235));
        bg.setCornerRadius(28);
        bg.setStroke(2, Color.argb(110, 255, 255, 255));
        return bg;
    }

    private TextView item(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(size);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(24, 12, 24, 12);
        view.setFocusable(true);
        return view;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(Color.rgb(18, 25, 45));

        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setBackground(glass());

        String[] options = {
            "Início", "Meus Apps", "Filmes e Séries",
            "TV ao Vivo", "Jogos", "Música",
            "Favoritos", "Pesquisar", "Configurações"
        };

        for (String option : options) {
            menu.addView(item(option, 16));
        }

        root.addView(menu, new LinearLayout.LayoutParams(
            250, LinearLayout.LayoutParams.MATCH_PARENT));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(24, 0, 0, 0);

        TextView title = item("TCL Google TV", 26);
        title.setTypeface(null, Typeface.BOLD);
        content.addView(title);

        TextView featured = item(
            "DIVERTIDA MENTE 2\n\n" +
            "Já disponível no Disney+\n\n" +
            "Assistir agora     Mais detalhes", 24);
        featured.setBackground(glass());

        content.addView(featured,
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 2));

        TextView apps = item(
            "Aplicativos favoritos\n\n" +
            "Netflix   YouTube   Disney+   Prime Video   Max", 19);
        apps.setBackground(glass());

        LinearLayout.LayoutParams appParams =
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        appParams.topMargin = 18;
        content.addView(apps, appParams);

        root.addView(content,
            new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1));

        setContentView(root);
    }
}
