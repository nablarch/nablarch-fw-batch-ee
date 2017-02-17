package nablarch.fw.batch.progress;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * {@link EstimatedEndTimeCalculator}のテスト。
 */
public class EstimatedEndTimeCalculatorTest {

    private final EstimatedEndTimeCalculator sut = new EstimatedEndTimeCalculator();
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void 推定終了時間が求められること() throws Exception {
        // 1000TPSで残り1000件なので1秒後が終了予測時間になる
        final long expected = System.currentTimeMillis() + 1000;
        final Date actual = sut.calculate(1100, 100, 1000);

        assertThat(actual.getTime(), allOf(
                greaterThanOrEqualTo(expected),
                lessThan(expected + 50)             // 誤差があるので50ミリ秒ぐらいまで許容する
        ));
    }

    @Test
    public void 小さいTPSでも推定終了時間が求められること() throws Exception {
        // 0.1TPSなので、残り3件処理するのに30秒かかる
        final long expected = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
        final Date actual = sut.calculate(33, 30, 0.1);

        assertThat(actual.getTime(), allOf(
                greaterThanOrEqualTo(expected),
                lessThan(expected + 50)             // 誤差があるので50ミリ秒ぐらいまで許容する
        ));
    }

    @Test
    public void 残り件数が0の場合でも推定終了時間が求められること() throws Exception {
        // 残り件数0なので終了予測時間は現在の次官となる
        final long expected = System.currentTimeMillis();
        final Date actual = sut.calculate(33, 33, 0.1);

        assertThat(actual.getTime(), allOf(
                greaterThanOrEqualTo(expected),
                lessThan(expected + 50)             // 誤差があるので50ミリ秒ぐらいまで許容する
        ));
    }

    @Test
    public void 処理対象件数に0以下を指定した場合例外が送出されること() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("input count is invalid. input count must set 1 or more.");
        sut.calculate(0, 1, 10);
    }

    @Test
    public void 処理済み件数に0以下を指定した場合例外が送出されること() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("processed count is invalid. processed count must set 1 or more.");
        sut.calculate(1, 0, 10);
    }

    @Test
    public void 処理対象件数より処理済み件数が大きい場合例外が送出されること() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("processed count is invalid. processed count must set less than input count.");
        sut.calculate(1, 2, 10);
    }

    @Test
    public void TPSに0以下の値を設定した場合例外が送出されること() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("tps is invalid. tps must set 1 or more.");
        sut.calculate(1, 1, 0);
    }
}