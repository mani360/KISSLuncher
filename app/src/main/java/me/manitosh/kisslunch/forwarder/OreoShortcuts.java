package me.manitosh.kisslunch.forwarder;

import android.content.Intent;
import android.content.pm.LauncherApps;

import me.manitosh.kisslunch.MainActivity;
import me.manitosh.kisslunch.utils.ShortcutUtil;

class OreoShortcuts extends Forwarder {
    OreoShortcuts(MainActivity mainActivity) {
        super(mainActivity);
    }

    void onCreate() {
        // Shortcuts in Android O
        if (ShortcutUtil.areShortcutsEnabled(mainActivity)) {
            // On first run save all shortcuts
            if (prefs.getBoolean("first-run-shortcuts", true)) {
                if(mainActivity.isKissDefaultLauncher()) {
                    // Save all shortcuts
                    ShortcutUtil.addAllShortcuts(mainActivity);
                    // Set flag to falseX
                    prefs.edit().putBoolean("first-run-shortcuts", false).apply();
                }
            }

            Intent intent = mainActivity.getIntent();
            if (intent != null) {
                final String action = intent.getAction();
                if (LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT.equals(action)) {
                    // Save single shortcut via a pin request
                    ShortcutUtil.addShortcut(mainActivity, intent);
                }
            }
        }

    }
}
