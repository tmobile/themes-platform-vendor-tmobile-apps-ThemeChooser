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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
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
    private TextView mCurrentPositionView;

    private ThemeChooserAdapter mAdapter;

    private static final int DIALOG_APPLY = 0;
    private static final int DIALOG_MISSING_HOST_DENSITY = 1;
    private static final int DIALOG_MISSING_THEME_PACKAGE_SCOPE = 2;
    private final ChangeThemeHelper mChangeHelper = new ChangeThemeHelper(this, DIALOG_APPLY);

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);

        mCurrentPositionView = (TextView)findViewById(R.id.adapter_position);
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
        AlertDialog.Builder builder;
        switch (id) {
            case DIALOG_MISSING_HOST_DENSITY:
                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.dialog_theme_error_title);
                builder.setMessage(R.string.dialog_missing_host_density_msg);
                builder.setPositiveButton(R.string.dialog_apply_anyway_btn, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        int selectedPos = mGallery.getSelectedItemPosition();
                        ThemeItem item = (ThemeItem)mGallery.getItemAtPosition(selectedPos);
                        doApply(item);
                    }
                });
                builder.setNegativeButton(R.string.dialog_bummer_btn, null);
                return builder.create();
            case DIALOG_MISSING_THEME_PACKAGE_SCOPE:
                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.dialog_theme_error_title);
                builder.setMessage(R.string.dialog_missing_theme_package_scope_msg);
                builder.setPositiveButton(android.R.string.ok, null);
                return builder.create();
            default:
                return mChangeHelper.dispatchOnCreateDialog(id);
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        mChangeHelper.dispatchOnPrepareDialog(id, dialog);
    }

    private final OnItemSelectedListener mItemSelected = new OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            ThemeItem item = (ThemeItem)parent.getItemAtPosition(position);
            mCurrentPositionView.setText((position + 1) + "/" + parent.getCount());
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
            if (!item.hasHostDensity()) {
                showDialog(DIALOG_MISSING_HOST_DENSITY);
                return;
            }
            if (!item.hasThemePackageScope()) {
                showDialog(DIALOG_MISSING_THEME_PACKAGE_SCOPE);
                return;
            }
            doApply(item);
        }
    };

    private void doApply(ThemeItem item) {
        Uri uri = item.getUri(ThemeChooser.this);
        Log.i(TAG, "Sending request to change to '" + item.getName() + "' (" + uri + ")");
        mChangeHelper.beginChange(item.getName());
        if (getResources().getBoolean(R.bool.config_change_style_only)) {
            Themes.changeStyle(ThemeChooser.this, uri);
        } else {
            Themes.changeTheme(ThemeChooser.this, uri);
        }
    }

    private static class ThemeChooserAdapter extends ThemeAdapter {
        public ThemeChooserAdapter(Activity context) {
            super(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View row = LayoutInflater.from(context).inflate(R.layout.theme_item, parent, false);
            row.setTag(new ViewHolder(row));
            return row;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ThemeItem themeItem = mDAOItem;
            ViewHolder holder = (ViewHolder)view.getTag();
            holder.preview.setImageURI(themeItem.getPreviewUri());
        }

        @Override
        public Object getItem(int position) {
            return getDAOItem(position);
        }
    }

    private static class ViewHolder {
        public ImageView preview;

        public ViewHolder(View row) {
            preview = (ImageView)row.findViewById(R.id.theme_preview);
        }
    }
}
