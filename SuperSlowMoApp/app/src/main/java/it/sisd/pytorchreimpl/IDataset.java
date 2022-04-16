package it.sisd.pytorchreimpl;

public interface IDataset<T> extends Iterable<T> {
    public abstract T get(int index);
    public abstract int len();
}
