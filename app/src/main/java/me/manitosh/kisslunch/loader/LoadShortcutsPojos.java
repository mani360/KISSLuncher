package me.manitosh.kisslunch.loader;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import me.manitosh.kisslunch.KissApplication;
import me.manitosh.kisslunch.TagsHandler;
import me.manitosh.kisslunch.db.DBHelper;
import me.manitosh.kisslunch.db.ShortcutRecord;
import me.manitosh.kisslunch.pojo.ShortcutPojo;
import me.manitosh.kisslunch.utils.ShortcutUtil;

public class LoadShortcutsPojos extends LoadPojos<ShortcutPojo> {

    private final TagsHandler tagsHandler;

    public LoadShortcutsPojos(Context context) {
        super(context, ShortcutPojo.SCHEME);
        tagsHandler = KissApplication.getApplication(context).getDataHandler().getTagsHandler();
    }

    @Override
    protected ArrayList<ShortcutPojo> doInBackground(Void... arg0) {
        if(context.get() == null) {
            return new ArrayList<>();
        }

        List<ShortcutRecord> records = DBHelper.getShortcuts(context.get());
        ArrayList<ShortcutPojo> pojos = new ArrayList<>(records.size());

        for (ShortcutRecord shortcutRecord : records) {
            String id = ShortcutUtil.generateShortcutId(shortcutRecord.name);

            ShortcutPojo pojo = new ShortcutPojo(id, shortcutRecord.packageName, shortcutRecord.intentUri);

            pojo.setName(shortcutRecord.name);
            pojo.setTags(tagsHandler.getTags(pojo.id));

            pojos.add(pojo);
        }

        return pojos;
    }

}
