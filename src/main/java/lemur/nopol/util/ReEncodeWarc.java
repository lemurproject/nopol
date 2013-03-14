package lemur.nopol.util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;
import org.jwat.warc.WarcWriter;
import org.jwat.warc.WarcWriterFactory;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

/**
 * 
 * 
 *
 */
public class ReEncodeWarc {

    private WarcWriter writer;

    public ReEncodeWarc(WarcWriter writer) {
        this.writer = writer;
    }

    public void encodeRecords(WarcReader reader) throws IOException {
        Iterator<WarcRecord> inputIter = reader.iterator();
        int n = 0;
        while (inputIter.hasNext()) {
            try {
                WarcRecord record = inputIter.next();

                byte[] content = IOUtils.toByteArray(record.getPayloadContent());
                CharsetMatch match = detectEncoding(content);

                ByteArrayOutputStream bos = new ByteArrayOutputStream((int) record.getPayload().getTotalLength());
                bos.write(record.getHttpHeader().getHeader());
                bos.write(match.getString().getBytes("UTF-8"));
                bos.close();
                byte[] payload = bos.toByteArray();

                System.out.printf("%s\n", match.getName());

                writer.writeRawHeader(record.header.headerBytes, (long) payload.length);
                writer.writePayload(payload);
                writer.closeRecord();            
                n++;
            } catch (Exception e) {
                System.out.printf("Error processing record: %d %s\n", n, e);
            }
        }
    }

    static CharsetMatch detectEncoding(byte[] content){
        CharsetDetector detector = new CharsetDetector();
        detector.enableInputFilter(true);
        detector.setText(content);

        return detector.detect();
    }
    
    
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: ReEncodeWarc input.warc.gz output.warc.gz");
            System.exit(1);
        }

        String inFile = args[0];
        String outFile = args[1];

        InputStream gzIs = new GZIPInputStream(new FileInputStream(inFile));
        OutputStream fOs = new FileOutputStream(outFile);

        WarcReader reader = WarcReaderFactory.getReader(gzIs);
        WarcWriter writer = WarcWriterFactory.getWriter(fOs, true);

        ReEncodeWarc extractor = new ReEncodeWarc(writer);
        extractor.encodeRecords(reader);
        writer.close();
    }
}
