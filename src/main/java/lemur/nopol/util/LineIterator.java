package lemur.nopol.util;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

	public LineIterator(String path) throws IOException {
		this.reader = new BufferedReader(new FileReader(path));
	}

    public LineIterator(File file) throws IOException {
        this.reader = new BufferedReader(new FileReader(file));
    }
	
	@Override
	protected String computeNext() {
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				return line;
			}
		} catch (IOException e) {
			return endOfData();
		}
		return endOfData();
	}

	public void close() throws IOException {
		reader.close();
	}

}
