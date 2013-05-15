package lemur.nopol;

import java.io.FileInputStream;
import java.io.IOException;

import lemur.nopol.ResponseIterator.WarcEntry;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

public class CharDet {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: CharDet file.warc.gz");
            System.exit(1);
        }
        String warcFile = args[0];
        FileInputStream input = new FileInputStream(warcFile);
        ResponseIterator responses = new ResponseIterator(input);


        
        int n = 0;
        while (responses.hasNext()) {
            WarcEntry resp = responses.next();
            CharsetDetector detector = new CharsetDetector(); 
            detector.enableInputFilter(true);
            detector.setText(resp.content);
            /**
            CharsetMatch[] charsets = detector.detectAll();
            for (CharsetMatch charsetMatch : charsets) {
                System.out.printf("%3d\t%s\n", charsetMatch.getConfidence(), charsetMatch.getName());                
            }
            System.out.println();
*/
  
            CharsetMatch charset = detector.detect();
            String detected = charset.getString();
            byte[] bytesUtf = detected.getBytes("UTF-8");
  
            String contentAsUtf = new String(resp.content, "UTF-8");
            
            System.out.printf("%s\t%d\t%s\t%d\t%d\t%d\n", resp.trecId, charset.getConfidence(), 
                    charset.getName(), detected.length(), bytesUtf.length, resp.content.length);

            System.out.printf("asUtf. chars=%s bytes=%s\n\n", contentAsUtf.length(), contentAsUtf.getBytes("UTF-8").length); 
            
            
            n += 1;
        }
        System.err.printf("Records: %d\n", n);
    }
}
