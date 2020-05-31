package me.manitosh.kisslunch.dataprovider.simpleprovider;

import java.util.Locale;

import me.manitosh.kisslunch.pojo.Pojo;
import me.manitosh.kisslunch.pojo.TagDummyPojo;
import me.manitosh.kisslunch.searcher.Searcher;

public class TagsProvider extends SimpleProvider {
    public static final String SCHEME = "kisstag://";

    public static String generateUniqueId(String tag) {
        return SCHEME + tag.toLowerCase(Locale.ROOT);
    }

    @Override
    public void requestResults(String s, Searcher searcher) {

    }

    @Override
    public boolean mayFindById(String id) {
        return id.startsWith(SCHEME);
    }

    @Override
    public Pojo findById(String id) {
        return new TagDummyPojo(id);
    }
}
