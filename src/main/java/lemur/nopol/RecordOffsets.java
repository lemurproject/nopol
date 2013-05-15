package lemur.nopol;

import java.io.FileInputStream;
import java.io.IOException;

import lemur.nopol.ResponseIterator.WarcEntry;

/**
 * Prints the record offsets for a WARC file. 
 *
 */
public class RecordOffsets {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: RecordOffsets filename");
            System.exit(1);
        }

        String warcFile = args[0];

        FileInputStream input = new FileInputStream(warcFile);
        ResponseIterator entries = new ResponseIterator(input);

        int nRecords = 0;
        while (entries.hasNext()) {
            WarcEntry entry = entries.next();
            System.out.printf("%s\t%s\n", entry.trecId, entry.contentOffset);
            nRecords++;
        }
        System.err.printf("Finished. %d records\n", nRecords);
    }
}
