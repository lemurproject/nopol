package lemur.nopol;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import lemur.nopol.util.TarArchiveIterator;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.jwat.common.Payload;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

/**
 * Verifies that the created processed files (.tar.gz) are correct.
 * 
 * It compares the .tar.gz with the original .warc.gz in terms of number of
 * entries and the length (in bytes) of each one.
 * 
 * If detects differences, it prints them to the standard output and returns
 * an exit status 1.
 * 
 */
public class TestProcessed {

    public static String[] validateFile(File warcFile, File tarFile) throws IOException {

        if (!tarFile.isFile()) {
            return new String[] { String.format("%s\tFile does not exist: %s\n",
                    warcFile.getName(), tarFile.getName()) };
        }

        TarArchiveIterator tarIter = TarArchiveIterator.openCompressed(tarFile);

        GZIPInputStream warcGzIs = new GZIPInputStream(new FileInputStream(warcFile));
        WarcReader reader = WarcReaderFactory.getReader(warcGzIs);
        Iterator<WarcRecord> warcIter = reader.iterator();

        List<String> errors = new ArrayList<String>();

        int n = 0;
        while (warcIter.hasNext()) {
            WarcRecord record = warcIter.next();
            if (!ResponseIterator.isResponse(record)) {
                continue;
            }

            if (!tarIter.hasNext()) {
                errors.add(String.format("Record %d missing in tar file", n));
                break;
            }

            ArchiveEntry tarEntry = tarIter.next();

            try {
                // Total record size: http header +  content
                long recSize = record.getPayload().getTotalLength();

                long entrySize = tarEntry.getSize();
                if (recSize != entrySize) {
                    errors.add(String.format("Record %d. Sizes do not match. warc=%d tar=%d", n, recSize, entrySize));
                }

            } catch (Exception e){
                errors.add(String.format("Error processing record %d: %s. payload=%d",
                        n, e.getMessage(), record.getPayload()));
            }
            n += 1;
           
        }

        if (tarIter.hasNext()){
            errors.add("Tar file has additional entries");
        }

        reader.close();
        warcGzIs.close();
        return errors.toArray(new String[] {});
    }

    
    public static int testFile(File warcFile, File tarFile) throws IOException {
        String[] errors = validateFile(warcFile, tarFile);
        for(String e: errors){
            System.out.printf("%s\t%s\n", warcFile.getName(), e);
        }
        return errors.length;
    }
    
    /**
     * Tests all the .warc.gz files in warcDir with the .tar.gz in tarDir.
     * 
     * @param warcDir
     * @param tarDir
     * @return 
     * @throws IOException
     */
    public static int testDir(File warcDir, File tarDir) throws IOException {
        String[] warcNames = warcDir.list(new SuffixFileFilter(".warc.gz"));

        int errors = 0;
        
        for (String warcName : warcNames) {
            String tarName = warcName.replaceAll("\\.warc.gz", ".tar.gz");
            File tarFile = new File(tarDir, tarName);
            File warcFile = new File(warcDir, warcName);
            errors += testFile(warcFile, tarFile);
        }
        
        return errors;
    }

    public static void usageAndExit() {
        System.err.println("Usage: file|dir warc tar");
        System.err.println();
        System.err
                .println("  warc: Refererence .warc.gz file or directory containing them.");
        System.err.println("  tar:  Created .tar.gz file or directory containing them.");
        System.exit(1);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            usageAndExit();
        }

        String type = args[0];
        File warcArg = new File(args[1]);
        File tarArg = new File(args[2]);

        int errors = 0;
        if (type.equals("file") && warcArg.isFile()) {
            errors = testFile(warcArg, tarArg);
        } else if (type.equals("dir") && warcArg.isDirectory() && tarArg.isDirectory()) {
            errors = testDir(warcArg, tarArg);
        } else {
            usageAndExit();
        }
        System.err.printf("%d errors\n", errors);
    }

}
