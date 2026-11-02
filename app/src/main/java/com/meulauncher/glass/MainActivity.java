package com.meulauncher.glass;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    SharedPreferences prefs;
    LinearLayout root, menu, content;
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    GradientDrawable glass(){
        int alpha=prefs.getInt("opacity",110);
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.argb(alpha,155,195,230),Color.argb(alpha/2,50,70,110),0x99313a58});
        g.setCornerRadius(dp(18));
        g.setStroke(dp(1),0x99d9e8ff);
        return g;
    }
    TextView label(String text,int size){
        TextView t=new TextView(this);t.setText(text);t.setTextColor(Color.WHITE);
        t.setTextSize(size);t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(15),dp(9),dp(15),dp(9));return t;
    }
    TextView action(String text,Runnable run){
        TextView t=label(text,17);t.setBackground(glass());t.setFocusable(true);
        t.setOnFocusChangeListener((v,f)->{v.setScaleX(f?1.04f:1);v.setScaleY(f?1.04f:1);});
        t.setOnClickListener(v->run.run());return t;
    }
    LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    void add(LinearLayout parent,View view,int h){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(h));p.bottomMargin=dp(8);parent.addView(view,p);
    }
    @Override public void onCreate(Bundle state){super.onCreate(state);prefs=getSharedPreferences("glass",MODE_PRIVATE);home();}
    void frame(){
        root=new LinearLayout(this);root.setPadding(dp(18),dp(18),dp(18),dp(18));
        int theme=prefs.getInt("theme",0);
        root.setBackgroundColor(theme==1?0xff13131d:0xff102447);
        menu=vertical();menu.setPadding(dp(8),dp(8),dp(8),dp(8));menu.setBackground(glass());
        root.addView(menu,new LinearLayout.LayoutParams(dp(205),-1));
        content=vertical();content.setPadding(dp(18),0,0,0);
        root.addView(content,new LinearLayout.LayoutParams(0,-1,1));
        setContentView(root);
        add(menu,action("⌂  Início",()->home()),48);
        add(menu,action("▦  Meus Apps",()->apps()),48);
        add(menu,action("▶  Filmes e Séries",()->apps()),48);
        add(menu,action("♫  Música",()->apps()),48);
        add(menu,action("☆  Favoritos",()->apps()),48);
        add(menu,action("⚙  Configurações",()->settings()),48);
    }
    List<ResolveInfo> installed(){
        PackageManager pm=getPackageManager();
        Intent tv=new Intent(Intent.ACTION_MAIN);tv.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> list=new ArrayList<>(pm.queryIntentActivities(tv,0));
        Intent mobile=new Intent(Intent.ACTION_MAIN);mobile.addCategory(Intent.CATEGORY_LAUNCHER);
        for(ResolveInfo item:pm.queryIntentActivities(mobile,0)){
            boolean exists=false;
            for(ResolveInfo other:list)if(other.activityInfo.packageName.equals(item.activityInfo.packageName))exists=true;
            if(!exists)list.add(item);
        }
        Collections.sort(list,new ResolveInfo.DisplayNameComparator(pm));return list;
    }
    void launch(ResolveInfo item){
        try{
            Intent i=new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setClassName(item.activityInfo.packageName,item.activityInfo.name);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);
        }catch(Exception e){Toast.makeText(this,"Não foi possível abrir o aplicativo",Toast.LENGTH_SHORT).show();}
    }
    void home(){
        frame();
        LinearLayout header=new LinearLayout(this);
        TextView title=label("TCL Google TV",27);title.setTypeface(null,Typeface.BOLD);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(60),1));
        header.addView(label(new java.text.SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()),22));
        content.addView(header);
        LinearLayout banner=vertical();banner.setPadding(dp(18),dp(12),dp(18),dp(12));banner.setBackground(glass());
        banner.addView(label("SUGESTÕES DE FILMES E SÉRIES",15));
        banner.addView(label("Descubra novas histórias",29));
        banner.addView(label("Explore seus aplicativos de streaming",18));
        add(banner,action("Ver aplicativos ▶",()->apps()),50);
        add(content,banner,205);
        add(content,label("Seus aplicativos",21),45);
        HorizontalScrollView scroll=new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout strip=new LinearLayout(this);
        PackageManager pm=getPackageManager();
        List<ResolveInfo> apps=installed();
        for(int i=0;i<Math.min(12,apps.size());i++){
            ResolveInfo item=apps.get(i);
            LinearLayout tile=vertical();tile.setGravity(Gravity.CENTER);tile.setBackground(glass());
            ImageView icon=new ImageView(this);
            try{icon.setImageDrawable(item.loadIcon(pm));}catch(Exception ignored){}
            tile.addView(icon,new LinearLayout.LayoutParams(dp(55),dp(55)));
            TextView name=label(item.loadLabel(pm).toString(),12);
            name.setSingleLine(true);name.setGravity(Gravity.CENTER);
            tile.addView(name);
            tile.setFocusable(true);tile.setOnClickListener(v->launch(item));
            tile.setOnFocusChangeListener((v,f)->{v.setScaleX(f?1.04f:1);v.setScaleY(f?1.04f:1);});
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(125),dp(120));p.rightMargin=dp(10);
            strip.addView(tile,p);
        }
        scroll.addView(strip);content.addView(scroll);
        if(prefs.getBoolean("categories",true)){
            add(content,label("Categorias",21),45);
            add(content,action("Filmes • Música • Jogos • TV ao Vivo",()->apps()),60);
        }
    }
    void apps(){
        frame();add(content,label("Meus aplicativos",27),60);
        ScrollView sc=new ScrollView(this);LinearLayout list=vertical();
        for(ResolveInfo app:installed())add(list,action(app.loadLabel(getPackageManager()).toString(),()->launch(app)),52);
        sc.addView(list);content.addView(sc);
    }
    void settings(){
        frame();add(content,label("Personalização",27),60);
        ScrollView sc=new ScrollView(this);LinearLayout list=vertical();
        add(list,action("Estilo Liquid Glass",()->new AlertDialog.Builder(this).setTitle("Estilo")
            .setItems(new String[]{"Azul","Escuro"},(d,n)->{prefs.edit().putInt("theme",n).apply();settings();}).show()),62);
        add(list,action("Transparência",()->new AlertDialog.Builder(this).setTitle("Transparência")
            .setItems(new String[]{"Leve","Média","Forte"},(d,n)->{
                prefs.edit().putInt("opacity",new int[]{60,110,180}[n]).apply();settings();
            }).show()),62);
        add(list,action("Mostrar / ocultar categorias",()->{
            prefs.edit().putBoolean("categories",!prefs.getBoolean("categories",true)).apply();settings();
        }),62);
        add(list,action("Restaurar aparência",()->new AlertDialog.Builder(this)
            .setMessage("Restaurar as configurações?")
            .setPositiveButton("Restaurar",(d,n)->{prefs.edit().clear().apply();settings();})
            .setNegativeButton("Cancelar",null).show()),62);
        sc.addView(list);content.addView(sc);
    }
    @Override public void onBackPressed(){home();}
}
