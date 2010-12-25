package com.tmobile.themechooser;

import com.tmobile.themes.ThemeManager;
import com.tmobile.themes.provider.Themes.ThemeColumns;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.res.Configuration;
import android.content.res.CustomTheme;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Utility class to centralize common logic found in the profile chooser, theme
 * chooser, and style chooser. This logic is designed to "seize" the user and
 * hold them onto the chooser screen until the change event has finished
 * processing.
 * <p>
 * To use, a new instance must be created with the activity and the various
 * on<Event> methods must be connected into the activity lifecycle.
 */
public class ChangeThemeHelper {
    private final Activity mContext;
    private final int mDialogId;

    /** Tracked to trap theme change configuration events */
    private CustomTheme mCurrentTheme;

    /**
     * Set when the "Select" button is clicked, used by the 'setting theme'
     * dialog to show the applying theme. This value is used only in that
     * situation and is not preserved across config changes.
     */
    private String mApplyingName;

    /**
     * Used to impose a short delay between theme change "completion" and the
     * actual finish() call to work around imprecisions inherent to detecing
     * theme change completion.
     */
    private final ChangeHandler mHandler = new ChangeHandler();

    public ChangeThemeHelper(Activity context, int dialogId) {
        mContext = context;
        mDialogId = dialogId;
    }

    public void dispatchOnCreate() {
        mCurrentTheme = mContext.getResources().getConfiguration().customTheme;
    }

    public void dispatchOnConfigurationChanged(Configuration newConfig) {
        /**
         * It is necessary to detect theme changes in this way (as well as via
         * the broadcast) in order to handle the case where the user leaves the
         * activity with the Home button in the middle of a theme change. When
         * the theme change event is received by this activity (when it is
         * brought back to the foreground), we need to finish automatically
         * rather than present a UI with a potentially stale theme applied.
         */
        CustomTheme newTheme = newConfig.customTheme;
        if (!CustomTheme.nullSafeEquals(newTheme, mCurrentTheme)) {
            mHandler.scheduleFinish("Theme config change, closing!");
        }
    }

    public void dispatchOnPause() {
        /*
         * If the user leaves this screen, just remove the progress dialog and
         * give them the "raw" experience (the device will be slow for a short
         * while)
         */
        mContext.removeDialog(mDialogId);
        mContext.unregisterReceiver(mThemeChangedReceiver);
    }

    public void dispatchOnResume() {
        /**
         * Register a receiver that will dismiss the dialog and finish this
         * activity when it is believed that theme change is complete (this is
         * an estimate, since theme change is never truly "complete", as it
         * doesn't update every running activity immediately).
         * <p>
         * If theme change occurs (from some other component) while we're
         * looking at this screen it will automatically finish(). Might seem
         * weird to the user, but it is a rare corner case and would be
         * difficult to handle correctly.
         */
        IntentFilter filter = new IntentFilter(ThemeManager.ACTION_THEME_CHANGED);
        try {
            filter.addDataType(ThemeColumns.CONTENT_ITEM_TYPE);
            filter.addDataType(ThemeColumns.STYLE_CONTENT_ITEM_TYPE);
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException(e);
        }
        mContext.registerReceiver(mThemeChangedReceiver, filter);
    }

    private final BroadcastReceiver mThemeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mHandler.scheduleFinish("Theme change 'complete', closing!");
        }
    };

    private class ChangeHandler extends Handler {
        private static final int MSG_FINISH_SCHEDULE = 0;
        private static final int MSG_FINISH_EXECUTE = 1;

        private static final int SCHEDULE_DELAY = 500;
        private static final int FINISH_DELAY = 500;
        private static final int TIMEOUT_DELAY = 10000;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                /*
                 * MSG_FINISH_SCHEDULE is no longer strictly needed in the open
                 * source tree, but here for legacy purposes.
                 */
                case MSG_FINISH_SCHEDULE:
                    final String message = (String)msg.obj;
                    removeMessages(MSG_FINISH_EXECUTE);
                    sendMessageDelayed(obtainMessage(MSG_FINISH_EXECUTE, message),
                            FINISH_DELAY);
                    break;
                case MSG_FINISH_EXECUTE:
                    handleThemeChangeSwitch((String)msg.obj);
                    break;
            }
        }

        /**
         * Schedule the finish event to occur after all receivers have finished
         * executing. This is a way to try to better time the stable state of
         * the Profile Manager screen (once wallpapers, ringtones, etc have all
         * been committed to the database). A minimum delay of
         * {@link #SCHEDULE_DELAY} is imposed before this is even attempted in
         * case something has gone horribly wrong with the receiver queue (such
         * as it being full and a new thread must be created for our task).
         * <p>
         * In addition the scheduling delay, {@link #FINISH_DELAY} is applied
         * before executing the finish() call.
         */
        public void scheduleFinish(String message) {
            removeMessages(MSG_FINISH_SCHEDULE);
            removeMessages(MSG_FINISH_EXECUTE);
            sendMessageDelayed(obtainMessage(MSG_FINISH_SCHEDULE, message), SCHEDULE_DELAY);
        }

        /**
         * Schedule a timeout that will invoke {@link #MSG_FINISH_EXECUTE} after
         * {@link #TIMEOUT_DELAY} inactivity. This is a catch-all to gracefully
         * handle certain unlikely error cases.
         */
        public void scheduleTimeout() {
            if (!hasMessages(MSG_FINISH_SCHEDULE) && !hasMessages(MSG_FINISH_EXECUTE)) {
                sendMessageDelayed(obtainMessage(MSG_FINISH_EXECUTE,
                        "Timed out waiting for theme change event."), TIMEOUT_DELAY);
            }
        }

        private void handleThemeChangeSwitch(String message) {
            if (Constants.DEBUG) {
                Log.i(Constants.TAG, message);
            }

            /*
             * Will dismiss if present, but doesn't require that it is currently
             * being shown. This is important because the user might have left
             * the screen while the dialog was up, or a third party might have
             * actually sent this broadcast (in response to a different event,
             * like maybe automatic profile switching).
             */
            mContext.removeDialog(mDialogId);

            mContext.finish();
        }
    }

    public Dialog dispatchOnCreateDialog(int id) {
        if (id == mDialogId) {
            ProgressDialog dialog = new ProgressDialog(mContext);
            dialog.setTitle(R.string.theme_change_dialog_title);
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            return dialog;
        } else {
            return null;
        }
    }

    public void dispatchOnPrepareDialog(int id, Dialog dialog) {
        if (mApplyingName != null) {
            ((ProgressDialog)dialog).setMessage(mContext.getResources().getString(
                    R.string.switching_to_theme, mApplyingName));
        }
    }

    public void beginChange(String applyingName) {
        mApplyingName = applyingName;
        mContext.showDialog(mDialogId);

        /*
         * If no theme change events are seen before the timeout occurs, dismiss
         * the dialog anyway. This is to hide some of the clumsier corners of
         * this implementation from the user. For instance, if a profile
         * specifies a theme that no longer exists, the change theme receiver
         * will return without sending a follow-up broadcast event that the
         * theme has been changed. Eventually this timeout will be reached and
         * we'll tidy up anyway, warning appropriately.
         */
        mHandler.scheduleTimeout();
    }
}
