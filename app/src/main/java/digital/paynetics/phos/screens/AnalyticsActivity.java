package digital.paynetics.phos.screens;

import android.graphics.Paint;
import android.os.Handler;
import android.util.LruCache;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.db.chart.animation.Animation;
import com.db.chart.model.BarSet;
import com.db.chart.model.ChartEntry;
import com.db.chart.model.LineSet;
import com.db.chart.renderer.AxisRenderer;
import com.db.chart.view.BarChartView;
import com.db.chart.view.ChartView;
import com.github.ybq.android.spinkit.SpinKitView;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import digital.paynetics.phos.PhosApplication;
import digital.paynetics.phos.R;
import digital.paynetics.phos.classes.PhosBar;
import digital.paynetics.phos.classes.enums.BrandingColors;
import digital.paynetics.phos.classes.enums.LoadingState;
import digital.paynetics.phos.classes.helpers.Convert;
import digital.paynetics.phos.classes.helpers.IdProvider;
import digital.paynetics.phos.classes.lang.PhosString;
import digital.paynetics.phos.screens.views.PhosLineChartView;
import digital.paynetics.phos.sdk.CompletionHandler;
import digital.paynetics.phos.sdk.ResponseCode;
import digital.paynetics.phos.sdk.entities.Analytics;
import digital.paynetics.phos.sdk.enums.AnalyticsType;
import digital.paynetics.phos.sdk.services.PhosConnect;


@EActivity(R.layout.activity_analytics)
public class AnalyticsActivity extends BaseActivity {


    @ViewById
    Button button1;

    @ViewById
    Button button2;

    @ViewById
    Button button3;

    @ViewById
    View selector;

    @ViewById
    BarChartView chartDaily;

    @ViewById
    BarChartView chartWeekly;

    @ViewById
    PhosLineChartView chartMonthly;

    @ViewById(R.id.loadingIndicator)
    LinearLayout indicator;

    @ViewById
    TextView labelCentered;

    @ViewById
    ScrollView scroll1;

    @ViewById
    TextView txtTitle;

    @ViewById
    TextView lblNetSales;

    @ViewById
    TextView lblAverageSale;

    @ViewById
    TextView lblSales;

    @ViewById
    TextView txtDate;

    @ViewById
    LinearLayout infoRow3;

    @ViewById
    TextView labelNetSales;

    @ViewById
    TextView labelAverageSale;

    @ViewById
    TextView labelSales;



    private boolean initCalled = false;
    private int currentPosition = -1;
    private int[] btn1loc = new int[2], btn2loc = new int[2], btn3loc = new int[2];

    private int colorPrimary;
    private int colorWhite;

    private AnalyticsType type = AnalyticsType.DAILY;

    private String textNoRecords;
    private String textError;

    private Double valueNet = 0.0;
    private Double valueAverage = 0.0;
    private int numSales = 0;

    private String currency = null;

    private Calendar cal = Calendar.getInstance();

    private DateFormat dateFormatMonthYear = new SimpleDateFormat("MMMM YYYY", Locale.US);
    private DateFormat dateFormatDayMonth = new SimpleDateFormat("d MMMM", Locale.US);

    private static int MAX_DAILY_COLUMNS = 24;
    private static int MAX_WEEKLY_COLUMNS = 7;

    Handler mainHandler;

    private final LruCache<String, Analytics> cache = new LruCache<>(100);

    private final AnalyticsRunnable analyticsRunnable = new AnalyticsRunnable();

    private class AnalyticsRunnable implements Runnable {

        Date date;
        AnalyticsType type;
        String timezone;

        @Override
        public void run() {
            phosConnect.loadAnalytics(date, type, timezone, new CompletionHandler.AnalyticsInfo() {
                @Override
                public void onSuccess(Analytics analyticsInfo) {
                    String key = createKey(date, type);
                    cache.put(key, analyticsInfo);
                    updateValues(analyticsInfo);
                    updateInterface(LoadingState.LOADED, analyticsInfo);
                }

                @Override
                public void onError(ResponseCode responseCode) {
                    updateInterface(LoadingState.FAILED);
                }
            });
        }

        void setDate(Date date) {
            this.date = date;
        }

        void setType(AnalyticsType type) {
            this.type = type;
        }

        void setTimezone(String timezone) {
            this.timezone = timezone;
        }
    }

    @Inject
    IdProvider idProvider;

