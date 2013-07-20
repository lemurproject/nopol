package lemur.cw.ann;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Groups the annotations by the WARC file that included the original pages.
 * <p/>
 * This script takes as input several annotation files (in .tsv format, optionally
 * gzipped), identifies the name of the WARC file that contains the page and, for
 * each one, creates an output file that contains the annotations included in the
 * respective WARC file. This allows us later to process the annotations and the
 * WARC files in parallel.
 * <p/>
 * Usage: GroupAnnotations baseDir {cw09,cw12} outdir ann_file [ann_file ...]
 * <p/>
 * positional arguments:
 * baseDir      Directory containing a set of files that describe the datasets
 *  (CW09 and CW12) and are used to identify the WARC that contains a page.
 * {cw09,cw12}  Dataset name
 * outdir       Output directory
 * ann_file     Annotation files in .tsv[.gz] format
 * <p/>
 */
public class GroupAnnotations {

    private String dataSet;
    private File outDir;
    private Map<String, String> dirLists;
    private Map<String, BufferedWriter> writers;

    public GroupAnnotations(String dataSet, File outDir, Map<String, String> dirLists) {
        this.dataSet = dataSet;
        this.outDir = outDir;
        this.dirLists = dirLists;
        this.writers = new HashMap<String, BufferedWriter>();
    }

    public static BufferedWriter createWriter(File file) throws FileNotFoundException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
    }

    public static String parseAnnotation(String line) throws Exception {
        String[] cols = line.split("\t");
        if (cols.length != 7) {
            throw new Exception(String.format("Error parsing annotation. Columns %d/7", cols.length));
        }

        String[] nameParts = cols[0].split("-");
        return nameParts[1] + "-" + nameParts[2];
    }

    public static void usage() {
        System.err.println("Usage: baseDir [cw09|cw12] outdir input ...");
        System.exit(1);
    }

    /**
     * Create a dictionary that maps directory names to their path in CW.
     * <p/>
     * It reads a list of directories of the dataset and returns a dictionary
     * that has as keys the name of the directory and values the relative
     * path of the directory with respect to the root directory of the dataset.
     * <p/>
     * For example, for CW09 `fname_root` points to a file with the following
     * content:
     * <p/>
     * /bos/tmp8/ClueWeb09
     * <p/>
     * While `fname_dirlists` will contain several lines with the list of
     * directories, as follows:
     * <p/>
     * /bos/tmp8/ClueWeb09/Disk1/ClueWeb09_English_1/en0001
     * ...
     * /bos/tmp8/ClueWeb09/Disk1/ClueWeb09_English_2/en0019
     * <p/>
     * Then, the returned dictionary will contain the following entries:
     * <p/>
     * en0001: Disk1/ClueWeb09_English_1/en0001
     * en0021: Disk1/ClueWeb09_English_2/en0021
     * <p/>
     * This dictionary allows us to find the path in which the output file
     * must be created, given the directory name encoded in the TREC-ID of a
     * record.
     */
    public static Map<String, String> loadDirLists(File fileRoot, File fileDirLists) throws IOException {
        String rootDir = FileUtils.readFileToString(fileRoot).trim();

        int rootPrefix = rootDir.length() + 1;

        Map<String, String> dirlists = new HashMap<String, String>();
        for (String line : FileUtils.readLines(fileDirLists)) {
            line = line.trim();

            String path = line.substring(rootPrefix);
            int fileIndex = path.lastIndexOf('/');
            String key = path.substring(fileIndex);
            dirlists.put(key, path);
        }
        ;
        return dirlists;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            usage();
        }

        int REPORT_EVERY = 10000;

        File baseDir = new File(args[0]);
        String dataset = args[1];
        File outdir = new File(args[2]);
        String[] inputFiles = Arrays.copyOfRange(args, 3, args.length);

        // Load directory lists
        File fileRoot = new File(baseDir, dataset + "-root.txt");
        File fileDirLists = new File(baseDir, dataset + "-dirlist.txt");
        Map<String, String> dirlists = loadDirLists(fileRoot, fileDirLists);

        GroupAnnotations grouper = new GroupAnnotations(dataset, baseDir, dirlists);

        int nFiles = 0;
        int nLines = 0;
        // Read the input files
        for (String fName : inputFiles) {
            nFiles++;
            System.err.printf("Processing file %d/%d %s\n", nFiles, args.length, fName);
            Iterator<String> lines = FileUtils.lineIterator(new File(fName));
            while (lines.hasNext()) {
                String line = lines.next().trim();
                if (line.length() > 0) {
                    String key = parseAnnotation(line);
                    grouper.addAnnotation(key, line);
                    nLines++;
                    if (nLines % REPORT_EVERY == 0) {
                        System.err.printf("%10d lines processed\n", nLines);
                    }
                }
            }
        }
        grouper.close();
    }

    public BufferedWriter createWriter(String key) throws IOException {
        String[] parts = key.split("-");
        String dirName = parts[0];
        String fileNum = parts[1];
        String fileName = null;
        if (this.dataSet.equals("cw09")) {
            fileName = String.format("%s.ann.tsv", fileNum);
        } else if (this.dataSet.equals("cw12")) {
            fileName = String.format("'%s-%s.ann.tsv", dirName, fileNum);
        }

        File warcDir = new File(outDir, dirLists.get(dirName));
        if (!warcDir.isDirectory()) {
            FileUtils.forceMkdir(warcDir);
        }

        File outFile = new File(warcDir, fileName);
        BufferedWriter writer = createWriter(outFile);
        writers.put(key, writer);
        return writer;
    }

    public void addAnnotation(String key, String line) throws IOException {
        BufferedWriter writer = this.writers.get(key);
        if (writer == null) {
            writer = createWriter(key);
        }
        writer.write(line);
        writer.newLine();
    }

    public void close() throws IOException {
        for (BufferedWriter writer : this.writers.values()) {
            writer.close();
        }
    }
}
