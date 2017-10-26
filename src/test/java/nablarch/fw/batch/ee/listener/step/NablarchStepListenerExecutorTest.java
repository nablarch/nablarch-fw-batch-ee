package nablarch.fw.batch.ee.listener.step;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;

import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;

import org.junit.Before;
import org.junit.Test;

import mockit.Deencapsulation;
import mockit.Mocked;
import mockit.Expectations;
import mockit.VerificationsInOrder;

/**
 * {@link NablarchStepListenerExecutor}のテスト。
 */
public class NablarchStepListenerExecutorTest {

    @Mocked
    StepContext mockStepContext;

    @Mocked
    JobContext mockJobContext;

    @Mocked
    NablarchStepListener mockListener1;

    @Mocked
    NablarchStepListener mockListener2;

    /** テスト対象 */
    NablarchStepListenerExecutor sut = new NablarchStepListenerExecutor();

    @Before
    public void setUp() throws Exception {

        new Expectations() {{
            mockJobContext.getJobName();
            result = "testJob";
            maxTimes = 1;
            minTimes = 0;

            mockStepContext.getStepName();
            result = "testStep";
            maxTimes = 1;
            minTimes = 0;
        }};

        Deencapsulation.setField(sut, "jobContext", mockJobContext);
        Deencapsulation.setField(sut, "stepContext", mockStepContext);
    }

    /**
     * ステップが正常に実行され、{@link NablarchStepListener#beforeStep(NablarchListenerContext)}、
     * {@link NablarchStepListener#afterStep(NablarchListenerContext)} が順次実行されるケース。
     */
    @Test
    public void testExecute() throws Exception {

        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                List<NablarchStepListener> listeners = new ArrayList<NablarchStepListener>();
                listeners.add(mockListener1);
                listeners.add(mockListener2);

                Map<String, Object> objects = new HashMap<String, Object>();
                objects.put("stepListeners", listeners);
                return objects;
            }
        });

        sut.beforeStep();
        sut.afterStep();

        new VerificationsInOrder() {{
            // 設定順に実行されていること
            mockListener1.beforeStep(new NablarchListenerContext(
                    withAny(mockJobContext), withAny(mockStepContext)));
            times = 1;
            mockListener2.beforeStep(new NablarchListenerContext(
                    withAny(mockJobContext), withAny(mockStepContext)));
            times = 1;

            // beforeの実行順とは逆順に実行されていること
            mockListener2.afterStep(new NablarchListenerContext(
                    withAny(mockJobContext), withAny(mockStepContext)));
            times = 1;
            mockListener1.afterStep(new NablarchListenerContext(
                    withAny(mockJobContext), withAny(mockStepContext)));
            times = 1;
        }};
    }

    /**
     * {@link NablarchStepListener#beforeStep(NablarchListenerContext)}が実行されず、
     * {@link NablarchStepListener#afterStep(NablarchListenerContext)} のみが実行されるケース。
     */
    @Test
    public void testExecute_without_before() {
        try {
            sut.afterStep();
        } catch (Exception e) {
            fail("例外は発生しない");
        }
    }

    /**
     * ListenerでJobContextやStepContextを使えることを確認するテスト。
     * @throws Exception
     */
    @Test
    public void testUseContext() throws Exception {
        final TestStepListener stepListener = new TestStepListener();
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                List<NablarchStepListener> listeners = new ArrayList<NablarchStepListener>();
                listeners.add(stepListener);
                return Collections.<String, Object>singletonMap("stepListeners", listeners);
            }
        });

        sut.beforeStep();
        sut.afterStep();

        assertThat(stepListener.before, is(true));
        assertThat(stepListener.after, is(true));
    }
    
    private static class TestStepListener extends AbstractNablarchStepListener {

        private boolean before;

        private boolean after;

        @Override
        public void beforeStep(final NablarchListenerContext context) {
            before = true;

            final JobContext jobContext = context.getJobContext();
            final StepContext stepContext = context.getStepContext();

            if (!jobContext.getJobName().equals("testJob")) {
                throw new IllegalArgumentException("ジョブ名が想定とちがいます。");
            }

            if (!stepContext.getStepName().equals("testStep")) {
                throw new IllegalArgumentException("ステップ名画想定とちがいます。");
            }
        }

        @Override
        public void afterStep(final NablarchListenerContext context) {
            after = true;
            beforeStep(context);
        }
    }
}