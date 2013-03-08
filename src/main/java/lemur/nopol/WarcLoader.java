package lemur.nopol;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

/**
 * 
 * 
 *
 */
public abstract class WarcLoader {

    /**
     * Loads an input WARC file.
     * 
     * @param inFile
     * @return
     * @throws IOException
     */
    private static DataInputStream readInputFile(File inFile) throws IOException {
        return new DataInputStream(new GZIPInputStream(new FileInputStream(inFile)));
    }
    
    /**
     * (Oh, Java, why you don't have tuples?) 
     */
    public static class WarcEntry {
        public final byte[] content;
        public final String trecId;
        public final long offset;

        public WarcEntry(String trecId, byte[] content, long offset) {
            this.trecId = trecId;
            this.content = content;
            this.offset = offset;
        }
    }

    public abstract int errors();
    
    public Iterator<WarcEntry> loadFile(String fileName) throws IOException {
        return load(readInputFile(new File(fileName)));
    }
    
    public Iterator<WarcEntry> loadFile(File file) throws IOException {
        return load(readInputFile(file));
    }
    
    public abstract Iterator<WarcEntry> load(DataInputStream input) throws IOException;
    
}
