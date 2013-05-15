package lemur.cw.ann;

import java.io.File;
import java.io.IOException;
import java.util.List;

import lemur.cw.ann.Matcher.Match;
import lemur.cw.ann.io.EntryAnnotations;
import lemur.cw.ann.io.EntryAnnotationsIterator;

import com.ibm.icu.text.CharsetMatch;

/**
 * Detects and fixes the offset errors in the annotations of a Warc file.
 * 
 * It loads the annotations and tries to match them with the original text of
 * the records. If it detects an error (that can be fixed), it modifies the
 * offsets of the annotations and writes into the standard output the correct
 * offsets. If the annotations are correct (i.e. the offsets match), it writes
 * them as they are. Otherwise, if the errors can not be repaired, it prints a
 * message in the standard error.
 * 
 * TODO: Things that this class should do:
 * 
 * - Fix the offsets when the records do no match (because of the header bug)
 * 
 */
public class DetectEncoding {

    private static boolean reportErrors = false;
    
    /**
     * Common encodings to try before detecting the encoding using the content 
     */
    public static final String[] encodings = new String[] { "UTF-8", "ISO-8859-1", "windows-1252" };

    public static String matchingEncoding(EntryAnnotations entryAnns) throws IOException {
        List<Match> errors;
        // Try first with the most common encodings
        for (String encoding : encodings) {
            errors = Matcher.detectErrors(entryAnns, encoding);
            if (errors.size() == 0) {
                return encoding;
            }
        }

        // Detect the encoding based on the content
        CharsetMatch detected = Matcher.detectEncoding(entryAnns.warcEntry);
        errors = Matcher.detectErrors(entryAnns, detected.getName());
        if (errors.size() == 0) {
            return detected.getName();
        }

        if (reportErrors ){
            for (Match err: errors){
                System.err.printf("%s\t%s\t%d\t%d\t'%s'\t'%s'\n", entryAnns.warcEntry.trecId,
                        detected.getName(),
                        err.annotation.start, err.annotation.end,  
                        err.doc, err.ann);
            }
        }

        // No match found
        return null;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: FixOffsetErrors file.warc.gz annotations.tsv[.gz] [debug]");
            System.err.println(
                "Detects the encoding of each document in the .warc.gz file that matches the annotations." +
                "\n" +
                "\n" +
                "Use 'true' as the last argument to print the annotations that can't be matched to the stderr");
            System.exit(1);
        }
        File warcFile = new File(args[0]);
        File annFile = new File(args[1]);

        if (args.length == 3){
            reportErrors = args[2].equalsIgnoreCase("true");
        }

        EntryAnnotationsIterator entryAnnsIter = new EntryAnnotationsIterator(warcFile,
                annFile);

        while (entryAnnsIter.hasNext()) {
            EntryAnnotations entryAnns = entryAnnsIter.next();
            // Detecting the encoding and output only documents
            String encoding = matchingEncoding(entryAnns);
            if (encoding == null){
                continue;
            }

            for (Annotation ann: entryAnns.annotations){
                System.out.printf("%s\t%s\t%s\t%d\t%d\t%8f\t%s\n", entryAnns.warcEntry.trecId, encoding,
                        ann.text, ann.start, ann.end, ann.score, ann.mId);
            }
        }
    }
}
