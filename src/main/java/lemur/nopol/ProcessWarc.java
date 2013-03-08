package lemur.nopol;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import lemur.nopol.WarcLoader.WarcEntry;
import lemur.nopol.io.Cw09Loader;
import lemur.nopol.io.Cw12Loader;
import lemur.nopol.io.TarWriter;

/**
 * 
 * 
 *
 */
public class ProcessWarc {

    public static final byte Space = (byte)' ';

    /**
     * Process the content from a WarcEntry. 
     * 
     * It simply replaces the HTTP headers with spaces.
     * 
     * @param content
     * @return
     */
    public static byte[] processContent(byte[] content) {

        // Find when the body of the document starts.
        int bodyStart = 0;
        for (int i=0; i < content.length - 1; i++){
            if (content[i] == '\n' && content[i+1] == '\n'){
                bodyStart = i+2;
                break;
            }
        }
        Arrays.fill(content, 0, bodyStart, Space);
        return content;
    }
    
    public static void processFile(WarcLoader loader, File inFile, File outFile) throws IOException {
        int n = -1;
        int nDocs = 0;
        int nErrors = 0;

        long tStart = System.currentTimeMillis();

        TarWriter writer = new TarWriter(outFile);
        Iterator<WarcEntry> entries = loader.loadFile(inFile);
        
        while (entries.hasNext()){
            try {
                WarcEntry entry = entries.next();
                //byte[] processed = processContent(entry.content);
                byte[] processed = entry.content;
                
                writer.writeRecord(entry.trecId, processed);
                nDocs++;
            } catch (Exception e) {
                nErrors++;
                System.err.printf("Error processing record: %d\n", n);
                e.printStackTrace();
            }
        }
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
     * @param loader
     * @param inputDir
     * @param outDir
     * @throws IOException
     */
    public static void processDir(WarcLoader loader, File inputDir, File outDir) throws IOException {
        File[] inputFiles = inputDir.listFiles();

        for (File inputFile : inputFiles) {
            String outFname = inputFile.getName().replaceAll("\\.warc.gz", ".tar.gz");
            File outFile = new File(outDir, outFname);
            System.err.printf("Input file: %s\nOutput file: %s\n",
                    inputFile.getAbsolutePath(), outFile.getAbsolutePath());
            processFile(loader, inputFile, outFile);
        }
    }

    public static void usageAndExit() {
        System.err.println("Usage: cw09|cw12 file|dir input output");
        System.exit(1);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            usageAndExit();
        }
        String format = args[0];
        String type = args[1];
        File inputArg = new File(args[2]);
        File outArg = new File(args[3]);
        
        WarcLoader loader = null;
        if (format.equalsIgnoreCase("cw09")){
            loader = new Cw09Loader();
        } else if (format.equalsIgnoreCase("cw12")){
            loader = new Cw12Loader();
        } else {
            usageAndExit();
        }
        
        if (type.equals("file")) {
            processFile(loader, inputArg, outArg);
        } else if (type.equals("dir")) {
            processDir(loader, inputArg, outArg);
        } else {
            usageAndExit();
        }
    }
}
