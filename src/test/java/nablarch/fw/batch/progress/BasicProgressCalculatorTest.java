package nablarch.fw.batch.progress;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.Test;

import mockit.Expectations;
import mockit.Mocked;

/**
 * {@link BasicProgressCalculator}のテスト。
 */
public class BasicProgressCalculatorTest {

    @Test
    public void 処理対象件数と処理済み件数からTPSと終了予測時間が求められること(
            @Mocked final TpsCalculator mockTpsCalculator,
            @Mocked final EstimatedEndTimeCalculator mockEstimatedEndTimeCalculator) throws Exception {

        final Date expectedEstimatedEndTime = new Date(System.currentTimeMillis() + 1234L);
        new Expectations() {{
            mockTpsCalculator.calculate(anyLong, 10);
            result = 100.0D;
            mockEstimatedEndTimeCalculator.calculate(100L, 10L, 100);
            result = expectedEstimatedEndTime;
        }};

        final BasicProgressCalculator sut = new BasicProgressCalculator(100L);
        final Progress progress = sut.calculate(10);

        assertThat(progress, allOf(
                hasProperty("tps", is(100.0D)),
                hasProperty("estimatedEndTime", is(expectedEstimatedEndTime))
        ));
    }
}