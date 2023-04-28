package nablarch.fw.batch.ee.listener.job;

import jakarta.batch.runtime.context.JobContext;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.batch.ee.initializer.LogInitializer;
import nablarch.fw.batch.ee.initializer.RepositoryInitializer;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.test.support.reflection.ReflectionUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

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
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * {@link NablarchJobListenerExecutor}のテスト。
 */
public class NablarchJobListenerExecutorTest {

    JobContext mockJobContext = mock(JobContext.class);

    NablarchJobListener mockListener1 = mock(NablarchJobListener.class);

    NablarchJobListener mockListener2 = mock(NablarchJobListener.class);

    LogInitializer mockLogInitializer = mock(LogInitializer.class);

    RepositoryInitializer mockRepositoryInitializer = mock(RepositoryInitializer.class);

    /** テスト対象 */
    NablarchJobListenerExecutor sut = new NablarchJobListenerExecutor();


    @Before
    public void setUp() throws Exception {
        when(mockJobContext.getJobName()).thenReturn("testJob");

        ReflectionUtil.setFieldValue(sut, "jobContext", mockJobContext);
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

        try (
            final MockedStatic<LogInitializer> logInitializer = mockStatic(LogInitializer.class);
            final MockedStatic<RepositoryInitializer> repositoryInitializer = mockStatic(RepositoryInitializer.class);
        ) {
            sut.beforeJob();
            sut.afterJob();
            
            logInitializer.verify(LogInitializer::initialize);
            repositoryInitializer.verify(() -> RepositoryInitializer.initialize("batch-boot.xml"));

            final InOrder inOrder = inOrder(mockListener1, mockListener2);

            // 設定順に実行されていること
            inOrder.verify(mockListener1).beforeJob(any(NablarchListenerContext.class));
            inOrder.verify(mockListener2).beforeJob(any(NablarchListenerContext.class));

            // beforeの実行順とは逆順に実行されていること
            inOrder.verify(mockListener2).afterJob(any(NablarchListenerContext.class));
            inOrder.verify(mockListener1).afterJob(any(NablarchListenerContext.class));
        }
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

