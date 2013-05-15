package lemur.cw.ann.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lemur.cw.ann.Annotation;
import lemur.cw.ann.util.AbstractIterator;
import lemur.cw.ann.util.LineIterator;

/**
 * Iterates over a Warc file and its annotations, returning an 
 * 'EntryAnnotations' object for every entry in the Warc file that has annotations. 
 * <p>
 * <strong>Important</strong>: This class assumes that both the entries in the 
 * Warc file and the annotations are sorted.
 * </p>
 */
public class EntryAnnotationsIterator extends AbstractIterator<EntryAnnotations> {

    private final ResponseIterator warcIter;
    private final LineIterator annFileIter;
    private final Iterator<Annotation> annIter;

    /** Current element of warcIter */
    WarcEntry entry = null;
    
    /** Current element of annIter */
    Annotation ann = null;
    
    public EntryAnnotationsIterator(File warcFile, File annFile) throws IOException{
        FileInputStream warcIs = new FileInputStream(warcFile);
        this.warcIter = new ResponseIterator(warcIs);

        this.annFileIter = LineIterator.load(annFile);
        this.annIter = Annotation.iterator(annFileIter);
    }
    
    @Override
    protected EntryAnnotations computeNext() {
        
        while (warcIter.hasNext() && annIter.hasNext()) {
            if (ann == null)
                ann = annIter.next();

            if (entry == null)
                entry = warcIter.next();

            int annCmpEntry = ann.trecId.compareTo(entry.trecId);

            // Both iterators point to the same document: 
            // - Collect the annotations and return the element 
            if (annCmpEntry == 0) {
                EntryAnnotations entryAnns = collectAnnotations();
                // Forward warcIter to the next element 
                if (warcIter.hasNext())
                    entry = warcIter.next();
                
                return entryAnns;
            } else if (annCmpEntry < 0) {
                if (annIter.hasNext()) {
                    ann = annIter.next();
                }
            } else {
                if (warcIter.hasNext()) {
                    entry = warcIter.next();
                }
            }
        }

        // Collect the annotations for the last entry
        if (entry != null && ann != null){
            EntryAnnotations entryAnns = collectAnnotations();
            entry = null;
            ann = null;
            return entryAnns;
        }
        
        try {
            warcIter.close();
            annFileIter.close();
        } catch (IOException e) {
            // Fail silently
        }
        return endOfData();
    }

    /**
     * Collects the annotations pointed by the current 'heads' of the iterators
     * 
     * @param entry
     * @param ann
     * @return
     */
    private EntryAnnotations collectAnnotations() {
        List<Annotation> annotations = new ArrayList<Annotation>();
        do {
            if (!ann.trecId.equals(entry.trecId)) {
                break;
            }
            annotations.add(ann);
        } while (annIter.hasNext() && ((ann = annIter.next()) != null));
        return new EntryAnnotations(entry, annotations);
    }
    
}
