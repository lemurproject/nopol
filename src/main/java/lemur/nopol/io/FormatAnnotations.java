package lemur.nopol.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lemur.cw.ann.util.LineIterator;

/**
 * Converts the annotations sent by Google into the format used by ClueWeb.
 */
public class FormatAnnotations {

    public static int formatLines(Iterator<String> lines) {
        int nErrors = 0;

        String prevHeader = null;
        while (lines.hasNext()) {
            String line = lines.next().trim();
            if (line.length() == 0) {
                prevHeader = null;
            }
            String[] cols = line.split("\t");
            if (cols.length == 1) {
                prevHeader = cols[0];
                if (prevHeader.endsWith(".html")) {
                    prevHeader = prevHeader.substring(0, prevHeader.length() - 5);
                }
                continue;
            } else if (cols.length != 5) {
                // Ignoring line
                nErrors += 1;
                System.err.printf("Ignoring line (%d cols): '%s'\n", cols.length, line);
                continue;
            }

            System.out.printf("%s\t%s\t%s\t%s\t%s\t%s\n", prevHeader, cols[0], cols[1],
                    cols[2], cols[3], cols[4]);
        }
        return nErrors;
    }

    public static void main(String[] args) throws IOException {

        List<InputStream> data = new ArrayList<InputStream>();
        if (args.length == 0) {
            System.err.println("Reading data from stdin.");
            data.add(System.in);
        } else {
            for (int i = 0; i < args.length; i++) {
                InputStream inputFile = new FileInputStream(args[i]);
                data.add(inputFile);
            }
        }

        for (InputStream inputStream : data) {
            LineIterator iter = new LineIterator(inputStream);
            int nErrors = formatLines(iter);
            System.err.printf("Stream procesed: %d errors\n", nErrors);
        }
        System.err.println("Finished.");
    }
}
