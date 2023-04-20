package nablarch.fw.batch.progress;

import org.junit.Test;
import org.mockito.MockedConstruction;

import java.util.Date;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

/**
 * {@link ProcessedCountBasedProgressCalculator}のテスト。
 */
public class ProcessedCountBasedProgressCalculatorTest {

    @Test
    public void 処理対象件数と処理済み件数からTPSと終了予測時間が求められること() throws Exception {
        final Date expectedEstimatedEndTime = new Date(System.currentTimeMillis() + 1234L);
        
        try (
            final MockedConstruction<TpsCalculator> tpsCalculator = mockConstruction(TpsCalculator.class, (mock, context) -> {
                when(mock.calculate(anyLong(), anyLong())).thenReturn(100.0D, 200.0D, 300.0D);
            });
            final MockedConstruction<EstimatedEndTimeCalculator> estimatedEndTimeCalculator = mockConstruction(EstimatedEndTimeCalculator.class, (mock, context) -> {
                when(mock.calculate(100L, 10L, 100)).thenReturn(expectedEstimatedEndTime);
                when(mock.calculate(100L, 20L, 300)).thenReturn(expectedEstimatedEndTime);
            });
        ) {
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