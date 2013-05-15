package lemur.cw.ann.io;

import java.util.List;

import lemur.cw.ann.Annotation;

/**
 * Represents a pair of an entry from a Warc file and its list of annotations.
 */
public class EntryAnnotations {

    public final WarcEntry warcEntry;
    public final Iterable<Annotation> annotations;
    public final int count;

    public EntryAnnotations(WarcEntry warcEntry, List<Annotation> annotations) {
        this.warcEntry = warcEntry;
        this.annotations = annotations;
        this.count = annotations.size();
    }

}
