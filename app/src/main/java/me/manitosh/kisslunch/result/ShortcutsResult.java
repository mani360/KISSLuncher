package me.manitosh.kisslunch.result;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import me.manitosh.kisslunch.DataHandler;
import me.manitosh.kisslunch.KissApplication;
import me.manitosh.kisslunch.R;
import me.manitosh.kisslunch.adapter.RecordAdapter;
import me.manitosh.kisslunch.pojo.ShortcutPojo;
import me.manitosh.kisslunch.ui.ListPopup;
import me.manitosh.kisslunch.utils.FuzzyScore;
import me.manitosh.kisslunch.utils.SpaceTokenizer;
import me.manitosh.kisslunch.utils.DrawableUtils;

import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC;
import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST;
import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED;

public class ShortcutsResult extends Result {
    private final ShortcutPojo shortcutPojo;

    ShortcutsResult(ShortcutPojo shortcutPojo) {
        super(shortcutPojo);
        this.shortcutPojo = shortcutPojo;
    }

    @NonNull
    @Override
    @SuppressWarnings("CatchAndPrintStackTrace")
    public View display(final Context context, View view, @NonNull ViewGroup parent, FuzzyScore fuzzyScore) {
        if (view == null)
            view = inflateFromId(context, R.layout.item_shortcut, parent);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        TextView shortcutName = view.findViewById(R.id.item_app_name);

        displayHighlighted(shortcutPojo.normalizedName, shortcutPojo.getName(), fuzzyScore, shortcutName, context);

        TextView tagsView = view.findViewById(R.id.item_app_tag);

        // Hide tags view if tags are empty
        if (shortcutPojo.getTags().isEmpty()) {
            tagsView.setVisibility(View.GONE);
        } else if (displayHighlighted(shortcutPojo.getNormalizedTags(), shortcutPojo.getTags(),
                fuzzyScore, tagsView, context) || prefs.getBoolean("tags-visible", true)) {
            tagsView.setVisibility(View.VISIBLE);
        } else {
            tagsView.setVisibility(View.GONE);
        }

        final ImageView shortcutIcon = view.findViewById(R.id.item_shortcut_icon);
        final ImageView appIcon = view.findViewById(R.id.item_app_icon);

        // Retrieve package icon for this shortcut
        final PackageManager packageManager = context.getPackageManager();
        Drawable appDrawable = null;
        try {
            Intent intent = Intent.parseUri(shortcutPojo.intentUri, 0);
            List<ResolveInfo> packages = packageManager.queryIntentActivities(intent, 0);
            if (packages.size() > 0) {
                ResolveInfo mainPackage = packages.get(0);
                String packageName = mainPackage.activityInfo.applicationInfo.packageName;
                String activityName = mainPackage.activityInfo.name;
                ComponentName className = new ComponentName(packageName, activityName);
                appDrawable = context.getPackageManager().getActivityIcon(className);
            } else {
                // Can't make sense of the intent URI (Oreo shortcut, or a shortcut from an activity that was removed from an installed app)
                // Retrieve app icon
                try {
                    appDrawable = packageManager.getApplicationIcon(shortcutPojo.packageName);
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return view;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return view;
        }

        if (!prefs.getBoolean("icons-hide", false)) {
            Drawable shortcutDrawable = getDrawable(context);
            String iconsPack = prefs.getString("icons-pack", "default");
            if(DrawableUtils.isIconsPackAdaptive(iconsPack)) {
                appDrawable = DrawableUtils.handleAdaptiveIcons(context, appDrawable);
                if(shortcutDrawable != null) {
                    shortcutDrawable = DrawableUtils.handleAdaptiveIcons(context, shortcutDrawable);
                }
            }
            if (shortcutDrawable != null) {
                shortcutIcon.setImageDrawable(shortcutDrawable);
                appIcon.setImageDrawable(appDrawable);
            } else {
                // No icon for this shortcut, use app icon
                shortcutIcon.setImageDrawable(appDrawable);
                appIcon.setImageResource(android.R.drawable.ic_menu_send);
            }
            if(!prefs.getBoolean("subicon-visible", true)) {
                appIcon.setVisibility(View.GONE);
            }
        }
        else {
            appIcon.setImageDrawable(null);
            shortcutIcon.setImageDrawable(null);
        }

        return view;
    }

    public Drawable getDrawable(Context context) {
        Drawable shortcutDrawable = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcherApps != null;

            if (launcherApps.hasShortcutHostPermission()) {
                LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
                query.setPackage(shortcutPojo.packageName);
                query.setShortcutIds(Collections.singletonList(shortcutPojo.getOreoId()));
                query.setQueryFlags(FLAG_MATCH_DYNAMIC | FLAG_MATCH_MANIFEST | FLAG_MATCH_PINNED);

                List<UserHandle> userHandles = launcherApps.getProfiles();

                // Find the correct UserHandle, and retrieve the icon.
                for (UserHandle userHandle : userHandles) {
                    List<ShortcutInfo> shortcuts = launcherApps.getShortcuts(query, userHandle);
                    if (shortcuts != null && shortcuts.size() > 0) {
                        shortcutDrawable = launcherApps.getShortcutIconDrawable(shortcuts.get(0), 0);
                    }
                }
            }
        }

        return shortcutDrawable;
    }


    @Override
    protected void doLaunch(Context context, View v) {
        if (shortcutPojo.isOreoShortcut()) {
            // Oreo shortcuts
            doOreoLaunch(context, v);
        } else {
            // Pre-oreo shortcuts
            try {
                Intent intent = Intent.parseUri(shortcutPojo.intentUri, 0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    intent.setSourceBounds(v.getClipBounds());
                }

                context.startActivity(intent);
            } catch (Exception e) {
                // Application was just removed?
                Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void doOreoLaunch(Context context, View v) {
        final LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        assert launcherApps != null;

        // Only the default launcher is allowed to start shortcuts
        if (!launcherApps.hasShortcutHostPermission()) {
            Toast.makeText(context, context.getString(R.string.shortcuts_no_host_permission), Toast.LENGTH_LONG).show();
            return;
        }

        LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
        query.setPackage(shortcutPojo.packageName);
        query.setShortcutIds(Collections.singletonList(shortcutPojo.getOreoId()));
        query.setQueryFlags(FLAG_MATCH_DYNAMIC | FLAG_MATCH_MANIFEST | FLAG_MATCH_PINNED);

        List<UserHandle> userHandles = launcherApps.getProfiles();

        // Find the correct UserHandle, and launch the shortcut.
        for (UserHandle userHandle : userHandles) {
            List<ShortcutInfo> shortcuts = launcherApps.getShortcuts(query, userHandle);
            if (shortcuts != null && shortcuts.size() > 0 && shortcuts.get(0).isEnabled()) {
                launcherApps.startShortcut(shortcuts.get(0), v.getClipBounds(), null);
                return;
            }
        }

        // Application removed? Invalid shortcut? Shortcut to an app on an unmounted SD card?
        Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
    }

    @Override
    ListPopup buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter, RecordAdapter parent, View parentView) {
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_add));
        adapter.add(new ListPopup.Item(context, R.string.menu_tags_edit));
        adapter.add(new ListPopup.Item(context, R.string.menu_remove));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_remove));
        adapter.add(new ListPopup.Item(context, R.string.menu_shortcut_remove));

