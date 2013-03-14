package lemur.nopol;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import lemur.nopol.ResponseIterator.WarcEntry;
import lemur.nopol.io.TarWriter;

/**
 * Command line tool for creating .tar.gz files from WARC files.
 * 
 * It simply adds one entry to the tar for each WARC response record. The
 * content is the body of the record (without WARC and HTML headers). The
 * entries are named ${WARC-TREC-ID}.html, where ${WARC-TREC-ID} is the value of
 * that header.
 * 
 */
public class ProcessWarc {

    /**
     * Replace HTTP headers by whitespace.
     */
    public static boolean HEADERS_AS_WS = true;

    public static void processFile(File inFile, File outFile) throws IOException {
        int n = -1;
        int nDocs = 0;
        int nErrors = 0;

        long tStart = System.currentTimeMillis();

        TarWriter writer = new TarWriter(outFile);

        FileInputStream input = new FileInputStream(inFile);
        ResponseIterator entries = new ResponseIterator(input);

        while (entries.hasNext()) {
            try {
                // Create a TAR record for each entry in the WARC file.
                WarcEntry entry = entries.next();

                ByteArrayOutputStream bos = new ByteArrayOutputStream(
                        entry.httpHeader.length + entry.content.length);

                // Output HTTP headers or whitespace
                if (HEADERS_AS_WS) {
                    byte[] headerWS = new byte[entry.httpHeader.length];
                    Arrays.fill(headerWS, (byte) ' ');
                    // Add a new line character to the end to simplify debugging
                    headerWS[headerWS.length - 1] = (byte) '\n';
                    bos.write(headerWS);
                } else {
                    bos.write(entry.httpHeader);
                }

                bos.write(entry.content);
                bos.close();
                byte[] processed = bos.toByteArray();

                writer.writeRecord(entry.trecId, processed);
                nDocs++;
            } catch (Exception e) {
                nErrors++;
                System.err.printf("Error processing record: %d\n", n);
                e.printStackTrace();
            }
        }
        entries.close();
        writer.close();
        System.err.printf("%s records: %d errors: %d\n", outFile.getAbsolutePath(),
                nDocs, nErrors);
        System.out.printf("%s %s\n", outFile.getAbsolutePath(), nDocs);

        double tTotal = (System.currentTimeMillis() - tStart) / 1000.0;
        tTotal += 1e-10;
        double recSec = nDocs / tTotal;

        System.err.printf("%.4f seconds. %.4f records/sec\n", tTotal, recSec);

    }

    /**
     * Processes all the .warc.gz files in a directory.
     * 
     * @param inputDir
     * @param outDir
     * @throws IOException
     */
    public static void processDir(File inputDir, File outDir) throws IOException {
        File[] inputFiles = inputDir.listFiles();

        for (File inputFile : inputFiles) {
            String outFname = inputFile.getName().replaceAll("\\.warc.gz", ".tar.gz");
            File outFile = new File(outDir, outFname);
            System.err.printf("Input file: %s\nOutput file: %s\n",
                    inputFile.getAbsolutePath(), outFile.getAbsolutePath());
            processFile(inputFile, outFile);
        }
    }

    public static void usageAndExit() {
        System.err.println("Usage: file|dir input output");
        System.exit(1);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            usageAndExit();
        }

        String type = args[0];
        File inputArg = new File(args[1]);
        File outArg = new File(args[2]);

        if (type.equals("file")) {
            processFile(inputArg, outArg);
        } else if (type.equals("dir")) {
            processDir(inputArg, outArg);
        } else {
            usageAndExit();
        }
    }
}
