package it.sisd.pytorchreimpl;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pytorch.Tensor;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TensorUtilsTest {

    @Before
    public void up() {
    }

    @Test
    public void testCat() {
        int[] data1 = {1,2,3,4};
        long[] data2 = {5,6,7,8};
        long[] shape = {2, 2};
        /*
             1 2       5 6
        t1 = 3 4  t2 = 7 8
        */
        Tensor t1 = Tensor.fromBlob(data1, shape);
        Tensor t2 = Tensor.fromBlob(data2, shape);

        Tensor outVert = TensorUtils.cat(new Tensor[]{t1, t2}, 0);
        int dataOutVert[] = outVert.getDataAsIntArray();
        /*
              1 2     
              3 4
              5 6
        out = 7 8
        */
        for (int i = 0; i < 8; i++)
            assertEquals(i + 1, dataOutVert[i]);

        Tensor outHori = TensorUtils.cat(new Tensor[]{t1, t2}, 1);
        int dataOutHori[] = outHori.getDataAsIntArray();
        /*
              1 2 5 6
        out = 3 4 7 8
         */
        assertEquals(1, dataOutHori[0]);
        assertEquals(2, dataOutHori[1]);
        assertEquals(5, dataOutHori[2]);
        assertEquals(6, dataOutHori[3]);
        assertEquals(3, dataOutHori[4]);
        assertEquals(4, dataOutHori[5]);
        assertEquals(7, dataOutHori[6]);
        assertEquals(8, dataOutHori[7]);
    }

    @Test
    public void testCatFloatDouble() {
        float[] data1 = {1.5f,2.5f,3.5f,4.5f};
        double[] data2 = {5.5,6.5,7.5,8.5};
        long[] shape = {2, 2};
        /*
             1 2       5 6
        t1 = 3 4  t2 = 7 8
        */
        Tensor t1_fail = Tensor.fromBlob(data1, shape);
        Tensor t2 = Tensor.fromBlob(data2, shape);

        assertThrows(IllegalArgumentException.class, () -> TensorUtils.cat(t1_fail, t2, 0));

        double[] data1AsDouble = new double[data1.length];
        for(int i = 0; i < data1.length; i++) data1AsDouble[i] = (double) data1[i];
        Tensor t1 = Tensor.fromBlob(data1AsDouble, shape);

        Tensor outVert = TensorUtils.cat(new Tensor[]{t1, t2}, 0);
        double dataOutVert[] = outVert.getDataAsDoubleArray();
        /*
              1.5 2.5
              3.5 4.5
              5.5 6.5
        out = 7.5 8.5
        */
        for (int i = 0; i < 8; i++)
            assertEquals(i + 1.5f, dataOutVert[i], 0.01);

        Tensor outHori = TensorUtils.cat(new Tensor[]{t1, t2}, 1);
        double dataOutHori[] = outHori.getDataAsDoubleArray();
        /*
              1 2 5 6
        out = 3 4 7 8
         */
        assertEquals(1.5, dataOutHori[0], 0.01);
        assertEquals(2.5, dataOutHori[1], 0.01);
        assertEquals(5.5, dataOutHori[2], 0.01);
        assertEquals(6.5, dataOutHori[3], 0.01);
        assertEquals(3.5, dataOutHori[4], 0.01);
        assertEquals(4.5, dataOutHori[5], 0.01);
        assertEquals(7.5, dataOutHori[6], 0.01);
        assertEquals(8.5, dataOutHori[7], 0.01);
    }

}