    @AfterInject
    void onInjectDependencies() {
        PhosApplication.getAppComponent().inject(this);
    }

    @AfterViews
    public void afterViews() {

        dateFormatMonthYear = new SimpleDateFormat("MMMM YYYY", stringManager.getCurrentLocale());
        dateFormatDayMonth = new SimpleDateFormat("d MMMM", stringManager.getCurrentLocale());

        selector.setAlpha(0);

        colorPrimary = getColorPrimary();
        colorWhite = getResources().getColor(R.color.white);


        txtTitle.setText(stringManager.getString(PhosString.analytics));
        button1.setText(stringManager.getString(PhosString.daily));
        button2.setText(stringManager.getString(PhosString.weekly));
        button3.setText(stringManager.getString(PhosString.monthly));
        labelNetSales.setText(stringManager.getString(PhosString.net_sales));
        labelNetSales.setTextColor(getColorPrimary());
        labelAverageSale.setText(stringManager.getString(PhosString.avg_sales));
        labelAverageSale.setTextColor(getColorPrimary());
        labelSales.setText(stringManager.getString(PhosString.sales));
        labelSales.setTextColor(getColorPrimary());

        txtDate.setTextColor(getColorPrimary());

        textNoRecords = stringManager.getString(PhosString.no_records_found).toString();
        textError = stringManager.getString(PhosString.retry_msg).toString();

        PhosBar.defaultColor = colorPrimary;

        initChartView(chartDaily);
        initChartView(chartWeekly);
        initChartView(chartMonthly);
        chartMonthly.setYLabels(AxisRenderer.LabelPosition.NONE);
        chartMonthly.setLabelsPaintColor(getColorPrimary());

        ((SpinKitView) indicator.findViewById(R.id.loadingView)).setColor(getColorLoading());

        mainHandler = new Handler(getMainLooper());
    }

    private int getColorPrimary() {
        boolean dynamicColorsEnabled = false;
        return dynamicColorsEnabled
                ? clientConfig.getDynamicColor(BrandingColors.PRIMARY_COLOR)
                : getResources().getColor(R.color.analytics_primary);
    }

    private int getColorLoading() {
        boolean dynamicColorsEnabled = false;
        return dynamicColorsEnabled
                ? clientConfig.getDynamicColor(BrandingColors.LOADING_INDICATOR_COLOR)
                : getResources().getColor(R.color.loading_indicator);
    }

    private void initChartView(ChartView chartView) {
        chartView.setBackgroundColor(colorWhite);
        chartView.setYAxis(false);
        chartView.setXAxis(false);
        chartView.setAxisColor(getResources().getColor(R.color.blue_25tr));
//        chartView.setAxisThickness(1);
        chartView.setLabelsColor(getResources().getColor(R.color.analytics_chart_labels_color));
        chartView.setFontSize(Convert.dpToPixels(10, this));
    }

    private void initBarChartValues(BarChartView chartView, int columns, ArrayList<PhosBar> values) {

        if (values.size() == 0) {
            chartView.getData().clear();
            return;
        }

        BarSet set = new BarSet();

        for (PhosBar val : values) {
            set.addBar(val);
        }

        Paint paint = new Paint();
        paint.setColor(getResources().getColor(R.color.blue_25tr));

        float maxValue = 0;

        for (ChartEntry entry : set.getEntries()) {
            if (maxValue < entry.getValue())
                maxValue = entry.getValue();
        }

        chartView.setGrid(6, columns, paint);

        chartView.getData().clear();

        chartView.addData(set);

        float step = calcStepFromMaxValue(maxValue);

        if (maxValue == 0) {
            maxValue = 100;
            step = 100;
        }

        chartView.setAxisBorderValues(0, maxValue, step);
    }


    private void initLineChartValues(ArrayList<PhosBar> values) {
        if (values.size() == 0) {
            chartMonthly.getData().clear();
            return;
        }

        LineSet set = new LineSet();

        set.setColor(colorPrimary);
        set.setSmooth(true);

        for (PhosBar val : values) {
            set.addPoint(val.getLabel(), val.getValue());
        }

        set.setDotsRadius(Convert.dpToPixels(10, this));
        set.setDotsColor(colorPrimary);

        Paint paint = new Paint();
        paint.setColor(getResources().getColor(R.color.blue_25tr));

        float maxValue = 0;

        for (ChartEntry entry : set.getEntries()) {
            if (maxValue < entry.getValue())
                maxValue = entry.getValue();
        }

        chartMonthly.setGrid(6, set.size(), paint);

        chartMonthly.getData().clear();

        chartMonthly.addData(set);

        float step = calcStepFromMaxValue(maxValue);

        if (maxValue == 0) {
            maxValue = 100;
            step = 100;
        }

        chartMonthly.setAxisBorderValues(0 - step, maxValue, step);
        chartMonthly.setBorderSpacing(Convert.dpToPixels(28, this));
    }

