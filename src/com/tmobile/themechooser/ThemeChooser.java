package com.tmobile.themechooser;

import com.tmobile.themes.provider.ThemeItem;
import com.tmobile.themes.provider.Themes;
import com.tmobile.themes.widget.ThemeAdapter;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;

public class ThemeChooser extends Activity {
    private static final String TAG = ThemeChooser.class.getSimpleName();

    private Gallery mGallery;
    private TextView mThemeNameView;

    private ThemeChooserAdapter mAdapter;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);

        mThemeNameView = (TextView)findViewById(R.id.theme_name);

        mAdapter = new ThemeChooserAdapter(this);
        mAdapter.setUseAutomaticMarking(true, null);

        mGallery = (Gallery)findViewById(R.id.gallery);
        mGallery.setAdapter(mAdapter);
        mGallery.setOnItemSelectedListener(mItemSelected);

        Button button = (Button)findViewById(R.id.apply);
        button.setOnClickListener(mApplyClicked);
    }

    private final OnItemSelectedListener mItemSelected = new OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            ThemeItem item = (ThemeItem)parent.getItemAtPosition(position);
            String text = item.getName();
            if (mAdapter.getMarkedPosition() == position) {
                text += " (current)";
            }
            mThemeNameView.setText(text);
        }

        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    private final OnClickListener mApplyClicked = new OnClickListener() {
        public void onClick(View v) {
            int selectedPos = mGallery.getSelectedItemPosition();
            ThemeItem item = (ThemeItem)mGallery.getItemAtPosition(selectedPos);
            Uri uri = item.getUri(ThemeChooser.this);
            Log.i(TAG, "Sending request to change to '" + item.getName() + "' (" + uri + ")");
            Themes.changeTheme(ThemeChooser.this, uri);
        }
    };

    private static class ThemeChooserAdapter extends ThemeAdapter {
        public ThemeChooserAdapter(Activity context) {
            super(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.theme_item, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ThemeItem themeItem = mDAOItem;
            ImageView vv = (ImageView)view;
            vv.setImageURI(themeItem.getPreviewUri());
        }

        @Override
        public Object getItem(int position) {
            return getDAOItem(position);
        }
    }
}