        return inflatePopupMenu(adapter, context);
    }

    @Override
    boolean popupMenuClickHandler(Context context, RecordAdapter parent, int stringId, View parentView) {
        switch (stringId) {
            case R.string.menu_shortcut_remove:
                launchUninstall(context, shortcutPojo);
                // Also remove item, since it will be uninstalled
                parent.removeResult(context, this);
                return true;
            case R.string.menu_tags_edit:
                launchEditTagsDialog(context, shortcutPojo);
                return true;
        }
        return super.popupMenuClickHandler(context, parent, stringId, parentView);
    }

    private void launchEditTagsDialog(final Context context, final ShortcutPojo pojo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.tags_add_title));

        // Create the tag dialog
        final View v = View.inflate(context, R.layout.tags_dialog, null);
        final MultiAutoCompleteTextView tagInput = v.findViewById(R.id.tag_input);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, KissApplication.getApplication(context).getDataHandler().getTagsHandler().getAllTagsAsArray());
        tagInput.setTokenizer(new SpaceTokenizer());
        tagInput.setText(shortcutPojo.getTags());

        tagInput.setAdapter(adapter);
        builder.setView(v);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // Refresh tags for given app
                pojo.setTags(tagInput.getText().toString());
                KissApplication.getApplication(context).getDataHandler().getTagsHandler().setTags(pojo.id, pojo.getTags());
                // Show toast message
                String msg = context.getResources().getString(R.string.tags_confirmation_added);
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        dialog.show();
    }

    private void launchUninstall(Context context, ShortcutPojo shortcutPojo) {
        DataHandler dh = KissApplication.getApplication(context).getDataHandler();
        if (dh != null) {
            dh.removeShortcut(shortcutPojo);
        }
    }

}
