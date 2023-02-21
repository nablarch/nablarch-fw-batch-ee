package nablarch.fw.batch.ee.listener.chunk;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;

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
 * {@link NablarchItemWriteListenerExecutor}のテスト。
 */
public class NablarchItemWriteListenerExecutorTest {

    @Mocked
    StepContext mockStepContext;

    @Mocked
    JobContext mockJobContext;

    @Mocked
    NablarchItemWriteListener mockListener1;

    @Mocked
    NablarchItemWriteListener mockListener2;

    /** テスト対象 */
    NablarchItemWriteListenerExecutor sut = new NablarchItemWriteListenerExecutor();

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
     * 書き込み処理が正常に実行され、{@link NablarchItemWriteListener#beforeWrite(NablarchListenerContext, List)}、
     * {@link NablarchItemWriteListener#afterWrite(NablarchListenerContext, List)}が順次実行されるケース。
     */
    @Test
    public void testExecute() throws Exception {

        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                List<NablarchItemWriteListener> listeners = new ArrayList<NablarchItemWriteListener>();
                listeners.add(mockListener1);
                listeners.add(mockListener2);

                Map<String, Object> objects = new HashMap<String, Object>();
                objects.put("itemWriteListeners", listeners);
                return objects;
            }
        });

        final List<Object> items = new ArrayList<Object>();

        sut.beforeWrite(items);
        sut.afterWrite(items);

        new VerificationsInOrder() {{
            // 設定順に実行されていること
            mockListener1.beforeWrite(new NablarchListenerContext(
                    withAny(mockJobContext), withAny(mockStepContext)), items);
            times = 1;
            mockListener2.beforeWrite(new NablarchListenerContext(
                    withAny(mockJobContext), withAny(mockStepContext)), items);
            times = 1;

            // beforeの実行順とは逆順に実行されていること
            mockListener2.afterWrite(new NablarchListenerContext(
                    withAny(mockJobContext), withAny(mockStepContext)), items);
            times = 1;
            mockListener1.afterWrite(new NablarchListenerContext(
                    withAny(mockJobContext), withAny(mockStepContext)), items);
            times = 1;
        }};
    }

    /**
     * 書き込み処理で例外が発生し、{@link NablarchItemWriteListener#beforeWrite(NablarchListenerContext, List)}、
     * {@link NablarchItemWriteListener#afterWrite(NablarchListenerContext, List)}が順次実行されるケース。
     */
    @Test
    public void testError() throws Exception {

        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                List<NablarchItemWriteListener> listeners = new ArrayList<NablarchItemWriteListener>();
                listeners.add(mockListener1);
                listeners.add(mockListener2);

                Map<String, Object> objects = new HashMap<String, Object>();
                objects.put("itemWriteListeners", listeners);
                return objects;
            }
        });

        final List<Object> items = new ArrayList<Object>();
        final Exception ex = new IllegalStateException();

        sut.beforeWrite(items);
        sut.onWriteError(items, ex);

        new VerificationsInOrder() {{
            // 設定順に実行されていること
            mockListener1.beforeWrite(new NablarchListenerContext(
                    withAny(mockJobContext), withAny(mockStepContext)), items);
            times = 1;
            mockListener2.beforeWrite(new NablarchListenerContext(
                    withAny(mockJobContext), withAny(mockStepContext)), items);
            times = 1;

            // beforeの実行順とは逆順に実行されていること
            mockListener2.onWriteError(new NablarchListenerContext(
                    withAny(mockJobContext), withAny(mockStepContext)), items, ex);
            times = 1;
            mockListener1.onWriteError(new NablarchListenerContext(
                    withAny(mockJobContext), withAny(mockStepContext)), items, ex);
            times = 1;
        }};
    }

    /**
     * {@link NablarchItemWriteListener#beforeWrite(NablarchListenerContext, List)}が実行されず、
     * {@link NablarchItemWriteListener#afterWrite(NablarchListenerContext, List)}のみが実行されるケース。
     */
    @Test
    public void testExecute_without_before() {
        try {
            sut.afterWrite(new ArrayList<Object>());
        } catch (Exception e) {
            fail("例外は発生しない");
        }
    }

    /**
     * {@link NablarchItemWriteListener#beforeWrite(NablarchListenerContext, List)}が実行されず、
     * {@link NablarchItemWriteListener#onWriteError(NablarchListenerContext, List, Exception)}のみが実行されるケース。
     */
    @Test
    public void testError_without_before() {
        try {
            sut.onWriteError(new ArrayList<Object>(), new IllegalStateException());
        } catch (Exception e) {
            fail("例外は発生しない");
        }
    }

    /**
     * 呼び出されるリスナーで、JobContext及びStepContextが利用できることを確認するテスト。
     */
    @Test
    public void testUseContext() throws Exception {
        final TestItemWriterListener testListener = new TestItemWriterListener();
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                List<NablarchItemWriteListener> listeners = new ArrayList<NablarchItemWriteListener>();
                listeners.add(testListener);
                Map<String, Object> objects = new HashMap<String, Object>();
                objects.put("itemWriteListeners", listeners);
                return objects;
            }
        });

        final List<Object> items = new ArrayList<Object>();

        sut.beforeWrite(items);
        sut.afterWrite(items);
        sut.onWriteError(items, new IllegalStateException());

        assertThat(testListener.before, is(true));
        assertThat(testListener.after, is(true));
        assertThat(testListener.error, is(true));
    }
    
    private static class TestItemWriterListener extends AbstractNablarchItemWriteListener {
        
        boolean before;
        boolean after;
        boolean error;

        @Override
        public void beforeWrite(final NablarchListenerContext context, final List<Object> items) {
            before = true;
            final JobContext jobContext = context.getJobContext();
            final StepContext stepContext = context.getStepContext();
            if (!jobContext.getJobName()
                          .equals("testJob")) {
                throw new IllegalArgumentException("ジョブ名がおかしいです");
            }
            if (!stepContext.getStepName()
                           .equals("testStep")) {
                throw new IllegalArgumentException("ステップ名がおかしいです");
            }
        }

        @Override
        public void afterWrite(final NablarchListenerContext context, final List<Object> items) {
            after = true;
            beforeWrite(context, items);
        }

        @Override
        public void onWriteError(final NablarchListenerContext context, final List<Object> items, final Exception ex) {
            error = true;
            beforeWrite(context, items);
        }
    }
}