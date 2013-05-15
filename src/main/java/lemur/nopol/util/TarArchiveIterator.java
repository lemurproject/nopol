package lemur.nopol.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import lemur.cw.ann.util.AbstractIterator;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Iterator over the entries of an ArchiveInputStream.
 * 
 */
public class TarArchiveIterator extends AbstractIterator<ArchiveEntry> {

    final TarArchiveInputStream tarIs;

    public TarArchiveIterator(TarArchiveInputStream tarIs) {
        super();
        this.tarIs = tarIs;
    }

    public static TarArchiveIterator openCompressed(File tarFile) throws IOException {
        return openCompressed(new FileInputStream(tarFile));
    }

    public static TarArchiveIterator openCompressed(InputStream inputStream)
            throws IOException {
        GZIPInputStream gzipIs = new GZIPInputStream(inputStream);
        TarArchiveInputStream tIs = new TarArchiveInputStream(gzipIs);
        return new TarArchiveIterator(tIs);
    }

    @Override
    protected ArchiveEntry computeNext() {
        ArchiveEntry entry;
        try {
            entry = tarIs.getNextEntry();
            while (entry != null) {
                return entry;
            }
            tarIs.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return endOfData();
    }

}
