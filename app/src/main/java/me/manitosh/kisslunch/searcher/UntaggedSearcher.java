package me.manitosh.kisslunch.searcher;

import java.util.Iterator;
import java.util.List;

import me.manitosh.kisslunch.KissApplication;
import me.manitosh.kisslunch.MainActivity;
import me.manitosh.kisslunch.pojo.AppPojo;
import me.manitosh.kisslunch.pojo.Pojo;
import me.manitosh.kisslunch.pojo.PojoWithTags;

public class UntaggedSearcher extends Searcher {

    public UntaggedSearcher(MainActivity activity )
    {
        super( activity, "<untagged>" );
    }

    @Override
    protected Void doInBackground(Void... voids) {
        MainActivity activity = activityWeakReference.get();
        if ( activity == null )
            return null;
        List<AppPojo> results = KissApplication.getApplication(activity).getDataHandler().getApplicationsWithoutExcluded();
        if (results == null)
            return null;
        for(Iterator<AppPojo> iterator = results.iterator(); iterator.hasNext(); ) {
            Pojo pojo = iterator.next();
            if (!(pojo instanceof PojoWithTags)) {
                iterator.remove();
                continue;
            }
            PojoWithTags pojoWithTags = (PojoWithTags) pojo;
            if (pojoWithTags.getTags() == null || pojoWithTags.getTags().isEmpty()) {
                continue;
            }
            iterator.remove();
        }
        this.addResult(results.toArray(new Pojo[0]));
        return null;
    }
}
