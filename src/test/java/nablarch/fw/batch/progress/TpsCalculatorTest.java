package nablarch.fw.batch.progress;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.hamcrest.number.IsCloseTo;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * {@link TpsCalculator}のテスト。
 */
public class TpsCalculatorTest {

    private final TpsCalculator sut = new TpsCalculator();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void 処理開始時間と処理済み件数からTPSがもとめられること() throws Exception {
        final long startTime = System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(100);
        final double actual = sut.calculate(startTime, 100);
        assertThat(actual, IsCloseTo.closeTo(1000, 0.5));
    }

    @Test
    public void TPSが1以下の場合でもTPSが求められること() throws Exception {
        final long startTime = System.nanoTime() - TimeUnit.SECONDS.toNanos(2);
        final double actual = sut.calculate(startTime, 1);
        assertThat(actual, IsCloseTo.closeTo(0.5, 0.1));
    }

    @Test
    public void 処理開始時間が未来の場合は例外が送出されること() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("start time is invalid. start time must set past time.");
        final long startTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(1);
        sut.calculate(startTime, 1);
    }

    @Test
    public void 処理件数が0の場合例外が送出されること() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("processed count is invalid. processing count must set 1 or more.");
        final long startTime = System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(100);
        sut.calculate(startTime, 0);
    }
    
    @Test
    public void 処理件数が0より小さい場合例外が送出されること() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("processed count is invalid. processing count must set 1 or more.");
        final long startTime = System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(100);
        sut.calculate(startTime, -1);
    }
}