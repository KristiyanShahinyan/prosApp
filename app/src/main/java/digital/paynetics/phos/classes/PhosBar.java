package digital.paynetics.phos.classes;

import com.db.chart.model.Bar;


public class PhosBar extends Bar {

    public static int defaultColor = 0;

    public PhosBar(String label, float value) {
        super(label, value);
        setColor(defaultColor);
    }
}
