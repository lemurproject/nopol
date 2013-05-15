package lemur.nopol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

import lemur.cw.ann.Annotation;
import lemur.cw.ann.util.LineIterator;
import lemur.nopol.ResponseIterator.WarcEntry;
import lemur.nopol.encdet.EncDetUils;
import lemur.nopol.encdet.EncodingDetector;
import lemur.nopol.encdet.StreamEncodingDetector;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

/**
 * Command line utility that tests the annotations made on a WARC file.
 * 
 * It iterates in parallel on the annotations file and the records in the WARC
 * file, for each annotation it tries to match the text of the annotation with
 * the content of the record in the specified offsets. Since the annotations
 * were made with respect to the content encoded as UTF-8, we try three 
 * options for the source encoding:
 * 
 * <ol>
 * <li>Use UTF-8</li>
 * <li>Use the default encoding for HTTP: ISO-8859-1</li>
 * <li>Detect the encoding based on the byte stream.</li>
 * </ol>
 * 
 * If both texts match using the first option (UTF-8), the annotation is 
 * skipped (since it is correct) and no output is generated.
 * 
 * Otherwise, the other two options are tried and the results are written 
 * into the standard output as a line with the following columns separated
 * by tabs:
 *  
 *  <pre>
 *      document-id
 *      annotation-text
 *      annotation-start
 *      annotation-end
 *      match:yes|no
 *      match-default:yes|no
 *      match-detected:yes|no
 *      charset-name
 *      charset-confidence
 * </pre>
 */
public class TestAnnotations {

    /**
     * Pattern to detect multiple whitespace
     */
    static final Pattern expWs = Pattern.compile("\\s+");
    
    
    static final Pattern expNonAlpha = Pattern.compile("\\W+");

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: TestAnnotations file.warc.gz annotations.tsv");
            System.exit(1);
        }
        String warcFile = args[0];
        String annFile = args[1];

        FileInputStream input = new FileInputStream(warcFile);
        ResponseIterator entries = new ResponseIterator(input);

        LineIterator lines = LineIterator.load(annFile);
        Iterator<Annotation> annotations = Annotation.iterator(lines);

        WarcEntry entry = null;
        Annotation ann = null;

        System.out.printf("doc\ttext\tstart\tend\tutf_matches\tutf_text\tiso_matches\tiso_text\tdetected_matches\tdetected_text\tdetected_charset\tdetected_conf\thttp_matches\thttp_text\thttp_enc\n");
        
        while (entries.hasNext() && annotations.hasNext()) {

            if (ann == null)
                ann = annotations.next();

            if (entry == null)
                entry = entries.next();

            int annCmpEntry = ann.trecId.compareTo(entry.trecId);

            if (annCmpEntry == 0) {
                ann = foo(entry, ann, annotations);
                // At the end of iteration we know that ann > response
                if (entries.hasNext())
                    entry = entries.next();

            } else if (annCmpEntry < 0) {
                if (annotations.hasNext()) {
                    ann = annotations.next();
                }
            } else {
                if (entries.hasNext()) {
                    entry = entries.next();
                }
            }
        }

    }

    public static class Match {
        final String doc;
        final String ann;
        final boolean match;
        
        // Shortcut for handling match errors (i.e. no text)
        public static final Match No = new Match(null, null, false);
        
        public Match (String doc, String ann, boolean match) {
            this.doc = doc;
            this.ann = ann;
            this.match = match;
        }
        
        
    }
    
    /**
     * Tests if an annotation matches the text of a document using a given encoding. 
     * @param entry
     * @param ann
     * @param encoding
     * @return
     * @throws IOException
     */
    static Match matchText(WarcEntry entry, Annotation ann, String encoding) throws IOException {
        String encoded = new String(entry.content, encoding);
        byte[] bytesUtf8 = encoded.getBytes("UTF-8");
        
        if (ann.end > bytesUtf8.length) {
            return Match.No;
        }
        
        byte[] matched = Arrays.copyOfRange(bytesUtf8, ann.start, ann.end); 
        String textDoc = new String(matched, "UTF-8");
        String cleaned = expWs.matcher(textDoc).replaceAll(" ");
        cleaned = expNonAlpha.matcher(cleaned).replaceAll("");
        
        String cleanedAnn = expWs.matcher(ann.text).replaceAll(" ");
        cleanedAnn = expNonAlpha.matcher(cleanedAnn).replaceAll("");
        
        return new Match(cleaned, cleanedAnn, cleaned.equalsIgnoreCase(cleanedAnn));
    }
    
    static CharsetMatch detectEncoding(WarcEntry entry){
        CharsetDetector detector = new CharsetDetector();
        detector.enableInputFilter(true);
        detector.setText(entry.content);
        
        return detector.detect();
    }
    
    static String detectHTTPEncoding(WarcEntry entry) throws IOException{
        InputStream data = new ByteArrayInputStream(entry.content);
        String encodingHeader = EncDetUils.encodingFromHttpHeader(entry.contentType);
        
        if (encodingHeader != null && encodingHeader.equalsIgnoreCase("utf-8")){
            return null;
        }
        
        StreamEncodingDetector streamEncDet = new StreamEncodingDetector(data);

        EncodingDetector encDet = new EncodingDetector(streamEncDet, encodingHeader);

        String encContent = encDet.getEncoding();
        return encContent;
    }
    
    private static Annotation foo(WarcEntry entry, Annotation ann,
            Iterator<Annotation> annotations) throws IOException {

        do {
            if (!ann.trecId.equals(entry.trecId)) {
                break;
            }

            Match matchesUTF8 = matchText(entry, ann, "UTF-8");
            
            if (!matchesUTF8.match) {
                Match matchesIso = matchText(entry, ann, "ISO-8859-1");
                
                // Detect the encoding
                CharsetMatch charMatch = detectEncoding(entry);
                
                Match matchesDetected = matchText(entry, ann, charMatch.getName());
                
                Match matchesHttp = Match.No;
                String encHttp = "-";
                
                if (!matchesDetected.match){
                    encHttp = detectHTTPEncoding(entry);
                    matchesHttp = (encHttp != null) ? matchText(entry, ann, encHttp ) : Match.No;
                }

                boolean matchesAny = matchesIso.match | matchesDetected.match | matchesHttp.match;
                
                if (!matchesAny){
                    System.out.printf("%s\t'%s'\t%d\t%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n", 
                            entry.trecId,
                            ann.text, ann.start, ann.end,
                            matchesUTF8.match, matchesUTF8.doc, 
                            matchesIso.match, matchesIso.doc,
                            matchesDetected.match, matchesDetected.doc,
                            charMatch.getName(),
                            charMatch.getConfidence(),
                            matchesHttp.match, matchesHttp.doc,
                            encHttp);
                }
            }
            

        } while (annotations.hasNext() && ((ann = annotations.next()) != null));

        return ann;

    }
}
