package digital.paynetics.phos.screens.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

import com.db.chart.model.ChartSet;
import com.db.chart.model.LineSet;
import com.db.chart.model.Point;
import com.db.chart.view.LineChartView;

import java.util.ArrayList;
import java.util.Locale;

import digital.paynetics.phos.R;
import digital.paynetics.phos.classes.helpers.Convert;


public class PhosLineChartView extends LineChartView {

    private boolean simplifyValues = true;

    private Paint labelsPaint;

    private int labelOffset;

    public PhosLineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initStuff();
    }


    public PhosLineChartView(Context context) {
        super(context);
        initStuff();
    }

    private void initStuff() {
        labelsPaint = new Paint();
        labelsPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.txtSubtitleSmaller));

        labelOffset = Convert.dpToPixels(16, this.getContext());
    }

    @Override
    public void onDrawChart(Canvas canvas, ArrayList<ChartSet> data) {

        super.onDrawChart(canvas, data);

        LineSet lineSet;

        for (ChartSet set : data) {

            lineSet = (LineSet) set;

            if (lineSet.isVisible()) {
                //Draw point labels
                drawPointLabels(canvas, lineSet);
            }
        }

    }



    private void drawPointLabels(Canvas canvas, LineSet set) {

        int begin = set.getBegin();
        int end = set.getEnd();
        Point dot;
        for (int i = begin; i < end; i++) {

            dot = (Point) set.getEntry(i);

            if (dot.isVisible()) {

                Float v = dot.getValue();

                String value;

                if (v < 1000) { // 0-999
                    value = String.format(Locale.US, "%.2f", v);
                } else if (v < 1000000) { // 1000 - 999999
                    v = v / 1000;
                    value = String.format(Locale.US, "%.1fk", v);
                } else {
                    v = v / 1000000;
                    value = String.format(Locale.US, "%.1fm", v);
                }

                float textWidth = labelsPaint.measureText(value);


                canvas.drawText(value, dot.getX() - (textWidth/2), dot.getY() + labelOffset + dot.getRadius(), labelsPaint);

            }
        }

    }

    public void setLabelsPaintColor(int labelsPaintColor) {
        labelsPaint.setColor(labelsPaintColor);
    }

}
