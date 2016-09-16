package nablarch.fw.batch.ee.listener.chunk;

import mockit.Deencapsulation;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.VerificationsInOrder;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;

import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import org.junit.Before;
import org.junit.Test;

import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;

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
        new NonStrictExpectations() {{
            mockJobContext.getJobName();
            result = "testJob";
            mockStepContext.getStepName();
            result = "testStep";
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
}