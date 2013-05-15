package lemur.cw.ann.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;


/**
 * Loads a source of lines (either from a file or from stdin) as an Iterator.
 * 
 * @see AbstractIterator
 */
public class LineIterator extends AbstractIterator<String> {

    private final BufferedReader reader;

    public LineIterator(BufferedReader reader) {
        this.reader = reader;
    }

    public LineIterator(InputStream is) {
        this.reader = new BufferedReader(new InputStreamReader(is));
    }

    /**
     * Creates a new instance from a file.
     * 
     * @param path
     *            Name of the file. If it ends in '.gz', it will be loaded as a
     *            Gzip file.
     * @return
     * @throws IOException
     */
    public static LineIterator load(String path) throws IOException {
        return load(new File(path));
    }

    /**
     * Creates a new instance from a file.
     * 
     * @param file
     *            Input file. If its name ends in '.gz', it will loaded as a
     *            Gzip file.
     * 
     * @return
     * @throws IOException
     */
    public static LineIterator load(File file) throws IOException {
        boolean gzip = file.getName().toLowerCase().endsWith(".gz");
        return load(new FileInputStream(file), gzip);
    }

    public static LineIterator load(InputStream is, boolean gzip) throws IOException {
        if (gzip) {
            is = new GZIPInputStream(is);
        }
        return new LineIterator(is);
    }

    @Override
    protected String computeNext() {
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                return line;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return endOfData();
        }
        return endOfData();
    }

    public void close() throws IOException {
        reader.close();
    }

}