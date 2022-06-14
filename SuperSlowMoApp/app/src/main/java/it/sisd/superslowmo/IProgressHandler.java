package it.sisd.superslowmo;

public interface IProgressHandler {
    void publishProgress(float progress);
    void publishProgress(float progress, String message);
}
