package lemur.nopol.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.jwat.warc.WarcRecord;
import org.jwat.warc.WarcWriter;
import org.jwat.warc.WarcWriterFactory;

public class Tar2Warc {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: Tar2Warc input.tar.gz output.warc.gz");
            System.exit(1);
        }
        
        String inFile = args[0];
        String outFile = args[1];        
        
        WarcWriter writer = WarcWriterFactory.getWriter(new FileOutputStream(outFile), true);
        
        GZIPInputStream gzipIs = new GZIPInputStream(new FileInputStream(inFile));
        TarArchiveInputStream tIs = new TarArchiveInputStream(gzipIs);
        ArchiveEntry entry = tIs.getNextEntry();
        while (entry != null){
            String trecId = entry.getName().replaceAll("\\.html", "");

            WarcRecord record = WarcRecord.createRecord(writer);
            //record.header.
            record.header.addHeader("WARC-TREC-ID", trecId);
            record.header.contentLength = entry.getSize();
            record.header.warcTypeStr = "response";

            writer.writeHeader(record);
            //writer.writeRawHeader(header_bytes, contentLength);
            writer.streamPayload(tIs);
            writer.closeRecord();
            
            entry = tIs.getNextEntry();

        }
        
        tIs.close();
        writer.close();
    }
}
