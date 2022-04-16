package it.sisd.pytorchreimpl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class Dataset<T> implements IDataset<T> {
    protected final static Map<String, Runnable> functions = new HashMap<>();

    public abstract T get(int index);
    public abstract int len();

    // MISSING: 'add' function

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return index < len();
            }

            @Override
            public T next() {
                return get(index++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove not supported");
            }
        };
    }

    // replace '__getattr__' with a simple getter, for now
    public Runnable getFunction(String functionName) {
        return functions.get(functionName);
    }

    public static void register_functions(String functionName, Runnable function) {
        functions.put(functionName, function);
    }

    // MISSING: 'registerDatapipeAsFunction'
}
