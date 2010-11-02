package com.tmobile.themechooser;

import com.tmobile.themes.provider.ThemeItem;
import com.tmobile.themes.provider.Themes;
import com.tmobile.themes.widget.ThemeAdapter;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

public class ThemeChooser extends ListActivity {
    private static final String TAG = ThemeChooser.class.getSimpleName();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        ThemeChooserAdapter adapter = new ThemeChooserAdapter(this);
        adapter.setUseAutomaticMarking(true, null);
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView list, View v, int position, long id) {
        ThemeItem item = (ThemeItem)list.getItemAtPosition(position);
        Uri uri = item.getUri(this);
        Log.i(TAG, "Sending request to change to '" + item.getName() + "' (" + uri + ")");
        Themes.changeTheme(this, uri);
    }

    private static class ThemeChooserAdapter extends ThemeAdapter {
        public ThemeChooserAdapter(Activity context) {
            super(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ThemeItem themeItem = mDAOItem;
            TextView vv = (TextView)view;
            String text = themeItem.getName();
            if (getMarkedPosition() == cursor.getPosition()) {
                text += " (current)";
            }
            vv.setText(text);
        }

        @Override
        public Object getItem(int position) {
            return getDAOItem(position);
        }
    }
}
