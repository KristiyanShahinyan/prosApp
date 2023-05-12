package digital.paynetics.phos.common;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import digital.paynetics.phos.BR;


public abstract class BaseListAdapter<T>
        extends RecyclerView.Adapter<BaseListAdapter.BaseViewHolder<T>>
        implements ActionListener<T> {

    private List<T> items = new ArrayList<>();
    private Set<Integer> itemSeparatorSkipSet = new HashSet<>();

    public BaseListAdapter() {
        update(items);
    }

    public BaseListAdapter(List<T> items) {
        update(items);
    }

    @LayoutRes
    protected abstract int getLayoutResourceId(T item);

    @NonNull
    @Override
    public BaseViewHolder<T> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, viewType, parent, false);
        return new BaseViewHolder<>(binding, this);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder<T> holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemViewType(int position) {
        return getLayoutResourceId(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void update(@Nullable List<T> newItems) {
        if (newItems != null) {
            this.items.clear();
            this.items.addAll(newItems);
            notifyDataSetChanged();
        }
    }

    public void update(@Nullable List<T> newItems, Set<Integer> itemSeparatorSkipSet) {
        if (newItems != null) {
            this.items.clear();
            this.items.addAll(newItems);
            this.itemSeparatorSkipSet.clear();
            this.itemSeparatorSkipSet.addAll(itemSeparatorSkipSet);
            notifyDataSetChanged();
        }
    }

    public void updateWithDiff(@Nullable List<T> newItems) {
        if (newItems != null) {
            List<T> oldItems = this.items;
            this.items = newItems;

            DiffUtil.Callback callback = createDiffUtilCallback(oldItems, newItems);

            DiffUtil.calculateDiff(callback, true)
                    .dispatchUpdatesTo(this);
        }
    }

    Set<Integer> getItemSeparatorSkipSet() {
        return itemSeparatorSkipSet;
    }

    @NonNull
    private DiffUtil.Callback createDiffUtilCallback(@NonNull List<T> items,
                                                     @NonNull List<T> newItems) {
        throw new IllegalStateException("Callback not created");
    }

    static class BaseViewHolder<T> extends RecyclerView.ViewHolder {

        private final ViewDataBinding binding;
        private final ActionListener<T> actionListener;

        BaseViewHolder(ViewDataBinding binding, ActionListener<T> actionListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.actionListener = actionListener;
        }

        void bind(T state) {
            binding.setVariable(BR.state, state);
            binding.setVariable(BR.actionListener, actionListener);
            binding.executePendingBindings();
        }
    }
}
