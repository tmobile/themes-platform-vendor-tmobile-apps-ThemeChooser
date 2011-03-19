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

import com.tmobile.themes.ThemeManager;
import com.tmobile.themes.provider.ThemeItem;
import com.tmobile.themes.provider.Themes;
import com.tmobile.themes.widget.ThemeAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

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

        Uri currentTheme = getIntent().getParcelableExtra(ThemeManager.EXTRA_THEME_EXISTING_URI);
        mAdapter = new ThemeChooserAdapter(this);
        mAdapter.setUseAutomaticMarking(true, currentTheme);

        inflateActivity();

        mGallery.setSelection(mAdapter.getMarkedPosition());
        mChangeHelper.dispatchOnCreate();
    }

    private void inflateActivity() {
        setContentView(R.layout.main);

        mCurrentPositionView = (TextView)findViewById(R.id.adapter_position);
        mThemeNameView = (TextView)findViewById(R.id.theme_name);

        mGallery = (Gallery)findViewById(R.id.gallery);
        mGallery.setAdapter(mAdapter);
        mGallery.setOnItemSelectedListener(mItemSelected);

        Button button = (Button)findViewById(R.id.apply);
        button.setOnClickListener(mApplyClicked);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Handle config changes ourselves, to avoid possible race condition
        // during theme change when app gets torn down and rebuilt due
        // to orientation change.
        boolean finishing = mChangeHelper.dispatchOnConfigurationChanged(newConfig);

        // If it's an orientation change and not a theme change,
        // re-inflate ThemeChooser with its new resources
        if (!finishing) {
            // re-inflating will cause our list positions and selections
            // to be lost, so request all Views in the window save their
            // instance state first.
            Bundle state = new Bundle();
            onSaveInstanceState(state);

            // Set the adapter null, so that on reinflating mGallery,
            // the previous mDataSetObserver gets unregistered, and we
            // don't leak a reference to the gallery on each config change.
            mGallery.setAdapter(null);
            inflateActivity();

            // Now have window restore previous instance state... just as
            // though it went through onDestroy/onCreate process.
            onRestoreInstanceState(state);
        }
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
            mCurrentPositionView.setText(getString(R.string.item_count,
                    (position + 1), mAdapter.getCount()));
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
            if (Intent.ACTION_PICK.equals(getIntent().getAction())) {
                Intent i = new Intent(null, item.getUri(ThemeChooser.this));
                setResult(Activity.RESULT_OK, i);
                finish();
            } else {
                doApply(item);
            }
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
            int orientation = context.getResources().getConfiguration().orientation;
            holder.preview.setImageURI(themeItem.getPreviewUri(orientation));
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
