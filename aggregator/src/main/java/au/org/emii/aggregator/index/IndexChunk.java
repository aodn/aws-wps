package au.org.emii.aggregator.index;

/**
 * Index chunk
 */
public class IndexChunk {
    private int[] offset;
    private int[] shape;

    public IndexChunk(int[] offset, int[] shape) {
        this.offset = offset.clone();
        this.shape = shape.clone();
    }


    public int[] getOffset() {
        return offset.clone();
    }

    public int[] getShape() {
        return shape.clone();
    }
}
