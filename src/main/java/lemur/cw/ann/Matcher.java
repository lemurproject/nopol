package lemur.cw.ann;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import net.htmlparser.jericho.CharacterReference;

import lemur.cw.ann.io.EntryAnnotations;
import lemur.cw.ann.io.WarcEntry;
import lemur.cw.ann.util.AsciiFolder;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

public class Matcher {

    /**
     * Regular expression that matches multiple, consecutive whitespace
     */
    static final Pattern expWs = Pattern.compile("\\s+");

    static final Pattern expHtml = Pattern.compile("<[^>]+>");
    
    static final Pattern expPunct = Pattern.compile("[\\.,;:]+");
    
    static final Pattern expNonAlpha = Pattern.compile("\\W+");
    
    //static final Pattern expNonAlpha = Pattern.compile("[^\\w ]+");

    /**
     * Represents the comparison between the text of an annotation with the
     * document.
     * 
     */
    public static class Match {
        
        final Annotation annotation;
        
        /** Text found in the document */
        final String doc;

        /** Text described in the annotations */
        final String ann;

        /** Indicates whether both texts match */
        final boolean match;

        // Shortcut for handling match errors (i.e. no text)
        public static final Match No = new Match(null, null, null, false);

        public Match(Annotation annotation, String doc, String ann, boolean match) {
            this.annotation = annotation;
            this.doc = doc;
            this.ann = ann;
            this.match = match;
        }
        
        public String toString(){
            return String.format("Match[doc='%s' ann='%s']", doc, ann);
        }
        
    }

    /**
     * Cleans a string so that 
     * @param input
     * @return
     */
    public static String cleanText(String input){
        String output = input.toLowerCase();
        output = CharacterReference.decode(output);
        output = expHtml.matcher(output).replaceAll("");
        output = expPunct.matcher(output).replaceAll("");
        output = expWs.matcher(output).replaceAll(" ");
        output = AsciiFolder.foldToAscii(output);
        output = expNonAlpha.matcher(output).replaceAll("");
        return output;
    }
    
    /**
     * Tests if an annotation matches the text of a document using a given
     * encoding.
     * 
     * @param bytesUtf8
     *            A byte sequence of characters encoded as UTF-8
     * @param ann
     * @param encoding
     * @return
     * @throws IOException
     */
    static Match matchText(byte[] bytesUtf8, Annotation ann) throws IOException {
        if (ann.end > bytesUtf8.length) {
            return Match.No;
        }

        byte[] matched = Arrays.copyOfRange(bytesUtf8, ann.start, ann.end);
        String textDoc = new String(matched, "UTF-8");
        String textDocCleaned = cleanText(textDoc);
        String annCleaned = cleanText(ann.text);

        boolean match = textDocCleaned.equals(annCleaned);
        return new Match(ann, textDocCleaned, annCleaned, match);
    }

    public static byte[] encodeDocument(WarcEntry entry, String encoding)
            throws IOException {
        String encoded = new String(entry.content(), encoding);
        return encoded.getBytes("UTF-8");
    }

    /**
     * Detects the character encoding of a document.
     * 
     * @param entry
     * @return
     * @throws IOException 
     */
    static CharsetMatch detectEncoding(WarcEntry entry) throws IOException {
        CharsetDetector detector = new CharsetDetector();
        detector.enableInputFilter(true);
        detector.setText(entry.content());

        return detector.detect();
    }

    /**
     * Obtains the list of errors that occurred while comparing the annotations
     * with the documents in the WARC file. 
     * 
     * @param entryAnns
     * @param encoding
     * @return
     * @throws IOException
     */
    public static List<Match> detectErrors(EntryAnnotations entryAnns, String encoding) throws IOException{
        byte[] bytesUtf8 = encodeDocument(entryAnns.warcEntry, encoding);
        List<Match> errors = new ArrayList<Match>();
        for(Annotation ann: entryAnns.annotations){
            Match match = matchText(bytesUtf8, ann);
            if (!match.match){
                errors.add(match);
            }
        }
        return errors;
    }
}
