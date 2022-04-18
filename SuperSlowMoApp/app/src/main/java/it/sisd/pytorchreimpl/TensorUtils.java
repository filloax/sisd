package it.sisd.pytorchreimpl;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.util.Arrays;

public class TensorUtils {
    public static Tensor cat(Tensor tensor1, Tensor tensor2, int dim) {
        return cat(new Tensor[]{tensor1, tensor2}, dim);
    }

    public static Tensor cat(Tensor[] tensors, int dim) {
        /*
        Target functionality
        >>> x = torch.randn(2, 3)
        >>> x
        tensor([[ 0.6580, -1.0969, -0.4614],
                [-0.1034, -0.5790,  0.1497]])
        >>> torch.cat((x, x, x), 0)
        tensor([[ 0.6580, -1.0969, -0.4614],
                [-0.1034, -0.5790,  0.1497],
                [ 0.6580, -1.0969, -0.4614],
                [-0.1034, -0.5790,  0.1497],
                [ 0.6580, -1.0969, -0.4614],
                [-0.1034, -0.5790,  0.1497]])
        >>> torch.cat((x, x, x), 1)
        tensor([[ 0.6580, -1.0969, -0.4614,  0.6580, -1.0969, -0.4614,  0.6580,
                 -1.0969, -0.4614],
                [-0.1034, -0.5790,  0.1497, -0.1034, -0.5790,  0.1497, -0.1034,
                 -0.5790,  0.1497]])
         */

        if(tensors.length < 2){
            throw new IllegalArgumentException("Number of tensors must be at least 2.");
        }

        if(dim < 0){
            throw new NotImplementedException("Negative input dimensions are not supported in this java reimplementation.");
        }

        if(dim > tensors[0].shape().length){
            throw new IndexOutOfBoundsException("Dimension out of range (expected to be in range [0, " + (tensors[0].shape().length-1) + "] but got " + dim + ".");
        }

        for(int i=1;i<tensors.length;i++){
            // Check that all tensors have the same number of dimensions (shape lengths)
            if(tensors[0].shape().length != tensors[i].shape().length){
                throw new IllegalArgumentException("Tensors must have the same number of dimensions " +
                        "(expected " + tensors[0].shape().length + " but got " + tensors[i].shape().length + "for tensor number " + i + ")");
            }
            if(tensors[0].dtype() != tensors[i].dtype()) {
                throw new IllegalArgumentException("Tensors must have the same dtype " +
                        "(expected " + tensors[0].dtype() + " but got " + tensors[i].dtype() + "for tensor number " + i + ")");
            }
        }

        // Check that all tensors have matching sizes except for dimension 0 (shape1[i] == shape2[i] with i>0)
        for(int i=1;i<tensors.length;i++){
            for(int j=0;j<tensors[0].shape().length;j++) {
                if (tensors[0].shape()[j] != tensors[i].shape()[j]) {
                    throw new IllegalArgumentException("Sizes of tensors must match except in dimension 0. Expected size " + tensors[0].shape()[j] +
                            "but got size " + tensors[i].shape()[j] + " for tensor number " + i + ".");
                }
            }
        }

        int numTensors = tensors.length;

        long[] newShape = tensors[0].shape().clone();
        // Number of elements to copy from each tensor after each "jump"
        int[] toCopy = new int[tensors.length];
        int numJumps = 0;
        int newIndex = 0;

        // The shape of the resulting tensor will be equal to the first tensor's shape except for the "target" dimension,
        // which consists in the sum of the N dimension at index <dim>
        for(int i=1;i<numTensors;i++){
            newShape[dim] = newShape[dim] + tensors[i].shape()[dim];
        }

        Arrays.fill(toCopy, 1);
        for(int i=0;i<tensors.length;i++){
            for(int j=dim;j<tensors[i].shape().length;j++){
                toCopy[i] *= tensors[i].shape()[j];
            }
        }

        Tensor out;

        switch (tensors[0].dtype()) {
            case INT8: out = tensorCat_Byte(tensors, numTensors, toCopy, newIndex, numJumps, dim, newShape); break;
            case UINT8: out = tensorCat_UByte(tensors, numTensors, toCopy, newIndex, numJumps, dim, newShape); break;
            case INT32: out = tensorCat_Int(tensors, numTensors, toCopy, newIndex, numJumps, dim, newShape); break;
            case INT64: out = tensorCat_Long(tensors, numTensors, toCopy, newIndex, numJumps, dim, newShape); break;
            case FLOAT32: out = tensorCat_Float(tensors, numTensors, toCopy, newIndex, numJumps, dim, newShape); break;
            case FLOAT64: out = tensorCat_Double(tensors, numTensors, toCopy, newIndex, numJumps, dim, newShape); break;
            default: throw new RuntimeException("Wrong dtype, shouldn't happen");
        }

        //debug
        System.out.println("newIndex = " + newIndex + "; numel = " + out.numel());

        return out;
    }

