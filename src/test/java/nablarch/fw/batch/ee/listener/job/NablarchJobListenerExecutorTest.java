package nablarch.fw.batch.ee.listener.job;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.batch.runtime.context.JobContext;

import mockit.Expectations;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.batch.ee.initializer.LogInitializer;
import nablarch.fw.batch.ee.initializer.RepositoryInitializer;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;

import org.junit.Before;
import org.junit.Test;

import mockit.Deencapsulation;
import mockit.Mocked;
import mockit.VerificationsInOrder;

/**
 * {@link NablarchJobListenerExecutor}のテスト。
 */
public class NablarchJobListenerExecutorTest {

    @Mocked
    JobContext mockJobContext;

    @Mocked
    NablarchJobListener mockListener1;

    @Mocked
    NablarchJobListener mockListener2;

    @Mocked
    LogInitializer mockLogInitializer;

    @Mocked
    RepositoryInitializer mockRepositoryInitializer;

    /** テスト対象 */
    NablarchJobListenerExecutor sut = new NablarchJobListenerExecutor();


    @Before
    public void setUp() throws Exception {
        new Expectations() {{
            mockJobContext.getJobName();
            maxTimes = 1;
            minTimes = 0;
            result = "testJob";
        }};

        Deencapsulation.setField(sut, "jobContext", mockJobContext);
    }

    /**
     * ジョブが正常に実行され、{@link NablarchJobListener#beforeJob(NablarchListenerContext)}
     * {@link NablarchJobListener#afterJob(NablarchListenerContext)} が順次実行されるケース。
     */
    @Test
    public void testExecute() throws Exception {

        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                List<NablarchJobListener> listeners = new ArrayList<NablarchJobListener>();
                listeners.add(mockListener1);
                listeners.add(mockListener2);

                Map<String, Object> objects = new HashMap<String, Object>();
                objects.put("jobListeners", listeners);
                return objects;
            }
        });

        sut.beforeJob();
        sut.afterJob();

        new VerificationsInOrder() {{
            LogInitializer.initialize();
            times = 1;
            RepositoryInitializer.initialize("batch-boot.xml");
            times = 1;

            // 設定順に実行されていること
            mockListener1.beforeJob(new NablarchListenerContext(withAny(mockJobContext), null));
            times = 1;
            mockListener2.beforeJob(new NablarchListenerContext(withAny(mockJobContext), null));
            times = 1;

            // beforeの実行順とは逆順に実行されていること
            mockListener2.afterJob(new NablarchListenerContext(withAny(mockJobContext), null));
            times = 1;
            mockListener1.afterJob(new NablarchListenerContext(withAny(mockJobContext), null));
            times = 1;
        }};
    }

    /**
     * {@link NablarchJobListener#beforeJob(NablarchListenerContext)}が実行されず、
     * {@link NablarchJobListener#afterJob(NablarchListenerContext)} のみが実行されるケース。
     */
    @Test
    public void testExecute_without_before() {
        try {
            sut.afterJob();
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
        final TestJobListener jobListener = new TestJobListener();
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                List<NablarchJobListener> listeners = new ArrayList<NablarchJobListener>();
                listeners.add(jobListener);
                return Collections.<String, Object>singletonMap("jobListeners", listeners);
            }
        });

        sut.beforeJob();
        sut.afterJob();

        assertThat(jobListener.before, is(true));
        assertThat(jobListener.after, is(true));
        
    }
    
    private static class TestJobListener extends AbstractNablarchJobListener {

        private boolean after;

        private boolean before;

        @Override
        public void beforeJob(final NablarchListenerContext context) {
            before = true;
            final JobContext jobContext = context.getJobContext();
            if (!jobContext.getJobName()
                           .equals("testJob")) {
                throw new IllegalArgumentException("ジョブ名が想定と違います。");
            }

            if (context.getStepContext() != null) {
                throw new IllegalArgumentException("step contextは取れないはず");
            }
        }

        @Override
        public void afterJob(final NablarchListenerContext context) {
            after = true;
            beforeJob(context);
        }
    }
}

