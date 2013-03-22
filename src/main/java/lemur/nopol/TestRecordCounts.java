package lemur.nopol;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import lemur.nopol.util.LineIterator;
import lemur.nopol.util.TarArchiveIterator;

/**
 * Tests that the number of entries in the .tar.gz files is the same as the
 * given in the list of record counts.
 * 
 * The program reads the record counts from files in the following formats:
 * 
 * <pre>
 * Disk1/ClueWeb09_English_1/en0000/18.warc.gz  33914
 * Disk1/ClueWeb09_English_1/en0000/19.warc.gz  36403
 * </pre>
 *
 * or
 * <pre>
 * Disk1/ClueWeb12_04/0400tw/0400tw-12.warc.gz     22394
 * Disk1/ClueWeb12_04/0400tw/0400tw-13.warc.gz     23697
 * </pre>
 *
 * And compares the number of entries on the respective tar.gz file with the
 * expected count on the file. The name .tar.gz file is found by replacing the
 * '.warc.gz' extension by '.tar.gz'. The tar.gz are opened with respect to 
 * a base directory given as an argument.   
 *
 */
public class TestRecordCounts {

    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2){
            System.out.println("Usage: TestRecordCounts base-dir record-counts.txt");
            System.exit(1);
        }

        File baseDir = new File(args[0]);
        File recCounts = new File(args[1]);

        int errors = 0;
        
        Iterator<String> lines = new LineIterator(recCounts);
        while (lines.hasNext()){
            String line = lines.next();
            String cols[] = line.split("\\W");
            if (cols.length != 2){
                System.err.printf("Ignoring line: '%s'\n", line);
            }
            String warcName = cols[0];
            int records = Integer.parseInt(cols[1]);
            String tarName = warcName.replaceAll("\\.warc.gz", ".tar.gz");
            File tarFile = new File(baseDir, tarName);
            if (tarFile.isFile()){
                System.out.printf("%s tar not found\n", warcName);
                errors += 1;
            }
            int tarEntries = countTarEntries(tarFile);
            if (records != tarEntries){
                System.out.printf("%s expected %d found %d\n", warcName, records, tarEntries);
                errors += 1;
            }
        }
        
        System.err.printf("%d errors\n", errors);
        int status = (errors > 0) ? 1 : 0;
        System.exit(status);
    }

    private static int countTarEntries(File tarFile) throws IOException {
        TarArchiveIterator tarIter = TarArchiveIterator.openCompressed(tarFile);
        int n = 0;
        while (tarIter.hasNext()){
            n += 1;
        }
        return n;
    }

}
