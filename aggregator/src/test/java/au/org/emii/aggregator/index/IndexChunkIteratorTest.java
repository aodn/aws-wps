package au.org.emii.aggregator.index;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import ucar.ma2.InvalidRangeException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class IndexChunkIteratorTest {

    private static final Object[] TEST_1D_REGULAR_CHUNKS = {
        new int[][][]{
            {{0}, {2}},
            {{2}, {2}},
            {{4}, {2}},
            {{6}, {2}},
            {{8}, {2}}
        }, new int[]{10}, 2 };

    private static final Object[] TEST_1D_OVERSIZED_CHUNKS = {
        new int[][][]{
            {{0}, {5}}
        }, new int[]{5}, 7};

    private static final Object[] TEST_1D_IRREGULAR_CHUNKS = {
        new int[][][]{
            {{0}, {2}},
            {{2}, {2}},
            {{4}, {1}}},
        new int[]{5}, 2};

    private static final Object[] TEST_2D_REGULAR_CHUNKS = {
        new int[][][]{
            {{0, 0}, {1, 2}},
            {{0, 2}, {1, 2}},
            {{1, 0}, {1, 2}},
            {{1, 2}, {1, 2}}
        },
        new int[]{2, 4}, 2
    };

    private static final Object[] TEST_2D_IRREGULAR_CHUNKS = {
        new int[][][]{
            {{0, 0}, {1, 2}},
            {{0, 2}, {1, 2}},
            {{0, 4}, {1, 1}},
            {{1, 0}, {1, 2}},
            {{1, 2}, {1, 2}},
            {{1, 4}, {1, 1}}
        },
        new int[]{2, 5}, 2
    };

    private static final Object[] TEST_2D_OVERSIZE_CHUNKS = {
        new int[][][]{
            {{0, 0}, {1, 5}},
            {{1, 0}, {1, 5}}
        },
        new int[]{2, 5}, 7
    };

    private static final Object[] TEST_3D_REGULAR_CHUNKS = {
        new int[][][]{
            {{0, 0, 0}, {1, 1, 2}},
            {{0, 0, 2}, {1, 1, 2}},
            {{0, 1, 0}, {1, 1, 2}},
            {{0, 1, 2}, {1, 1, 2}},
            {{0, 2, 0}, {1, 1, 2}},
            {{0, 2, 2}, {1, 1, 2}},
            {{1, 0, 0}, {1, 1, 2}},
            {{1, 0, 2}, {1, 1, 2}},
            {{1, 1, 0}, {1, 1, 2}},
            {{1, 1, 2}, {1, 1, 2}},
            {{1, 2, 0}, {1, 1, 2}},
            {{1, 2, 2}, {1, 1, 2}}
        },
        new int[]{2, 3, 4}, 2
    };

    private static final Object[] TEST_3D_IRREGULAR_CHUNKS = {
        new int[][][]{
            {{0, 0, 0}, {1, 1, 3}},
            {{0, 0, 3}, {1, 1, 1}},
            {{0, 1, 0}, {1, 1, 3}},
            {{0, 1, 3}, {1, 1, 1}},
            {{0, 2, 0}, {1, 1, 3}},
            {{0, 2, 3}, {1, 1, 1}},
            {{1, 0, 0}, {1, 1, 3}},
            {{1, 0, 3}, {1, 1, 1}},
            {{1, 1, 0}, {1, 1, 3}},
            {{1, 1, 3}, {1, 1, 1}},
            {{1, 2, 0}, {1, 1, 3}},
            {{1, 2, 3}, {1, 1, 1}}
        },
        new int[]{2, 3, 4}, 3
    };

    private static final Object[] TEST_3D_OVERSIZE_CHUNKS = {
        new int[][][]{
            {{0, 0, 0}, {1, 3, 4}},
            {{1, 0, 0}, {1, 3, 4}}
        },
        new int[]{2, 3, 4}, 15
    };

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            TEST_1D_REGULAR_CHUNKS,
            TEST_1D_OVERSIZED_CHUNKS,
            TEST_1D_IRREGULAR_CHUNKS,
            TEST_2D_REGULAR_CHUNKS,
            TEST_2D_IRREGULAR_CHUNKS,
            TEST_2D_OVERSIZE_CHUNKS,
            TEST_3D_REGULAR_CHUNKS,
            TEST_3D_IRREGULAR_CHUNKS,
            TEST_3D_OVERSIZE_CHUNKS
        });
    }

    private final int[][][] expected;
    private final int[] shape;
    private final int maxChunkElems;

    public IndexChunkIteratorTest(int[][][] expected, int[] shape, int maxChunkElems) {
        this.expected = expected;
        this.shape = shape;
        this.maxChunkElems = maxChunkElems;
    }

    @Test
    public void testChunking() throws InvalidRangeException {
        IndexChunkIterator index = new IndexChunkIterator(shape, maxChunkElems);

        List<int[][]> result = new ArrayList<>();

        while (index.hasNext()) {
            IndexChunk next = index.next();
            int[][] iterationResult = new int[2][];
            iterationResult[0] = next.getOffset();
            iterationResult[1] = next.getShape();
            result.add(iterationResult);
        }

        assertEquals(expected.length, result.size());

        for (int i=0; i<expected.length; i++) {
            int[][] iterationResult = result.get(i);
            int[][] iterationExpected = expected[i];
            assertArrayEquals(iterationExpected[0], iterationResult[0]);
            assertArrayEquals(iterationExpected[1], iterationResult[1]);
        }
    }
}
