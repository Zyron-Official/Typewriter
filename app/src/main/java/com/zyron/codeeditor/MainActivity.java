package com.zyron.codeeditor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem; 
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.google.android.material.appbar.MaterialToolbar;
import com.zyron.codeeditor.databinding.ActivityMainBinding;
import com.zyron.typewriter.widget.CodeEditor;
import com.zyron.codeeditor.R;

public class MainActivity extends AppCompatActivity {
    
    private ActivityMainBinding binding;
    private CodeEditor editor;
    private MaterialToolbar toolbar;
    
    private static final int MENU_ITEM_UNDO = 1;
    private static final int MENU_ITEM_REDO = 2;
    private static final int MENU_ITEM_CUSTOMIZE = 3;  
    private static final int MENU_ITEM_FIND = 4; 
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        toolbar = (MaterialToolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Typewriter");
        
        editor = binding.codeEditor;
        editor.requestFocus();
        editor.requestFocusFromTouch();
        editor.setTypeface(ResourcesCompat.getFont(this, R.font.jetbrains_mono));
        editor.setGutterTextAlign(Paint.Align.RIGHT);
//        editor.setText("System.out.println(Salaam Duniya);");
        editor.setGutterEnabled(true);
        editor.setDividerLineEnabled(true);
        editor.setTextSize(10f);
    }
    

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuItem undo = menu.add(Menu.NONE, MENU_ITEM_UNDO, 0, "Undo");
        undo.setIcon(R.drawable.ic_undo);
        undo.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        MenuItem redo = menu.add(Menu.NONE, MENU_ITEM_REDO, 1, "Redo");
        redo.setIcon(R.drawable.ic_redo);
        redo.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        MenuItem customize = menu.add(Menu.NONE, MENU_ITEM_CUSTOMIZE, 2, "Customize");
        customize.setIcon(R.drawable.ic_customize);
        customize.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        MenuItem find = menu.add(Menu.NONE, MENU_ITEM_FIND, 3, "Find");
        find.setIcon(R.drawable.ic_content_cut);
        find.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        if (editor.isUndo()) {
            undo.setEnabled(true);
        } else {
            undo.setEnabled(false);
        }

        if (editor.isRedo()) {
            redo.setEnabled(true);
        } else {
            redo.setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        invalidateOptionsMenu();
        MenuBuilder menuBuilder = (MenuBuilder) toolbar.getMenu();
        menuBuilder.setOptionalIconsVisible(true);
        return super.onCreateOptionsMenu(menu); 
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_ITEM_UNDO) {
            editor.onUndo();
        } else if (item.getItemId() == MENU_ITEM_REDO) {
            editor.onRedo();
        } else if (item.getItemId() == MENU_ITEM_CUSTOMIZE) {
            System.out.print("Hello");
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }
}