    private static Tensor tensorCat_Long(Tensor[] tensors, int numTensors, int[] toCopy, int newIndex, int numJumps, int dim, long[] newShape) {
        long[] firstTensorData = tensors[0].getDataAsLongArray();
        long[] newData = new long[firstTensorData.length * numTensors];

        // Last iteration is to append elements at the end of the tensor
        for(int i=0;i<=firstTensorData.length; i++){
            if(i<firstTensorData.length && (i == 0 || i % toCopy[0] != 0)){
                // Simply copy elements from the first tensor
                newData[newIndex++] = firstTensorData[i];
            }
            else if(numJumps < dim+1){
                // Copy elements from the other tensors
                for(int j=1;j<tensors.length;j++){
                    for(int k = toCopy[j]*numJumps; k < toCopy[j]*(numJumps+1); k++){
                        // getDataAsIntArray fires an exception because apparently data is stored as long
                        // (although it works perfectly fine for tensors[0], see declaration of "firstTensorData" above)
                        newData[newIndex++] = tensors[j].getDataAsLongArray()[k];
                    }
                }

                if(i<firstTensorData.length)
                    newData[newIndex++] = firstTensorData[i];

                numJumps++;
            }
        }
        return Tensor.fromBlob(newData, newShape);
    }

    private static Tensor tensorCat_Int(Tensor[] tensors, int numTensors, int[] toCopy, int newIndex, int numJumps, int dim, long[] newShape) {
        int[] firstTensorData = tensors[0].getDataAsIntArray();
        int[] newData = new int[firstTensorData.length * numTensors];

        // Last iteration is to append elements at the end of the tensor
        for(int i=0;i<=firstTensorData.length; i++){
            if(i<firstTensorData.length && (i == 0 || i % toCopy[0] != 0)){
                // Simply copy elements from the first tensor
                newData[newIndex++] = firstTensorData[i];
            }
            else if(numJumps < dim+1){
                // Copy elements from the other tensors
                for(int j=1;j<tensors.length;j++){
                    for(int k = toCopy[j]*numJumps; k < toCopy[j]*(numJumps+1); k++){
                        // getDataAsIntArray fires an exception because apparently data is stored as long
                        // (although it works perfectly fine for tensors[0], see declaration of "firstTensorData" above)
                        newData[newIndex++] = tensors[j].getDataAsIntArray()[k];
                    }
                }

                if(i<firstTensorData.length)
                    newData[newIndex++] = firstTensorData[i];

                numJumps++;
            }
        }
        return Tensor.fromBlob(newData, newShape);
    }

    private static Tensor tensorCat_Byte(Tensor[] tensors, int numTensors, int[] toCopy, int newIndex, int numJumps, int dim, long[] newShape) {
        byte[] firstTensorData = tensors[0].getDataAsByteArray();
        byte[] newData = new byte[firstTensorData.length * numTensors];

        // Last iteration is to append elements at the end of the tensor
        for(int i=0;i<=firstTensorData.length; i++){
            if(i<firstTensorData.length && (i == 0 || i % toCopy[0] != 0)){
                // Simply copy elements from the first tensor
                newData[newIndex++] = firstTensorData[i];
            }
            else if(numJumps < dim+1){
                // Copy elements from the other tensors
                for(int j=1;j<tensors.length;j++){
                    for(int k = toCopy[j]*numJumps; k < toCopy[j]*(numJumps+1); k++){
                        // getDataAsIntArray fires an exception because apparently data is stored as long
                        // (although it works perfectly fine for tensors[0], see declaration of "firstTensorData" above)
                        newData[newIndex++] = tensors[j].getDataAsByteArray()[k];
                    }
                }

                if(i<firstTensorData.length)
                    newData[newIndex++] = firstTensorData[i];

                numJumps++;
            }
        }
        return Tensor.fromBlob(newData, newShape);
    }

