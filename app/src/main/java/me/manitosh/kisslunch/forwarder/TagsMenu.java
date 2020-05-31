package me.manitosh.kisslunch.forwarder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import me.manitosh.kisslunch.KissApplication;
import me.manitosh.kisslunch.MainActivity;
import me.manitosh.kisslunch.R;
import me.manitosh.kisslunch.TagsHandler;
import me.manitosh.kisslunch.dataprovider.simpleprovider.TagsProvider;
import me.manitosh.kisslunch.ui.ListPopup;

/**
 * Created by TBog on 5/8/2018.
 */
public class TagsMenu extends Forwarder {
    private final Set<String> tagList;
    private ListPopup popupMenu = null;

    public TagsMenu(MainActivity mainActivity) {
        super(mainActivity);
        tagList = new TreeSet<>();
    }

    public void onCreate() {
        loadTags();
    }

    public void onResume() {
        loadTags();
    }

    public boolean isTagMenuEnabled() {
        return prefs.getBoolean("pref-tags-menu", false);
    }

    public boolean isAutoDismiss()
    {
        return prefs.getBoolean("pref-tags-menu-dismiss", false);
    }

    private void loadTags() {
        if (isTagMenuEnabled())
        	setTags(getPrefTags(prefs, mainActivity));
        else
            setTags(null);
    }

    @NonNull
    public static Set<String> getPrefTags(SharedPreferences prefs, Context context) {
        Set<String> prefTags = getPrefTags(prefs);
        if (prefTags == null || prefTags.isEmpty()) {
            Set<String> tags = new HashSet<>(5);
            TagsHandler tagsHandler = KissApplication.getApplication(context).getDataHandler().getTagsHandler();
            Set<String> list = tagsHandler.getAllTagsAsSet();
            for (String tag : list) {
                tags.add(tag);
                if (tags.size() >= 5)
                    break;
            }
            return tags;
        }
        return prefTags;
    }

    @Nullable
    static Set<String> getPrefTags(SharedPreferences prefs) {
        return prefs.getStringSet("pref-toggle-tags-list", null);
    }

    private void setTags(Set<String> list) {
        tagList.clear();
        if (list != null)
            tagList.addAll(list);
    }

    interface MenuItem {
        @LayoutRes
        int getLayoutResource();
    }

    static class MenuItemDivider implements MenuItem {
        @Override
        public int getLayoutResource() {
            return R.layout.popup_divider;
        }
    }

    static class MenuItemTitle implements MenuItem {
        final String name;

        MenuItemTitle(Context context, @StringRes int nameRes) {
            this.name = context.getString(nameRes);
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int getLayoutResource() {
            return R.layout.popup_title;
        }
    }

    static class MenuItemTag implements MenuItem {
        final String tag;

        MenuItemTag(String tag) {
            this.tag = tag;
        }

        @Override
        public String toString() {
            return tag;
        }

        @Override
        public int getLayoutResource() {
            return R.layout.popup_list_item;
        }
    }

    static class MenuItemBtn implements MenuItem {
        final int nameRes;
        final String name;

        MenuItemBtn(Context context, @StringRes int nameRes) {
            this.nameRes = nameRes;
            this.name = context.getString(nameRes);
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int getLayoutResource() {
            return R.layout.popup_list_item;
        }
    }

    static class MenuAdapter extends BaseAdapter {
        final ArrayList<MenuItem> list = new ArrayList<>(0);

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public MenuItem getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @SuppressLint("ViewHolder")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MenuItem item = getItem(position);
            String text = item.toString();
            convertView = LayoutInflater.from(parent.getContext()).inflate(item.getLayoutResource(), parent, false);
            if (item instanceof MenuItemDivider) {
                return convertView;
            }

            TextView textView = convertView.findViewById(android.R.id.text1);
            textView.setText(text);

            return convertView;
        }

        public void add(MenuItem item) {
            list.add(item);
            notifyDataSetChanged();
        }

        public void add(int index, MenuItem item) {
            list.add(index, item);
            notifyDataSetChanged();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            MenuItem item = list.get(position);
            return (item instanceof MenuItemTag) || (item instanceof MenuItemBtn);
        }
    }

    public ListPopup showMenu(final View anchor) {
        if (popupMenu != null) {
            popupMenu.show(anchor, 0f);
            return popupMenu;
        }

        Context context = anchor.getContext();
        popupMenu = new ListPopup(context);
        MenuAdapter adapter = new MenuAdapter();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        //build menu
        adapter.add(new MenuItemTitle(context, R.string.popup_tags_title));
        for (String tag : tagList) {
            adapter.add(new MenuItemTag(tag));
        }

        // remember where the title should go
        int actionsTitlePosition = adapter.getCount();
        if (!prefs.getBoolean("history-onclick", false))
            adapter.add(new MenuItemBtn(context, R.string.show_history));
        if (prefs.getBoolean("pref-show-untagged", false))
            adapter.add(new MenuItemBtn(context, R.string.show_untagged));
        // insert title only if at least an action was added
        if (actionsTitlePosition != adapter.getCount())
            adapter.add(actionsTitlePosition, new MenuItemTitle(context, R.string.popup_tags_actions));

        adapter.add(new MenuItemDivider());
        adapter.add(new MenuItemBtn(context, R.string.ctx_menu));

        // set popup interaction rules
        popupMenu.setAdapter(adapter);
        popupMenu.setDismissOnItemClick( isAutoDismiss() );
        popupMenu.setOnItemClickListener(new ListPopup.OnItemClickListener() {
            @Override
            public void onItemClick(ListAdapter adapter, View view, int position) {
                Object adapterItem = adapter.getItem(position);
                if (adapterItem instanceof MenuItemTag) {
                    MenuItemTag item = (MenuItemTag) adapterItem;
                    // show only apps that match this tag
                    mainActivity.showMatchingTags(item.tag);
                } else if (adapterItem instanceof MenuItemBtn) {
                    switch (((MenuItemBtn) adapterItem).nameRes) {
                        case R.string.ctx_menu:
                            if (popupMenu != null)
                                popupMenu.dismiss();
                            popupMenu = null;
                            anchor.showContextMenu();
                            break;
                        case R.string.show_history:
                            mainActivity.showHistory();
                            break;
                        case R.string.show_untagged:
                            mainActivity.showUntagged();
                            break;
                    }
                }
            }
        });
        popupMenu.setOnItemLongClickListener((adapter1, view, position) -> {
            Object adapterItem = adapter1.getItem(position);
            if (adapterItem instanceof MenuItemTag) {
                MenuItemTag item = (MenuItemTag) adapterItem;
                String msg = mainActivity.getResources().getString(R.string.toast_favorites_added);
                KissApplication.getApplication(mainActivity).getDataHandler().addToFavorites(TagsProvider.generateUniqueId(item.tag));
                mainActivity.onFavoriteChange();
                Toast.makeText(mainActivity, String.format(msg, item.tag), Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        popupMenu.show(anchor, 0f);
        return popupMenu;
    }
}