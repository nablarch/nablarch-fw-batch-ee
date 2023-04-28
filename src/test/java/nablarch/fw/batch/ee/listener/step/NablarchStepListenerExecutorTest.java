package nablarch.fw.batch.ee.listener.step;

import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.test.support.reflection.ReflectionUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link NablarchStepListenerExecutor}のテスト。
 */
public class NablarchStepListenerExecutorTest {

    StepContext mockStepContext = mock(StepContext.class);

    JobContext mockJobContext = mock(JobContext.class);

    NablarchStepListener mockListener1 = mock(NablarchStepListener.class);

    NablarchStepListener mockListener2 = mock(NablarchStepListener.class);

    /** テスト対象 */
    NablarchStepListenerExecutor sut = new NablarchStepListenerExecutor();

    @Before
    public void setUp() throws Exception {
        when(mockJobContext.getJobName()).thenReturn("testJob");
        when(mockStepContext.getStepName()).thenReturn("testStep");

        ReflectionUtil.setFieldValue(sut, "jobContext", mockJobContext);
        ReflectionUtil.setFieldValue(sut, "stepContext", mockStepContext);
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

        final InOrder inOrder = inOrder(mockListener1, mockListener2);
        // 設定順に実行されていること
        inOrder.verify(mockListener1).beforeStep(any(NablarchListenerContext.class));
        inOrder.verify(mockListener2).beforeStep(any(NablarchListenerContext.class));

        // beforeの実行順とは逆順に実行されていること
        inOrder.verify(mockListener2).afterStep(any(NablarchListenerContext.class));
        inOrder.verify(mockListener1).afterStep(any(NablarchListenerContext.class));
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