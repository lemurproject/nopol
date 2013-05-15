package lemur.cw.ann.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * (Oh, Java, why you don't have tuples?)
 */
public class WarcEntry {
    public final byte[] body;
    public final String trecId;
    public final int contentOffset;
    public final String contentType;
    public final byte[] httpHeader;

    public WarcEntry(String trecId, byte[] content, byte[] httpHeader, int contentOffset,
            String contentType) {
        this.trecId = trecId;
        this.body = content;
        this.httpHeader = httpHeader;
        this.contentOffset = contentOffset;
        this.contentType = contentType;
    }

    public byte[] content() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(httpHeader.length
                + body.length);
        bos.write(httpHeader);
        bos.write(body);
        bos.close();
        return bos.toByteArray();
    }
}