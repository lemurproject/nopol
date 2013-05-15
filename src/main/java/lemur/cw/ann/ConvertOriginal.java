package lemur.cw.ann;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import lemur.cw.ann.util.LineIterator;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Converts the original annotations into the format used by ClueWeb.
 * <p>
 * This class is a command line utility that reads the files and writes into the
 * standard output the converted annotations.
 * </p>
 * <p>
 * This class supports both formats used for the annotations:
 * <ul>
 * <li>Plain text files using the format described below</li>
 * <li>.tar.gz archives that contains plain text files, using the same format</li>
 * </ul>
 * 
 */
public class ConvertOriginal {

    public static void usage() {
        System.err.println("Usage: tar|text [input1 [input2 ... ]]");
        System.exit(1);
    }

    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            usage();
        }

        String mode = args[0];
        if (!mode.equals("tar") && !mode.equals("text")) {
            usage();
        }

        List<InputStream> data = new ArrayList<InputStream>();
        if (args.length == 1) {
            System.err.println("Reading data from stdin.");
            data.add(System.in);
        } else {
            for (int i = 1; i < args.length; i++) {
                InputStream inputFile = new FileInputStream(args[i]);
                data.add(inputFile);
            }
        }

        int nFile = 1;
        for (InputStream inputStream : data) {
            int nErrors = 0;
            System.err.printf("Processing file %d\n", nFile);
            if (mode.equals("tar")) {
                nErrors = processTar(inputStream);
            } else {
                nErrors = processText(inputStream);
            }
            System.err.printf("File procesed: %d errors\n", nErrors);
            nFile += 1;
        }
        System.err.println("Finished.");
    }

    /**
     * Reads a .tar.gz file and prcess the entries in it a plain text files.
     * 
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static int processTar(InputStream inputStream) throws IOException {
        GZIPInputStream gzipIs = new GZIPInputStream(inputStream);
        TarArchiveInputStream tarIs = new TarArchiveInputStream(gzipIs);

        int nErrors = 0;
        try {
            ArchiveEntry entry = tarIs.getNextEntry();
            while (entry != null) {
                nErrors += processText(tarIs);
                entry = tarIs.getNextEntry();
            }
            tarIs.close();
        } catch (Exception e) {
            System.err.println("Error processing .tar.gz file: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return nErrors;
    }

    public static int processText(InputStream inputStream) {
        LineIterator iter = new LineIterator(inputStream);
        return formatLines(iter);
    }

    /**
     * Process the lines containing the original annotations and converts them
     * into the final output format.
     * 
     * @param lines
     * @return The number of errors detected.
     */
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
}
