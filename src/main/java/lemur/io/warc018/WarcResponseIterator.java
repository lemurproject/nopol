package lemur.io.warc018;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import lemur.cw.ann.util.AbstractIterator;

/**
 * An iterator over the <em>response</em> records of a WARC file.
 * 
 */
public class WarcResponseIterator extends AbstractIterator<WarcHTMLResponseRecord> {

    DataInputStream input;

    public WarcResponseIterator(DataInputStream input) {
        this.input = input;
    }

    public WarcResponseIterator(InputStream input) {
        this(new DataInputStream(input));
    }
    
    private static DataInputStream loadFile(File inFile, boolean compressed)
            throws IOException {
        InputStream inStream = new FileInputStream(inFile);
        if (compressed) {
            inStream = new GZIPInputStream(inStream);
        }
        return new DataInputStream(inStream);
    }

    public static WarcResponseIterator loadResponses(String fileName) throws IOException {
        File inFile = new File(fileName);
        boolean compressed = fileName.endsWith(".gz");
        DataInputStream input = loadFile(inFile, compressed);
        return new WarcResponseIterator(input);
    }

    @Override
    protected WarcHTMLResponseRecord computeNext() {
        WarcRecord record;
        try {
            while ((record = WarcRecord.readNextWarcRecord(input)) != null) {
                WarcHTMLResponseRecord response = new WarcHTMLResponseRecord(
                        record);
                if (!response.isValid()) {
                    System.err.printf("Not valid: %s\n", response);
                    continue;
                }
                return response;
            }
            input.close();
        } catch (IOException e) {
            System.err.println(":-");
            return this.endOfData();
        }
        return this.endOfData();
    }
}
