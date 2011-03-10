package com.tmobile.themechooser;

import com.tmobile.themes.ThemeManager;
import com.tmobile.themes.provider.Themes.ThemeColumns;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class ThemeChooserActions {

    /**
     * Start the ThemeChooser application with the preselectTheme being the
     * one centered in the gallery.  Allow the User to select a theme,
     * and when User presses Apply, begin a theme change that applies
     * the theme to the device immediately.
     *
     * @param context - A context from which to start the ThemeChooser.
     * @param preselectTheme - The theme to center in the Chooser
     * gallery upon startup.
     */
    public static void chooseThemeAndApply(Context context, Uri preselectTheme) {
        Intent i = new Intent();
        i.setAction(ThemeManager.ACTION_SET_THEME);
        i.putExtra(ThemeManager.EXTRA_THEME_EXISTING_URI, preselectTheme);
        context.startActivity(i);
    }

    /**
     * Start the ThemeChooser application with the preselectTheme being the
     * one centered in the gallery.  Allow the User to select a theme,
     * and when User presses Apply, return an intent with the URI of the
     * theme chosen.  The theme is NOT applied.
     * <p>
     * The ThemeChooser is started for result and the result can be
     * retrieved by {@link Activity#onActivityResult}.  The values returned
     * are: requestCode - the requestCode parameter provided, resultCode -
     * Activity.RESULT_OK only if a theme was chosen, data - The data
     * intent's getData() will be the chosen theme URI.
     *
     * @param activity - An activity from which to start the ThemeChooser and
     * to which results will be returned.
     * @param preselectTheme - The theme to center in the Chooser
     * gallery upon startup.
     * @param requestCode - The code that will be returned through
     * {@link Activity#onActivityResult} when the ThemeChooser exits.
     */
    public static void chooseThemeAndReturnResult(Activity activity, Uri preselectTheme, int requestCode) {
        Intent i = new Intent();
        i.setAction(Intent.ACTION_PICK);
        i.setType(ThemeColumns.CONTENT_TYPE);
        i.putExtra(ThemeManager.EXTRA_THEME_EXISTING_URI, preselectTheme);
        activity.startActivityForResult(i, requestCode);
    }
}
