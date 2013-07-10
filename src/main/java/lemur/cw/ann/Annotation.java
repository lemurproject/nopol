package lemur.cw.ann;

import java.util.Iterator;

import lemur.cw.ann.util.AbstractIterator;

public class Annotation {

    /**
     * Number of columns in the converted format (6 original + 1)
     */
    public static final int NUM_COLUMNS_CONVERTED = 7;
    
    public final String trecId;
    public final int start;
    public final int end;
    public final float scoreCtx;
    public final float scoreInd;
    public final String mId;
    public final String text;

    public Annotation(String trecId, String text, int start, int end, float scoreCtx, float scoreInd, String mId) {
        this.trecId = trecId;
        this.text = text;
        this.start = start;
        this.end = end;
        this.scoreCtx = scoreCtx;
        this.scoreInd = scoreInd;
        this.mId = mId;
    }

    public static Annotation load(String line) {
        String[] cols = line.split("\t");

        if (cols.length != NUM_COLUMNS_CONVERTED) {
            throw new IllegalArgumentException(String.format(
                    "Incorrect line format. Expected 5 columns, found %d", cols.length));
        }

        int start = Integer.parseInt(cols[2]);
        int end = Integer.parseInt(cols[3]);
        float scoreCtx = Float.parseFloat(cols[4]);
        float scoreInd =  Float.parseFloat(cols[5]);
        String mbId = cols[6];

        return new Annotation(cols[0], cols[1], start, end, scoreCtx, scoreInd, mbId);
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
