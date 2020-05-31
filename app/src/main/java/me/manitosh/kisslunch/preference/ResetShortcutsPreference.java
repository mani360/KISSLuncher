package me.manitosh.kisslunch.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import me.manitosh.kisslunch.R;
import me.manitosh.kisslunch.utils.ShortcutUtil;

public class ResetShortcutsPreference extends DialogPreference {

    public ResetShortcutsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Remove all shortcuts
            ShortcutUtil.removeAllShortcuts(getContext());

            // Build all shortcuts
            ShortcutUtil.addAllShortcuts(getContext());

            Toast.makeText(getContext(), R.string.regenerate_shortcuts_done, Toast.LENGTH_LONG).show();
        }
    }
}
