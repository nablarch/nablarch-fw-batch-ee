package nablarch.fw.batch.progress;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.Ignore;
import org.junit.Test;

import mockit.Expectations;
import mockit.Mocked;

/**
 * {@link ProcessedCountBasedProgressCalculator}のテスト。
 */
public class ProcessedCountBasedProgressCalculatorTest {

    @Test
    @Ignore("jacoco と jmockit が競合してエラーになるため")
    public void 処理対象件数と処理済み件数からTPSと終了予測時間が求められること(
            @Mocked final TpsCalculator mockTpsCalculator,
            @Mocked final EstimatedEndTimeCalculator mockEstimatedEndTimeCalculator) throws Exception {

        final Date expectedEstimatedEndTime = new Date(System.currentTimeMillis() + 1234L);
        new Expectations() {{
            mockTpsCalculator.calculate(anyLong, anyLong);
            returns(100.0D, 200.0D, 300.0D);
            mockEstimatedEndTimeCalculator.calculate(100L, 10L, 100);
            result = expectedEstimatedEndTime;
            mockEstimatedEndTimeCalculator.calculate(100L, 20L, 300);
            result = expectedEstimatedEndTime;
        }};

        final ProcessedCountBasedProgressCalculator sut = new ProcessedCountBasedProgressCalculator(100L);
        Progress progress = sut.calculate(10);

        assertThat(progress, allOf(
                hasProperty("tps", is(100.0D)),
                hasProperty("currentTps", is(100.0D)),
                hasProperty("estimatedEndTime", is(expectedEstimatedEndTime)),
                hasProperty("remainingCount", is(90L))
        ));

        progress = sut.calculate(20);
        assertThat(progress, allOf(
                hasProperty("tps", is(200.0D)),
                hasProperty("currentTps", is(300.0D)),
                hasProperty("estimatedEndTime", is(expectedEstimatedEndTime)),
                hasProperty("remainingCount", is(80L))
        ));
    }

    @Test
    public void 処理対数件数が1以上で処理済み件数が0の場合TPSは0で終了予測は不明となること() throws Exception {
        final ProcessedCountBasedProgressCalculator sut = new ProcessedCountBasedProgressCalculator(1L);
        final Progress progress = sut.calculate(0L);

        assertThat(progress, allOf(
                hasProperty("tps", is(0.0)),
                hasProperty("currentTps", is(0.0)),
                hasProperty("estimatedEndTime", is(nullValue())),
                hasProperty("remainingCount", is(1L))
        ));
    }
}