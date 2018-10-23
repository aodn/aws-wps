package au.org.emii.aggregator.index;

import ucar.ma2.Index;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An index chunk iterator which returns the next chunk of the index up to the requested maximum number of elements.
 * May return a chunk smaller than the maximum number of elements if that's all that's left.
 * Used to read up to the requested maximum number of elements from a variable at a time.
 */
public class IndexChunkIterator implements Iterator<IndexChunk> {
    private final int[] shape;
    private final long maxChunkElems;
    private final int rank;
    private final long[] stride;
    private final long size;

    private int[] currentOffset;

    /**
     * Create a new iterator to return chunks of the index up to {@code maxChunkElems} at a time for
     * an array of shape {@code shape}
     * @param shape
     * @param maxChunkElems
     */
    public IndexChunkIterator(int[] shape, long maxChunkElems) {
        this.shape = shape.clone();
        this.maxChunkElems = maxChunkElems;
        this.rank = shape.length;
        this.stride = buildStride(shape);
        this.size = calcSize(shape);
        this.currentOffset = new int[rank];
    }

    /**
     * Are there more index chunks to read?
     */

    @Override
    public boolean hasNext() {
        return getCurrent1DOffset() < size;
    }

    /**
     * Return the next index chunk
     */
    @Override
    public IndexChunk next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        IndexChunk next = new IndexChunk(currentOffset, getChunkShape());
        setCurrent1DOffset(getCurrent1DOffset() + Index.computeSize(next.getShape()));
        return next;
    }

    /**
     * Remove is not supported
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Return current offset in a 1D backing array of shape {@code shape}
     */
    private long getCurrent1DOffset() {
        long currentElement = 0L;

        for (int dim=0; dim<rank; dim++) {
            currentElement += currentOffset[dim] * stride[dim];
        }

        return currentElement;
    }

    /**
     * Set current offset from an offset in a 1D backing array of shape {@code shape}
     */
    private void setCurrent1DOffset(long value) {
        long remainder = value;

        for (int dim=0; dim<rank; dim++) {
            currentOffset[dim] = (int) (remainder / stride[dim]);
            remainder %= stride[dim];
        }
    }

    /**
     * Return next chunk of maxChunkElems in array of given shape starting from current offset in the array
     */
    private int[] getChunkShape() {
        int[] chunkShape = new int[rank];

        for (int iDim = 0; iDim < rank; ++iDim) {
            int size = (int) (maxChunkElems / stride[iDim]);
            size = (size == 0) ? 1 : size;
            size = Math.min(size, shape[iDim] - currentOffset[iDim]);
            chunkShape[iDim] = size;
        }

        return chunkShape;
    }

    /**
     * Build array of strides used to calculate the starting position of each dimension in a 1D backing
     * array of shape {@code shape}
     */
    private long[] buildStride(int[] shape) {
        long[] result = new long[shape.length];

        long stride = 1;

        for (int dim=shape.length-1; dim>=0; dim--) {
            result[dim] = stride;
            stride *= shape[dim];
        }

        return result;
    }

    /**
     * Calculate the number of elements in an array of shape {@code shape}
     */
    private long calcSize(int[] shape) {
        long result = 1L;

        for (int dim=0; dim<shape.length; dim++) {
            result *= shape[dim];
        }

        return result;
    }

}
