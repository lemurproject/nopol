package lemur.nopol;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;

public class TarWriter {
		
	private TarArchiveOutputStream tarOs;

	public TarWriter(File output) throws IOException {
		FileOutputStream fileOs = new FileOutputStream(output);
		GZIPOutputStream gzOs = new GZIPOutputStream(new BufferedOutputStream(fileOs));
		tarOs = new TarArchiveOutputStream(gzOs);
	}

	public void writeRecord(String trecId, byte[] content) throws Exception {
		String fileName = trecId + ".html";
		TarArchiveEntry entry = new TarArchiveEntry(fileName);
		entry.setSize(content.length);
		tarOs.putArchiveEntry(entry);
		tarOs.write(content);
		tarOs.closeArchiveEntry();
	}

	public void close() throws IOException {
		tarOs.close();
	}
}
