package lemur.nopol;

import java.util.Iterator;

import lemur.nopol.util.AbstractIterator;

public class Annotation {

    public final String trecId;
    public final int start;
    public final int end;
    public final float score;
    public final String mId;
    public final String text;

    public Annotation(String trecId, String text, int start, int end, float score, String mId) {
        this.trecId = trecId;
        this.text = text;
        this.start = start;
        this.end = end;
        this.score = score;
        this.mId = mId;
    }

    public static Annotation load(String line) {
        String[] cols = line.split("\t");

        if (cols.length != 6) {
            throw new IllegalArgumentException(String.format(
                    "Incorrect line format. Expected 5 columns, found %d", cols.length));
        }

        int start = Integer.parseInt(cols[2]);
        int end = Integer.parseInt(cols[3]);
        float score = Float.parseFloat(cols[4]);

        return new Annotation(cols[0], cols[1], start, end, score, cols[5]);
    }
    
    public static Iterator<Annotation> iterator(final Iterator<String> lines) {
        return new AbstractIterator<Annotation>() {
            @Override
            protected Annotation computeNext() {
                while (lines.hasNext()){
                    return Annotation.load(lines.next());
                }
                return endOfData();
            }
        };
    }
}
