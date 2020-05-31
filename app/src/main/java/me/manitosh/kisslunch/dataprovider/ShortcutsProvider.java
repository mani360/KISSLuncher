package me.manitosh.kisslunch.dataprovider;

import android.widget.Toast;

import me.manitosh.kisslunch.R;
import me.manitosh.kisslunch.loader.LoadShortcutsPojos;
import me.manitosh.kisslunch.normalizer.StringNormalizer;
import me.manitosh.kisslunch.pojo.Pojo;
import me.manitosh.kisslunch.pojo.ShortcutPojo;
import me.manitosh.kisslunch.searcher.Searcher;
import me.manitosh.kisslunch.utils.FuzzyScore;

public class ShortcutsProvider extends Provider<ShortcutPojo> {
    private static boolean notifiedKissNotDefaultLauncher = false;

    @Override
    public void reload() {
        super.reload();
        // If the user tries to add a new shortcut, but KISS isn't the default launcher
        // AND the services are not running (low memory), then we won't be able to
        // spawn a new service on Android 8.1+.

        try {
            this.initialize(new LoadShortcutsPojos(this));
        }
        catch(IllegalStateException e) {
            if(!notifiedKissNotDefaultLauncher) {
                // Only display this message once per process
                Toast.makeText(this, R.string.unable_to_initialize_shortcuts, Toast.LENGTH_LONG).show();
            }
            notifiedKissNotDefaultLauncher = true;
            e.printStackTrace();
        }
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        FuzzyScore.MatchInfo matchInfo;
        boolean match;

        for (ShortcutPojo pojo : pojos) {
            matchInfo = fuzzyScore.match(pojo.normalizedName.codePoints);
            match = matchInfo.match;
            pojo.relevance = matchInfo.score;

            // check relevance for tags
            if (pojo.getNormalizedTags() != null) {
                matchInfo = fuzzyScore.match(pojo.getNormalizedTags().codePoints);
                if (matchInfo.match && (!match || matchInfo.score > pojo.relevance)) {
                    match = true;
                    pojo.relevance = matchInfo.score;
                }
            }

            if (match && !searcher.addResult(pojo)) {
                return;
            }
        }
    }

    public Pojo findByName(String name) {
        for (Pojo pojo : pojos) {
            if (pojo.getName().equals(name))
                return pojo;
        }
        return null;
    }
}
