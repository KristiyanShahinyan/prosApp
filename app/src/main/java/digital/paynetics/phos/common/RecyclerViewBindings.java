package digital.paynetics.phos.common;

import android.content.Context;
import androidx.databinding.BindingAdapter;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import android.view.View;

public class RecyclerViewBindings {

    @BindingAdapter(value = {"type",
            "horizontal_orientation",
            "reverse_layout",
            "span_count",
            "space_dp",
            "include_edge",
            "show_divider"},
            requireAll = false)
    public static void bindRecyclerViewLayout(RecyclerView view,
                                              String type,
                                              boolean horizontalOrientation,
                                              boolean reverseLayout,
                                              int spanCount,
                                              int spaceDp,
                                              boolean includeEdge,
                                              boolean showDivider) {
        Context context = view.getContext();

        int orientation = horizontalOrientation ? LinearLayoutManager.HORIZONTAL
                : LinearLayoutManager.VERTICAL;

        spanCount = spanCount <= 0 ? 1 : spanCount;

        RecyclerView.LayoutManager layout;
        switch (type) {
            case "grid":
                layout = new GridLayoutManager(context, spanCount, orientation, reverseLayout);
                break;
            case "staggered":
                layout = new StaggeredGridLayoutManager(spanCount, orientation);
                break;
            case "linear":
                layout = new LinearLayoutManager(context, orientation, reverseLayout);
                break;
            default:
                throw new IllegalStateException("Unexpected layout manager type: " + type);

        }
        view.setLayoutManager(layout);

        int spaceInPx = (int) GeneralBindings.dp2px(view, spaceDp);
        view.addItemDecoration(new GridSpacingItemDecoration(spanCount, spaceInPx, includeEdge));

        // https://stackoverflow.com/questions/24618829/how-to-add-dividers-and-spaces-between-items-in-recyclerview
        if (showDivider) {
            view.addItemDecoration(new DividerItemDecoration(context, orientation));
        }
    }

    private static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect,
                                   @NonNull View view,
                                   @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;

                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) {
                    outRect.top = spacing;
                }
            }
        }
    }
}
