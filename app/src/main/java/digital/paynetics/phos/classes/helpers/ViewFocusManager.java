package digital.paynetics.phos.classes.helpers;

import android.animation.LayoutTransition;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class ViewFocusManager implements View.OnFocusChangeListener,
        LayoutTransition.TransitionListener {

    // TODO: 11/27/2019 Save this state on config changes?
    private View viewOnFocus;

    @Inject
    ViewFocusManager() {
    }

    @Override
    public void startTransition(LayoutTransition transition,
                                ViewGroup container,
                                View view,
                                int transitionType) {

    }

    @Override
    public void endTransition(LayoutTransition transition,
                              ViewGroup container,
                              View view,
                              int transitionType) {
        if (viewOnFocus != null) {
            viewOnFocus.requestFocus();
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            viewOnFocus = v;
        }
    }

    public LayoutTransition.TransitionListener getLayoutTransitionListener() {
        return this;
    }

    public void setFocusChangeListenerToView(View view) {
        view.setOnFocusChangeListener(this);
    }
}
