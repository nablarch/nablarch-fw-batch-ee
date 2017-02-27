package nablarch.fw.batch.ee.progress;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.Metric;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;

import nablarch.fw.batch.ee.integration.InMemoryAppender;
import nablarch.fw.batch.progress.ProcessedCountBasedProgressCalculator;
import nablarch.fw.batch.progress.Progress;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import mockit.Mocked;
import mockit.NonStrictExpectations;

/**
 * {@link BasicProgressManager}のテスト。
 */
public class BasicProgressManagerTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mocked
    private JobContext mockJobContext;
    
    @Mocked
    private StepContext mockStepContext;

    @Before
    public void setUp() throws Exception {
        InMemoryAppender.clear();

        new NonStrictExpectations() {{
            mockJobContext.getJobName();
            result = "test-job";
            mockStepContext.getStepName();
            result = "test-step";
        }};
    }

    @Test
    public void 進捗状況がログ出力されること(@Mocked final ProcessedCountBasedProgressCalculator progressCalculator) throws Exception {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSS");
        final Date estimatedDate1 = new Date();
        TimeUnit.MILLISECONDS.sleep(100);
        final Date estimatedDate2 = new Date();
        
        new NonStrictExpectations() {{
            mockStepContext.getMetrics();
            result = new Metric[] {
                    new Metric() {
                        @Override
                        public MetricType getType() {
                            return MetricType.READ_COUNT;
                        }

                        @Override
                        public long getValue() {
                            return 100;
                        }
                    }
            };

            progressCalculator.calculate(100);
            returns(new Progress(1.23, estimatedDate1, 12245), new Progress(1.21, estimatedDate2, 12205));
        }};
        final ProgressManager sut = new BasicProgressManager(mockJobContext, mockStepContext);
        sut.setInputCount(12345);
        sut.outputProgressInfo();
        sut.outputProgressInfo();

        final List<String> messages = InMemoryAppender.getLogMessages("PROGRESS");
        assertThat(messages, contains(
                containsString("job name: [test-job] step name: [test-step] input count: [12345]"),
                containsString("job name: [test-job] step name: [test-step] tps: [1.23] estimated end time: ["
                        + format.format(estimatedDate1) + "] "
                        + "remaining count: [12245]"),
                containsString("job name: [test-job] step name: [test-step] tps: [1.21] estimated end time: ["
                        + format.format(estimatedDate2) + "] "
                        + "remaining count: [12205]")
                )
        );
    }

    @Test
    public void 入力件数を明示的に指定して進捗ログが出力できること(@Mocked final ProcessedCountBasedProgressCalculator progressCalculator) throws Exception {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSS");
        final Date estimatedDate1 = new Date();
        TimeUnit.MILLISECONDS.sleep(100);
        final Date estimatedDate2 = new Date();

        new NonStrictExpectations() {{
            progressCalculator.calculate(1000);
            result = new Progress(1.23, estimatedDate1, 9000);
            progressCalculator.calculate(2000);
            result = new Progress(1.22, estimatedDate2, 7000);
        }};

        final ProgressManager sut = new BasicProgressManager(mockJobContext, mockStepContext);
        sut.setInputCount(10000);
        sut.outputProgressInfo(1000);
        sut.outputProgressInfo(2000);

        final List<String> messages = InMemoryAppender.getLogMessages("PROGRESS");
        assertThat(messages, contains(
                containsString("job name: [test-job] step name: [test-step] input count: [10000]"),
                containsString("job name: [test-job] step name: [test-step] tps: [1.23] estimated end time: ["
                        + format.format(estimatedDate1) + "] "
                        + "remaining count: [9000]"),
                containsString("job name: [test-job] step name: [test-step] tps: [1.22] estimated end time: ["
                        + format.format(estimatedDate2) + "] "
                        + "remaining count: [7000]")
                )
        );
    }

    @Test
    public void 入力件数が0の場合で進捗ログを出力しようとしても何も行われないこと() throws Exception {
        final ProgressManager sut = new BasicProgressManager(mockJobContext, mockStepContext);
        sut.setInputCount(0L);
        sut.outputProgressInfo(0L);
        sut.outputProgressInfo(0L);

        final List<String> messages = InMemoryAppender.getLogMessages("PROGRESS");
        assertThat("入力件数のログのみで進捗状況のログは出力されないこと", messages, contains(
                containsString("job name: [test-job] step name: [test-step] input count: [0]")
        ));
    }

    @Test
    public void 入力件数が0以上で処理済み件数が0の場合終了予測時間が求められないこと() throws Exception {
        final ProgressManager sut = new BasicProgressManager(mockJobContext, mockStepContext);
        sut.setInputCount(1000L);
        sut.outputProgressInfo(0L);

        final List<String> messages = InMemoryAppender.getLogMessages("PROGRESS");
        assertThat("終了予測が求められないこと", messages, contains(
                containsString("job name: [test-job] step name: [test-step] input count: [1000]"),
                containsString("tps: [0.00] estimated end time: [unknown]")
        ));
    }
    
    @Test
    public void 入力件数が0より小さい場合はエラーとなること() throws Exception {
        final ProgressManager sut = new BasicProgressManager(mockJobContext, mockStepContext);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("invalid input count. must set 0 or more." 
                + " job name: [test-job] step name: [test-step] input count: [-1]");
        sut.setInputCount(-1L);
    }

    @Test
    public void 入力件数が0の場合で処理件数が0以外の場合はエラーとなること() throws Exception {
        final ProgressManager sut = new BasicProgressManager(mockJobContext, mockStepContext);
        sut.setInputCount(0L);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("invalid processed count. processed count must set 0 because input count is 0." 
                + " job name: [test-job] step name: [test-step] input count: [0] processed count: [123]");
        
        sut.outputProgressInfo(123L);
    }

    @Test
    public void 入力件数を設定せずに進捗ログを出力しようとした場合はエラーとなること() throws Exception {

        new NonStrictExpectations() {{
            mockStepContext.getMetrics();
            result = new Metric[] {
                    new Metric() {
                        @Override
                        public MetricType getType() {
                            return MetricType.READ_COUNT;
                        }
                        @Override
                        public long getValue() {
                            return 100;
                        }
                    }
            };
        }};

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("input count is not set. must set input count. job name: [test-job] step name: [test-step]");
        final ProgressManager sut = new BasicProgressManager(mockJobContext, mockStepContext);
        sut.outputProgressInfo();
    }

    @Test
    public void StepContextかリード済み件数が取れない場合はエラーとなること() throws Exception {
        new NonStrictExpectations() {{
            mockStepContext.getMetrics();
            result = new Metric[0];
        }};
        expectedException.expect(BatchRuntimeException.class);
        expectedException.expectMessage("failed to StepContext#getMetrics");
        
        final ProgressManager sut = new BasicProgressManager(mockJobContext, mockStepContext);
        sut.setInputCount(1);
        sut.outputProgressInfo();
    }

}