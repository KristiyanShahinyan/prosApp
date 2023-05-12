package digital.paynetics.phos.common;

public interface ActionListener<T> {
    void onItemClick(T item);

    default void onItemClick(T item, int what) {
        onItemClick(item);
    }
}
