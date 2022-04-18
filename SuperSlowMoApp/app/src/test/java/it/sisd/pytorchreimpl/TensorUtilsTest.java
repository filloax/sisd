package it.sisd.pytorchreimpl;

import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TensorUtilsTest {

    Tensor t1;
    Tensor t2;

    @Before
    public void up() {
        int[] data1 = {1,2,3,4};
        long[] data2 = {5,6,7,8};
        long[] shape = {2, 2};
        t1 = Tensor.fromBlob(data1, shape);
        t2 = Tensor.fromBlob(data2, shape);
    }

    @Test
    public void testTensor() {
        Tensor out = TensorUtils.cat2(new Tensor[]{t1, t2}, 0);
        System.out.print("[");
        for(int i=0;i<out.getDataAsIntArray().length;i++){
            System.out.print(out.getDataAsIntArray()[i]);
            if(i<out.getDataAsIntArray().length-1)
                System.out.print(",");
        }
        System.out.println("]");
        System.out.print("Shape: ");
        System.out.print("[");
        for(int i=0;i<out.shape().length;i++){
            System.out.print(out.shape()[i]);
            if(i<out.shape().length-1)
                System.out.print(",");
        }
        System.out.println("]");
    }
}
