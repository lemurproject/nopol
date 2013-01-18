package lemur.nopol;

import java.io.IOException;
import java.util.Arrays;

import lemur.nopol.io.WarcRecord;

public class Exporter {
    public byte[] exportRecord(WarcRecord record) throws IOException {
        byte[] content = record.getContent();
        return processContent(content);   
    }

    protected byte[] extractBody(byte[] content) {
    	int bodyStart = 0;
    	for (int i=0; i < content.length - 1; i++){
    		if (content[i] == '\n' && content[i+1] == '\n'){
    			bodyStart = i+2;
    			break;
    		}
    	}
		return Arrays.copyOfRange(content, bodyStart , content.length);
    }
    
	protected byte[] processContent(byte[] content) {
		return extractBody(content);
	}
	
}
