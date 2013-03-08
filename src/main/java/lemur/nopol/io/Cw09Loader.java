package lemur.nopol.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.input.CountingInputStream;

import lemur.io.warc018.WarcHTMLResponseRecord;
import lemur.io.warc018.WarcResponseIterator;
import lemur.nopol.WarcLoader;
import lemur.nopol.util.AbstractIterator;

/**
 * Loads responses from the Warc files from Clueweb09.
 */
public class Cw09Loader extends WarcLoader {

    @Override
    public Iterator<WarcEntry> load(DataInputStream input) {
        final CountingInputStream in = new CountingInputStream(input);
        final WarcResponseIterator responses = new WarcResponseIterator(input);
        return new AbstractIterator<WarcLoader.WarcEntry>() {
            @Override
            protected WarcEntry computeNext() {

                while (responses.hasNext()) {
                    WarcHTMLResponseRecord response = responses.next();

                    long recordOffset = in.getByteCount();
                    String trecId = response.getTargetTrecID();
                    byte[] content = response.getRawRecord().getContent();
                    new WarcEntry(trecId, content, recordOffset);
                }
                try {
                    in.close();
                } catch (IOException e) {
                    System.err.printf("Error closing input: %s", e);
                }
                return endOfData();
            }
        };
    }

    @Override
    public int errors() {
        return 0;
    }

}