    private float calcStepFromMaxValue(float maxValue) {
        float step = (maxValue / 2);
        if (step < 1) {
            return step;
        } else if (step < 10) {
            return Math.round(step);
        } else if (step < 100) {
            return Math.round(step / 10) * 10;
        } else if (step < 1000) {
            return Math.round(step / 100) * 100;
        } else {
            return Math.round(step / 1000) * 1000;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    private void showBarChart(BarChartView chartView, int columns, ArrayList<PhosBar> values) {
        initBarChartValues(chartView, columns, values);
        if (values.size() > 0) {
            Animation animation = new Animation(500);
            chartView.show(animation);
        } else {
            chartView.getData().clear();
        }
    }

    private void showLineChart(ArrayList<PhosBar> values) {
        initLineChartValues(values);
        if (values.size() > 0) {
            Animation animation = new Animation(500);
            chartMonthly.show(animation);
        } else {
            chartMonthly.getData().clear();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);


        button1.getLocationInWindow(btn1loc);
        button2.getLocationInWindow(btn2loc);
        button3.getLocationInWindow(btn3loc);

        initElements(0, false);
    }

    private void initElements(int newPosition, boolean animate) {
        if (initCalled) {
            return;
        }
        initCalled = true;
        updateElements(newPosition, animate);
    }

    private void updateElements(int newPosition, boolean animate) {
        if (newPosition != currentPosition) {

            if (!animate) {

                switch (currentPosition) {
                    case 0:
                        button1.setTextColor(getResources().getColor(R.color.transactions_btn_color));
                        break;
                    case 1:
                        button2.setTextColor(getResources().getColor(R.color.transactions_btn_color));
                        break;
                    case 2:
                        button3.setTextColor(getResources().getColor(R.color.transactions_btn_color));
                        break;
                }

                selector.setAlpha(1);

                switch (newPosition) {
                    case 0:
                        type = AnalyticsType.DAILY;
                        selector.setTranslationX(0);
                        if (getResources().getBoolean(R.bool.analytics_color_anim_enabled)) {
                            button1.setTextColor(getColorPrimary());
                        }
                        break;
                    case 1:
                        selector.setTranslationX(btn2loc[0] - btn1loc[0]);
                        if (getResources().getBoolean(R.bool.analytics_color_anim_enabled)) {
                            button2.setTextColor(getColorPrimary());
                        }
                        break;
                    case 2:
                        selector.setTranslationX(btn3loc[0] - btn1loc[0]);
                        if (getResources().getBoolean(R.bool.analytics_color_anim_enabled)) {
                            button3.setTextColor(getColorPrimary());
                        }
                        break;
                }

                currentPosition = newPosition;

            } else {

                // animated

                int animationDuration = SELECTOR_ANIMATION_DURATION;

                // double the time if animating from end to end
                if (newPosition == 2 && currentPosition == 0 || currentPosition == 2 && newPosition == 0)
                    animationDuration = SELECTOR_ANIMATION_DURATION_SLOWER;

                switch (newPosition) {
                    case 0:
                        type = AnalyticsType.DAILY;
                        selector.animate().setDuration(animationDuration).translationX(0);
                        break;
                    case 1:
                        type = AnalyticsType.WEEKLY;
                        selector.animate().setDuration(animationDuration).translationX(btn2loc[0] - btn1loc[0]);
                        break;
                    case 2:
                        type = AnalyticsType.MONTHLY;
                        selector.animate().setDuration(animationDuration).translationX(btn3loc[0] - btn1loc[0]);
                        break;
                }

                if (getResources().getBoolean(R.bool.analytics_color_anim_enabled)) {
                    updateButtonTextColor(newPosition);
                }

                currentPosition = newPosition;

            }

        }
        resetCalendar();
        reloadData();
    }

    private void updateButtonTextColor(int newPosition) {

        if (newPosition == currentPosition)
            return;

        Button buttonFrom = null;
        Button buttonTo = null;

        int fromColor = getColorPrimary();
        int toColor = getResources().getColor(R.color.gray);


        switch (newPosition) {
            case 0:
                buttonTo = button1;
                break;
            case 1:
                buttonTo = button2;
                break;
            case 2:
                buttonTo = button3;
                break;
        }

        switch (currentPosition) {
            case 0:
                buttonFrom = button1;
                break;
            case 1:
                buttonFrom = button2;
                break;
            case 2:
                buttonFrom = button3;
                break;
        }

        int animationDuration = SELECTOR_ANIMATION_DURATION;

        // double the time if animating from end to end
        if (newPosition == 2 && currentPosition == 0 || currentPosition == 2 && newPosition == 0)
            animationDuration *= 2;

        animateButtonTextColor(buttonFrom, fromColor, toColor, animationDuration);
        animateButtonTextColor(buttonTo, toColor, fromColor, animationDuration);

    }


    @Click(R.id.button1)
    public void btn1() {
        updateElements(0, true);
    }

    private void updateInterface(LoadingState state) {
        updateInterface(state, null);
    }

    private void updateInterface(LoadingState state, Analytics analytics) {
        switch (state) {
            case LOADED:

                labelCentered.setVisibility(View.GONE);
                infoRow3.setVisibility(View.VISIBLE);

                if (hasValues(analytics)) {
                    labelCentered.setVisibility(View.GONE);
                } else {
                    labelCentered.setVisibility(View.VISIBLE);
                    labelCentered.setText(textNoRecords);
                }

                indicator.setVisibility(View.GONE);

                break;
            case FAILED:
                labelCentered.setVisibility(View.VISIBLE);
                infoRow3.setVisibility(View.GONE);
                labelCentered.setText(textError);

                indicator.setVisibility(View.GONE);

                break;
            case LOADING:
                labelCentered.setVisibility(View.GONE);
                infoRow3.setVisibility(View.GONE);
                indicator.setVisibility(View.VISIBLE);

                break;
        }


        switch (type) {
            case DAILY:
                chartDaily.setVisibility(View.VISIBLE);
                chartWeekly.setVisibility(View.GONE);
                chartMonthly.setVisibility(View.GONE);
                txtDate.setText(dateFormat.format(cal.getTime()));
                break;
            case WEEKLY:
                chartDaily.setVisibility(View.GONE);
                chartWeekly.setVisibility(View.VISIBLE);
                chartMonthly.setVisibility(View.GONE);

                Calendar calTo = Calendar.getInstance();
                calTo.setTime(cal.getTime());

                calTo.add(Calendar.DAY_OF_MONTH, 6);

                String weekTxt = "";

                if (cal.get(Calendar.YEAR) == calTo.get(Calendar.YEAR)) {

                    if (cal.get(Calendar.MONTH) == calTo.get(Calendar.MONTH)) {

                        // years and months are the same
                        weekTxt = cal.get(Calendar.DAY_OF_MONTH) + "-" + calTo.get(Calendar.DAY_OF_MONTH) + " " + dateFormatMonthYear.format(cal.getTime());

                    } else {

                        // if months are different
                        weekTxt = dateFormatDayMonth.format(cal.getTime()) + " - " + dateFormatDayMonth.format(calTo.getTime()) + " " + cal.get(Calendar.YEAR);

                    }

                } else {

                    // if years are different (can happen on new year's eve)
                    weekTxt = dateFormat.format(cal.getTime()) + " - " + dateFormat.format(calTo.getTime());

                }
                txtDate.setText(weekTxt);

                break;
            case MONTHLY:
                chartDaily.setVisibility(View.GONE);
                chartWeekly.setVisibility(View.GONE);
                chartMonthly.setVisibility(View.VISIBLE);
                txtDate.setText(dateFormatMonthYear.format(cal.getTime()));
                break;
        }

        updateValues(state);
    }

    private void setClearOrLoading() {
        lblSales.setText("-");
        lblAverageSale.setText("-");
        lblNetSales.setText("-");
    }

    private void updateValues(LoadingState state) {

        switch (state) {
            case LOADING:
            case FAILED:
                setClearOrLoading();
                break;
            case LOADED:

                if (numSales > 0) {

                    lblSales.setText(String.format(Locale.US, "%d", numSales));
                    lblAverageSale.setText(Convert.formatCurrencyWithFraction(valueAverage,
                            currency,
                            clientConfig.getFractionDigit()));
                    lblNetSales.setText(Convert.formatCurrencyWithFraction(valueNet,
                            currency,
                            clientConfig.getFractionDigit()));

                } else {
                    setClearOrLoading();
                }

                break;
        }

    }

    String createKey(Date date, AnalyticsType type) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return type.getType() + simpleDateFormat.format(date);
    }

    private void reloadData() {
        Date date = cal.getTime();
        ;
        String key = createKey(date, type);
        Analytics analytics = cache.get(key);
        if (analytics != null) {
            updateValues(analytics);
            updateInterface(LoadingState.LOADED, analytics);
        } else {
            loadAnalytics(date, type);
        }
    }

    private boolean hasValues(Analytics analytics) {
        return analytics != null && analytics.getTransactions() > 0;
    }

    private void updateValues(Analytics analytics) {
        ArrayList<PhosBar> values = new ArrayList<>();
        numSales = analytics.getTransactions();
        valueNet = analytics.getSales();
        valueAverage = analytics.getAverage();
        currency = analytics.getCurrency() != null
                ? analytics.getCurrency()
                : clientConfig.getTerminal().getCurrency();

        if (hasValues(analytics)) {
            infoRow3.setVisibility(View.VISIBLE);
            switch (type) {
                case DAILY:
                    updateValuesWithDailyAnalytics(values, analytics);
                    break;
                case WEEKLY:
                    updateValuesWithWeeklyAnalytics(values, analytics);
                    break;
                case MONTHLY:
                    updateValuesWithMonthlyAnalytics(values, analytics);
                    break;
            }
        } else {
            infoRow3.setVisibility(View.GONE);
        }

        if (type == AnalyticsType.DAILY) {
            showBarChart(chartDaily, MAX_DAILY_COLUMNS, values);
        } else if (type == AnalyticsType.WEEKLY) {
            showBarChart(chartWeekly, MAX_WEEKLY_COLUMNS, values);
        } else {
            showLineChart(values);
        }
    }

    private void updateValuesWithDailyAnalytics(ArrayList<PhosBar> values, Analytics analytics) {

        ArrayList<Double> hours = analytics.getAmounts();

//        ArrayList<Pair<String, Double>> filtered = new ArrayList<>(); // those will be the output values
//
//        int start = -1;
//        int end = -1;
//
//        int cntr = 0;
//
//        for (Double val : hours) {
//
//            if (start == -1 && val != 0) {
//                start = cntr;
//            }
//
//            if (val != 0) {
//                end = cntr;
//            }
//
//            cntr++;
//        }
//
//        // calculate if we need to prepend or append any of the zero values
//        int prependNum = (MAX_DAILY_COLUMNS - (end + 1 - start)) / 2;
//
//        if (end - start < MAX_DAILY_COLUMNS) {
//
//            if (start < prependNum)
//                prependNum = start;
//
//
//            // prepend some zeroes
//            for (int i = 0; i < prependNum; i++)
//                filtered.add(new Pair<>(String.format(Locale.US, "%d", (start - prependNum + i)), 0.0));
//
//        }
//
//        // if too many, we group by two hours
//        boolean tooMany = (end - cntr) > 12;
//
//        if (!tooMany) {
//            for (int i = start; i <= end; i++) {
//                filtered.add(new Pair<>(String.format(Locale.US, "%d", i), hours.get(i)));
//            }
//        } else {
//
//            for (int i = start; i <= (end - 1); i+=2) {
//                filtered.add(new Pair<>(String.format(Locale.US, "%d-%d", i, i + 1),
//                        hours.get(i).floatValue() + hours.get(i+1)));
//
//
//            }
//
//        }
//
//        // append some values at the end
//        int appendNum = MAX_DAILY_COLUMNS - filtered.size();
//
//        if (appendNum > 23 - end) {
//            appendNum = 23 - end;
//        }
//
//        for (int i = 0; i < appendNum; i++)
//            filtered.add(new Pair<>(String.format(Locale.US, "%d", (end + i)), 0.0));
//
//
//        for (Pair<String, Double> lblAndValue : filtered) {
//            values.add(new PhosBar(lblAndValue.first, lblAndValue.second.floatValue()));
//        }

        for (int i = 0; i < hours.size(); i++) {
            values.add(new PhosBar(String.valueOf(i), hours.get(i).floatValue()));
        }
    }

    private void updateValuesWithWeeklyAnalytics(ArrayList<PhosBar> values, Analytics analytics) {
        ArrayList<Double> daysW = analytics.getAmounts();

        if (daysW == null || daysW.size() == 0) {
            daysW = new ArrayList<>();

            for (int i = 0; i < MAX_WEEKLY_COLUMNS; i++) {
                daysW.add(0.0);
            }
        }

        for (int i = 0; i < daysW.size(); i++) {
            Calendar calTmp = (Calendar) cal.clone();
            calTmp.add(Calendar.DATE, i);
            values.add(new PhosBar(stringManager.getDayByNumber(calTmp.get(Calendar.DAY_OF_WEEK)), daysW.get(i).floatValue()));
        }
    }

    private void updateValuesWithMonthlyAnalytics(ArrayList<PhosBar> values, Analytics analytics) {
        ArrayList<Double> daysM = analytics.getAmounts();

        int week = 0;
        int day = 0;
        double totalForWeek = 0;
        int startDay = 1;
        int endDay = Math.min(daysM.size(), startDay + 6);
        for (int i = 0; i < daysM.size(); i++) {


            totalForWeek += daysM.get(i).floatValue();

            day++;
            if (day > 6 && week < 3) {
                values.add(new PhosBar(String.format(Locale.US, "%d-%d", startDay, endDay), (float) totalForWeek));

                day = 0;
                week++;
                totalForWeek = 0;

                startDay = startDay + 7;
                endDay = endDay + 7;
            }
        }

        if (day > 0) {
            if (endDay < daysM.size())
                endDay = daysM.size();

            values.add(new PhosBar(String.format(Locale.US, "%d-%d", startDay, endDay), (float) totalForWeek));
        }
    }

    private void loadAnalytics(Date date, AnalyticsType type) {
        updateInterface(LoadingState.LOADING);

        // Quick fix. Do not send multiple requests when user pumps the Next/Prev day button.
        analyticsRunnable.setDate(date);
        analyticsRunnable.setType(type);
        analyticsRunnable.setTimezone(idProvider.getDefaultTimeZone());
        mainHandler.removeCallbacks(analyticsRunnable);
        mainHandler.postDelayed(analyticsRunnable, 500);
    }

    private ArrayList<PhosBar> getNoValues() {
        ArrayList<PhosBar> values = new ArrayList<>();
        values.add(new PhosBar("6", 0.0f));
        values.add(new PhosBar("8", 0.0f));
        values.add(new PhosBar("10", 0.0f));
        values.add(new PhosBar("12", 0.0f));
        values.add(new PhosBar("14", 0.0f));
        values.add(new PhosBar("16", 0.0f));
        values.add(new PhosBar("18", 0.0f));
        return values;
    }


    @Click(R.id.button2)
    public void btn2() {
        updateElements(1, true);
    }


    @Click(R.id.button3)
    public void btn3() {
        updateElements(2, true);
    }


    @Click(R.id.btnBack)
    public void close() {
        finish();
    }

    private void resetCalendar() {
        cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

        switch (type) {
            case DAILY:
                break;
            case WEEKLY:
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                break;
            case MONTHLY:
                cal.set(Calendar.DAY_OF_MONTH, 1);
                break;
        }

    }

    @Click(R.id.btnPrev)
    public void prev() {

        switch (type) {
            case DAILY:
                cal.add(Calendar.DAY_OF_MONTH, -1);
                break;
            case WEEKLY:
                cal.add(Calendar.DAY_OF_MONTH, -7);
                break;
            case MONTHLY:
                cal.add(Calendar.MONTH, -1);
                break;
        }

        reloadData();
    }

    @Click(R.id.btnNext)
    public void next() {

        switch (type) {
            case DAILY:
                cal.add(Calendar.DAY_OF_MONTH, 1);
                break;
            case WEEKLY:
                cal.add(Calendar.DAY_OF_MONTH, 7);
                break;
            case MONTHLY:
                cal.add(Calendar.MONTH, 1);
                break;
        }

        if (cal.getTimeInMillis() > Calendar.getInstance().getTimeInMillis()) {
            resetCalendar();
        } else {
            reloadData();
        }
    }
}