    private static Tensor tensorCat_UByte(Tensor[] tensors, int numTensors, int[] toCopy, int newIndex, int numJumps, int dim, long[] newShape) {
        byte[] firstTensorData = tensors[0].getDataAsUnsignedByteArray();
        byte[] newData = new byte[firstTensorData.length * numTensors];

        // Last iteration is to append elements at the end of the tensor
        for(int i=0;i<=firstTensorData.length; i++){
            if(i<firstTensorData.length && (i == 0 || i % toCopy[0] != 0)){
                // Simply copy elements from the first tensor
                newData[newIndex++] = firstTensorData[i];
            }
            else if(numJumps < dim+1){
                // Copy elements from the other tensors
                for(int j=1;j<tensors.length;j++){
                    for(int k = toCopy[j]*numJumps; k < toCopy[j]*(numJumps+1); k++){
                        // getDataAsIntArray fires an exception because apparently data is stored as long
                        // (although it works perfectly fine for tensors[0], see declaration of "firstTensorData" above)
                        newData[newIndex++] = tensors[j].getDataAsUnsignedByteArray()[k];
                    }
                }

                if(i<firstTensorData.length)
                    newData[newIndex++] = firstTensorData[i];

                numJumps++;
            }
        }
        return Tensor.fromBlob(newData, newShape);
    }

    private static Tensor tensorCat_Float(Tensor[] tensors, int numTensors, int[] toCopy, int newIndex, int numJumps, int dim, long[] newShape) {
        float[] firstTensorData = tensors[0].getDataAsFloatArray();
        float[] newData = new float[firstTensorData.length * numTensors];

        // Last iteration is to append elements at the end of the tensor
        for(int i=0;i<=firstTensorData.length; i++){
            if(i<firstTensorData.length && (i == 0 || i % toCopy[0] != 0)){
                // Simply copy elements from the first tensor
                newData[newIndex++] = firstTensorData[i];
            }
            else if(numJumps < dim+1){
                // Copy elements from the other tensors
                for(int j=1;j<tensors.length;j++){
                    for(int k = toCopy[j]*numJumps; k < toCopy[j]*(numJumps+1); k++){
                        // getDataAsIntArray fires an exception because apparently data is stored as long
                        // (although it works perfectly fine for tensors[0], see declaration of "firstTensorData" above)
                        newData[newIndex++] = tensors[j].getDataAsFloatArray()[k];
                    }
                }

                if(i<firstTensorData.length)
                    newData[newIndex++] = firstTensorData[i];

                numJumps++;
            }
        }
        return Tensor.fromBlob(newData, newShape);
    }

    private static Tensor tensorCat_Double(Tensor[] tensors, int numTensors, int[] toCopy, int newIndex, int numJumps, int dim, long[] newShape) {
        double[] firstTensorData = tensors[0].getDataAsDoubleArray();
        double[] newData = new double[firstTensorData.length * numTensors];

        // Last iteration is to append elements at the end of the tensor
        for(int i=0;i<=firstTensorData.length; i++){
            if(i<firstTensorData.length && (i == 0 || i % toCopy[0] != 0)){
                // Simply copy elements from the first tensor
                newData[newIndex++] = firstTensorData[i];
            }
            else if(numJumps < dim+1){
                // Copy elements from the other tensors
                for(int j=1;j<tensors.length;j++){
                    for(int k = toCopy[j]*numJumps; k < toCopy[j]*(numJumps+1); k++){
                        // getDataAsIntArray fires an exception because apparently data is stored as long
                        // (although it works perfectly fine for tensors[0], see declaration of "firstTensorData" above)
                        newData[newIndex++] = tensors[j].getDataAsDoubleArray()[k];
                    }
                }

                if(i<firstTensorData.length)
                    newData[newIndex++] = firstTensorData[i];

                numJumps++;
            }
        }
        return Tensor.fromBlob(newData, newShape);
    }
}
