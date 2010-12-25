/*
 * Copyright (C) 2010, T-Mobile USA, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tmobile.themechooser;

import com.tmobile.themes.provider.ThemeItem;
import com.tmobile.themes.provider.Themes;
import com.tmobile.themes.widget.ThemeAdapter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
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

    private static final int DIALOG_APPLY = 0;
    private final ChangeThemeHelper mChangeHelper = new ChangeThemeHelper(this, DIALOG_APPLY);

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);

        mThemeNameView = (TextView)findViewById(R.id.theme_name);

        mAdapter = new ThemeChooserAdapter(this);
        mAdapter.setUseAutomaticMarking(true, null);

        mGallery = (Gallery)findViewById(R.id.gallery);
        mGallery.setAdapter(mAdapter);
        mGallery.setSelection(mAdapter.getMarkedPosition());
        mGallery.setOnItemSelectedListener(mItemSelected);

        Button button = (Button)findViewById(R.id.apply);
        button.setOnClickListener(mApplyClicked);

        mChangeHelper.dispatchOnCreate();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangeHelper.dispatchOnConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        mChangeHelper.dispatchOnResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mChangeHelper.dispatchOnPause();
        super.onPause();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        return mChangeHelper.dispatchOnCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        mChangeHelper.dispatchOnPrepareDialog(id, dialog);
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
            mChangeHelper.beginChange(item.getName());
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
