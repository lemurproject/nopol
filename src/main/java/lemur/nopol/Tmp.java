package lemur.nopol;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.jwat.common.Payload;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

public class Tmp {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: Tmp file.warc.gz n");
            System.exit(1);
        }
        String warcFile = args[0];
        int maxRecords = Integer.parseInt(args[1]);
        FileInputStream input = new FileInputStream(warcFile);

        WarcReader reader = WarcReaderFactory.getReader(new GZIPInputStream(input));
        Iterator<WarcRecord> iter = reader.iterator();

        int n = 0;
        String fmt = "%8s\t%8s\t%8s\t%8s\t%8s\t%8s\t%8s\t%8s\n";
        
        System.out.printf(fmt, "start", 
                "|w-header|", "|content|", "content.L", "http", "body", "h+b", "next");
        while (n < maxRecords) {
            WarcRecord rec = iter.next();

            if (!ResponseIterator.isResponse(rec)){
                continue;
            }
            Payload pay = rec.getPayload();
            byte[] httpHead = rec.getHttpHeader().getHeader();
            
            InputStream bodyIs = pay.getInputStream();
            byte[] bodyBytes = IOUtils.toByteArray(bodyIs);
            
            InputStream payIs = pay.getInputStreamComplete();
            byte[] payBytes = IOUtils.toByteArray(payIs);
            
            long nextStart = rec.getStartOffset() + rec.header.headerBytes.length + pay.getTotalLength();
            
            System.out.printf(fmt,
                    rec.getStartOffset(),
                    rec.header.headerBytes.length,
                    pay.getTotalLength(),
                    payBytes.length,
                    httpHead.length,
                    bodyBytes.length,
                    httpHead.length + bodyBytes.length,
                    nextStart);

            //String s = new String(httpHead).replaceAll("\n", "*");
            //System.err.printf("'%s'\n", s);

            String s = new String(Arrays.copyOfRange(bodyBytes, bodyBytes.length-10, bodyBytes.length-1));
            System.err.printf("'%s'\n", s);
            
            n++;
        }

        reader.close();
        input.close();
    }
}
