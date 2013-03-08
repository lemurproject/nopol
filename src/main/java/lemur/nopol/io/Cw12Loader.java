package lemur.nopol.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Iterator;

import lemur.nopol.WarcLoader;
import lemur.nopol.util.AbstractIterator;

import org.apache.commons.io.IOUtils;
import org.jwat.common.HeaderLine;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

/**
 * Loads entries from a WARC file from Clueweb12 using the appropriate parser.
 *
 */
public class Cw12Loader extends WarcLoader {

    int errors = 0;
    
    @Override
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
    public Iterator<WarcEntry> load(DataInputStream input) throws IOException {

        final WarcReader reader = WarcReaderFactory.getReader(input);
        final Iterator<WarcRecord> iter = reader.iterator();
        return new AbstractIterator<WarcEntry>() {
            @Override
            protected WarcEntry computeNext() {
                while (iter.hasNext()) {
                    WarcRecord record = iter.next();
                    // Skip records that are not responses
                    if (!isResponse(record))
                        continue;

                    HeaderLine idHeader = record.getHeader("WARC-TREC-ID");
                    if (idHeader == null){
                        errors += 1;
                        continue;
                    }
                    String trecId = idHeader.value;

                    long offset = (long)record.header.headerBytes.length;
                    
                    String foo = new String(record.header.headerBytes);
                    
                    System.err.printf("\n'%s'\n", foo);
                    System.err.println();
                    
                    
                    
                    try {
                        byte[] content = IOUtils.toByteArray(record.getPayloadContent());
                        return new WarcEntry(trecId, content, offset);

                    } catch (IOException e) {
                        System.err.printf("Error reading record: %s", e);
                        errors += 1;
                        return endOfData();
                    }
                }
                return endOfData();
            }

        };
    }

}
