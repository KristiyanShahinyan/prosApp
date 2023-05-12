package digital.paynetics.phos.classes;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import digital.paynetics.phos.logging.LoggerFactory;
import digital.paynetics.phos.sdk.BuildConfig;
import digital.paynetics.phos.sdk.PhosLogger;


public class CrashlyticsAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private PatternLayoutEncoder encoder = null;
    private PatternLayoutEncoder tagEncoder = null;

    public CrashlyticsAppender() {
    }

    @Override
    public void start() {
        if ((this.encoder == null) || (this.encoder.getLayout() == null)) {
            addError("No layout set for the appender named [" + name + "].");
            return;
        }

        // tag encoder is optional but needs a layout
        if (this.tagEncoder != null) {
            final Layout<?> layout = this.tagEncoder.getLayout();

            if (layout == null) {
                addError("No tag layout set for the appender named [" + name + "].");
                return;
            }

            // prevent stack traces from showing up in the tag
            // (which could lead to very confusing error messages)
            if (layout instanceof PatternLayout) {
                String pattern = this.tagEncoder.getPattern();
                if (!pattern.contains("%nopex")) {
                    this.tagEncoder.stop();
                    this.tagEncoder.setPattern(pattern + "%nopex");
                    this.tagEncoder.start();
                }

                PatternLayout tagLayout = (PatternLayout) layout;
                tagLayout.setPostCompileProcessor(null);
            }
        }

        super.start();
    }

    @Override
    public void append(ILoggingEvent event) {
        if (!isStarted()) {
            return;
        }

        if (!BuildConfig.SDK_LOGGING_KERNEL_ENABLED) {
            return;
        }

        if (event.getLevel() == Level.OFF) {
            return;
        }

        String tag = getTag(event);

        PhosLogger logger = LoggerFactory.getLogger();
        logger.d(tag, this.encoder.getLayout().doLayout(event));
    }

    /**
     * Gets the pattern-layout encoder for this appender's <i>logcat</i> message
     *
     * @return the pattern-layout encoder
     */
    public PatternLayoutEncoder getEncoder() {
        return this.encoder;
    }

    /**
     * Sets the pattern-layout encoder for this appender's <i>logcat</i> message
     *
     * @param encoder the pattern-layout encoder
     */
    public void setEncoder(PatternLayoutEncoder encoder) {
        this.encoder = encoder;
    }

    /**
     * Gets the pattern-layout encoder for this appender's <i>logcat</i> tag
     *
     * @return the pattern encoder
     */
    public PatternLayoutEncoder getTagEncoder() {
        return this.tagEncoder;
    }

    /**
     * Sets the pattern-layout encoder for this appender's <i>logcat</i> tag
     * <p>
     * The expanded text of the pattern must be less than 23 characters as
     * limited by Android. Layouts that exceed this limit are truncated,
     * and a star is appended to the tag to indicate this.
     * <p>
     * The <code>tagEncoder</code> result is limited to 22 characters plus a star to
     * indicate truncation (<code>%logger{0}</code> has no length limit in logback,
     * but <code>LogcatAppender</code> limits the length internally). For example,
     * if the <code>tagEncoder</code> evaluated to <code>foo.foo.foo.foo.foo.bar.Test</code>,
     * the tag seen in Logcat would be: <code>foo.foo.foo.foo.foo.ba*</code>.
     * Note that <code>%logger{23}</code> yields more useful results
     * (in this case: <code>f.f.foo.foo.bar.Test</code>).
     *
     * @param encoder the pattern-layout encoder; specify {@code null} to
     *                automatically use the logger's name as the tag
     */
    public void setTagEncoder(PatternLayoutEncoder encoder) {
        this.tagEncoder = encoder;
    }

    /**
     * Gets the logcat tag string of a logging event
     *
     * @param event logging event to evaluate
     * @return the tag string, truncated if max length exceeded
     */
    protected String getTag(ILoggingEvent event) {
        return (this.tagEncoder != null) ? this.tagEncoder.getLayout().doLayout(event) : event.getLoggerName();
    }
}
