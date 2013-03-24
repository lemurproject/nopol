package lemur.nopol;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import lemur.nopol.ResponseIterator.WarcEntry;
import lemur.nopol.util.AbstractIterator;

import org.apache.commons.io.IOUtils;
import org.jwat.common.HeaderLine;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

/**
 * An iterator over a WARC file that returns WarcEntry objects.
 * 
 * This iterator discards records that are not responses or that do not have 
 * the WARC-TREC-ID header.
 * 
 */
public class ResponseIterator extends AbstractIterator<WarcEntry> {

    /**
     * Name of the header with the Trec identifier.
     */
    final public static String TREC_ID = "WARC-TREC-ID";
    
    /**
     * (Oh, Java, why you don't have tuples?) 
     */
    public static class WarcEntry {
        public final byte[] content;
        public final String trecId;
        public final int contentOffset;
        public final String contentType;
        public final byte[] httpHeader;

        public WarcEntry(String trecId, byte[] content, byte[] httpHeader, int contentOffset, String contentType) {
            this.trecId = trecId;
            this.content = content;
            this.httpHeader = httpHeader;
            this.contentOffset = contentOffset;
            this.contentType = contentType;
        }
    }
    
    private final InputStream input;
    final WarcReader reader;
    final Iterator<WarcRecord> iter;
    private int errors = 0;
    
    /** Record offset */
    private int n = 0;
    
    public ResponseIterator(FileInputStream input) throws IOException {
        this.input = input;
        this.reader = WarcReaderFactory.getReader(new GZIPInputStream(input));
        this.iter = reader.iterator();
    }

    public int errors() {
        return errors;
    }

    public static boolean isResponse(WarcRecord record) {
        HeaderLine typeHeader = record.getHeader("WARC-Type");
        if (typeHeader != null) {
            return typeHeader.value.equals("response");
        }
        return false;
    }

    @Override
    protected WarcEntry computeNext() {
        while (iter.hasNext()) {
            WarcRecord record = iter.next();
            n++;
            // Skip records that are not responses
            if (!isResponse(record)){
                continue;
            }

            HeaderLine idHeader = record.getHeader(TREC_ID);
            if (idHeader == null) {
                errors += 1;
                continue;
            }
            String trecId = idHeader.value;
            int offset = record.header.headerBytes.length;

            String contentType = (record.getHttpHeader() != null) ? record.getHttpHeader().contentType : null;
            
            try {
                byte[] httpHeader = record.getHttpHeader().getHeader();
                byte[] content = IOUtils.toByteArray(record.getPayloadContent());
                return new WarcEntry(trecId, content, httpHeader, offset, contentType);
            } catch (Exception e) {
                System.err.printf("Error reading record %d: %s", n, e);
                errors += 1;
            }
        }
        try {
            reader.close();
            input.close();
        } catch (Exception e){
            // Ignore
        }
        return endOfData();
    }
    
    public void close(){
        this.reader.close();
    }

}
