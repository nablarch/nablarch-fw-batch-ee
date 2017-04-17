package nablarch.fw.batch.ee.listener;

import mockit.Deencapsulation;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.batch.ee.integration.InMemoryAppender;
import nablarch.fw.batch.ee.listener.NablarchListenerExecutor.Runner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link NablarchListenerExecutor}のテストクラス。
 */
public class NablarchListenerExecutorTest {

    /** テスト対象 */
    NablarchListenerExecutor<String> sut;

    List<String> listenerList = new ArrayList<String>();

    @Mocked
    JobContext jobContext;

    @Mocked
    StepContext stepContext;

    @Before
    public void setUp() throws Exception {
        new NonStrictExpectations() {{
            jobContext.getJobName();
            result = "testJob";
            stepContext.getStepName();
            result = "testStep";
        }};

        sut = new NablarchListenerExecutor<String>("testListeners", jobContext);
        listenerList.clear();
        SystemRepository.clear();
        InMemoryAppender.clear();
    }

    @After
    public void tearDown() throws Exception {
        SystemRepository.clear();
    }

    /**
     * {@link NablarchListenerExecutor#executeBefore(Runner)}のテストケース<br />
     * リポジトリにデフォルト名で登録されているリスナーリストの順番でリスナーが順次実行されていくこと。
     */
    @Test
    public void testExecuteBefore_default() {
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                List<String> listeners = new ArrayList<String>();
                listeners.add("testListener1");
                listeners.add("testListener2");

                Map<String, Object> objects = new HashMap<String, Object>();
                objects.put("testListeners", listeners);
                return objects;
            }
        });

        sut.executeBefore(new Runner<String>() {
            @Override
            public void run(String listener, NablarchListenerContext context) {
                listenerList.add("before:" + listener);
            }
        });

        // デフォルトで定義されたリスナーリストが設定された順番に実行されること
        assertThat(listenerList.size(), is(2));
        assertThat(listenerList.get(0), is("before:testListener1"));
        assertThat(listenerList.get(1), is("before:testListener2"));

        // スタックに積まれていること
        LinkedList<String> stack = Deencapsulation.getField(sut, "executedListenerStack");
        assertThat(stack.size(), is(2));
        assertThat(stack.pop(), is("testListener2"));
        assertThat(stack.pop(), is("testListener1"));
    }

    /**
     * {@link NablarchListenerExecutor#executeBefore(NablarchListenerExecutor.Runner)}のテストケース<br />
     * リポジトリに個別に登録されているリスナーリストの順番でリスナーが順次実行されていくこと。
     */
    @Test
    public void testExecuteBefore_custom() {
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                List<String> defaultListeners = new ArrayList<String>();
                defaultListeners.add("testListener1");
                defaultListeners.add("testListener2");

                List<String> customListeners = new ArrayList<String>();
                customListeners.add("testListener3");
                customListeners.add("testListener4");

                Map<String, Object> objects = new HashMap<String, Object>();
                objects.put("testJob.testListeners", customListeners);
                objects.put("testListeners", defaultListeners);
                return objects;
            }
        });

        sut.executeBefore(new Runner<String>() {
            @Override
            public void run(String listener, NablarchListenerContext context) {
                listenerList.add("before:" + listener);
            }
        });

        // デフォルトで定義されたリスナーリストが設定された順番に実行されること
        assertThat(listenerList.size(), is(2));
        assertThat(listenerList.get(0), is("before:testListener3"));
        assertThat(listenerList.get(1), is("before:testListener4"));

        // スタックに積まれていること
        LinkedList<String> stack = Deencapsulation.getField(sut, "executedListenerStack");
        assertThat(stack.size(), is(2));
        assertThat(stack.pop(), is("testListener4"));
        assertThat(stack.pop(), is("testListener3"));
    }

    /**
     * {@link NablarchListenerExecutor#executeBefore(NablarchListenerExecutor.Runner)}のテストケース<br />
     * リポジトリにリスナーのリストが登録されていない場合、リスナーが実行されずに処理が終了すること
     */
    @Test
    public void testExecuteBefore_notFound() {
        sut.executeBefore(new Runner<String>() {
            @Override
            public void run(String listener, NablarchListenerContext context) {
                listenerList.add("before:" + listener);
            }
        });

        // リスナーが実行されていないこと
        assertThat(listenerList.size(), is(0));

        // スタックに積まれていないこと
        LinkedList<String> stack = Deencapsulation.getField(sut, "executedListenerStack");
        assertThat(stack.size(), is(0));
    }

    /**
     * {@link NablarchListenerExecutor#executeBefore(NablarchListenerExecutor.Runner)}のテストケース<br />
     * リスナー実行時に例外が発生した場合、例外が発生した以降のリスナーが実行されないこと
     */
    @Test
    public void testExecuteBefore_exception() {
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                List<String> listeners = new ArrayList<String>();
                listeners.add("testListener1");
                listeners.add("exceptionListener");
                listeners.add("testListener2");

                Map<String, Object> objects = new HashMap<String, Object>();
                objects.put("testListeners", listeners);
                return objects;
            }
        });

        try {
            sut.executeBefore(new Runner<String>() {
                @Override
                public void run(String listener, NablarchListenerContext context) {
                    listenerList.add("before:" + listener);
                    if (listener.startsWith("exception")) {
                        throw new IllegalStateException("before exception.");
                    }
                }
            });
            fail("ここには到達しない");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("before exception."));
        }

        // 例外が発生した以降のリスナーが実行されないこと
        assertThat(listenerList.size(), is(2));
        assertThat(listenerList.get(0), is("before:testListener1"));
        assertThat(listenerList.get(1), is("before:exceptionListener"));

        // 実行されたリスナーのみスタックに積まれていること
        LinkedList<String> stack = Deencapsulation.getField(sut, "executedListenerStack");
        assertThat(stack.size(), is(2));
        assertThat(stack.pop(), is("exceptionListener"));
        assertThat(stack.pop(), is("testListener1"));
    }

    /**
     * {@link NablarchListenerExecutor#executeBefore(NablarchListenerExecutor.Runner)}のテストケース<br />
     * リスナー実行時にエラーが発生した場合、エラーが発生した以降のリスナーが実行されないこと
     */
    @Test
    public void testExecuteBefore_error() {
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                List<String> listeners = new ArrayList<String>();
                listeners.add("testListener1");
                listeners.add("testListener2");
                listeners.add("errorListener");
                listeners.add("testListener3");

                Map<String, Object> objects = new HashMap<String, Object>();
                objects.put("testListeners", listeners);
                return objects;
            }
        });

        try {
            sut.executeBefore(new Runner<String>() {
                @Override
                public void run(String listener, NablarchListenerContext context) {
                    listenerList.add("before:" + listener);
                    if (listener.startsWith("error")) {
                        throw new StackOverflowError("before error.");
                    }
                }
            });
            fail("ここには到達しない");
        } catch (Error e) {
            assertThat(e.getMessage(), is("before error."));
        }

        // エラーが発生した以降のリスナーが実行されないこと
        assertThat(listenerList.size(), is(3));
        assertThat(listenerList.get(0), is("before:testListener1"));
        assertThat(listenerList.get(1), is("before:testListener2"));
        assertThat(listenerList.get(2), is("before:errorListener"));

        // 実行されたリスナーのみスタックに積まれていること
        LinkedList<String> stack = Deencapsulation.getField(sut, "executedListenerStack");
        assertThat(stack.size(), is(3));
        assertThat(stack.pop(), is("errorListener"));
        assertThat(stack.pop(), is("testListener2"));
        assertThat(stack.pop(), is("testListener1"));
    }

    /**
     * {@link NablarchListenerExecutor#executeAfter(NablarchListenerExecutor.Runner)} のテストケース<br />
     * スタックに積まれている順番にリスナーが実行されること
     */
    @Test
    public void testExecuteAfter_basic() throws Exception {

        LinkedList<String> stack = Deencapsulation.getField(sut, "executedListenerStack");
        stack.push("testListener1");
        stack.push("testListener2");
        stack.push("testListener3");

        sut.executeAfter(new Runner<String>() {
            @Override
            public void run(String listener, NablarchListenerContext context) {
                listenerList.add("after:" + listener);
            }
        });

        // スタックに積まれたリスナーが順番に実行されること
        assertThat(listenerList.size(), is(3));
        assertThat(listenerList.get(0), is("after:testListener3"));
        assertThat(listenerList.get(1), is("after:testListener2"));
        assertThat(listenerList.get(2), is("after:testListener1"));
    }

    /**
     * {@link NablarchListenerExecutor#executeAfter(NablarchListenerExecutor.Runner)} のテストケース<br />
     * 実行可能なリスナーが存在しない場合、例外が発生せずに処理が終了すること
     */
    @Test
    public void testExecuteAfter_empty() throws Exception {

        sut.executeAfter(new Runner<String>() {
            @Override
            public void run(String listener, NablarchListenerContext context) {
                listenerList.add("after:" + listener);
            }
        });

        // リスナーが実行されていないこと
        assertThat(listenerList.size(), is(0));
    }

    /**
     * {@link NablarchListenerExecutor#executeAfter(NablarchListenerExecutor.Runner)} のテストケース<br />
     * リスナー実行時に例外が発生した場合、全てのリスナー実行後に例外が呼び出し元に送出されること
     */
    @Test
    public void testExecuteAfter_exception() throws Exception {

        LinkedList<String> stack = Deencapsulation.getField(sut, "executedListenerStack");
        stack.push("testListener1");
        stack.push("exceptionListener");
        stack.push("testListener3");

        try {
            sut.executeAfter(new Runner<String>() {
                @Override
                public void run(String listener, NablarchListenerContext context) {
                    listenerList.add("after:" + listener);
                    if (listener.startsWith("exception")) {
                        throw new IllegalStateException("after exception.");
                    }
                }
            });
            fail("ここには到達しない");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("after exception."));
        }

        // スタックに積まれたリスナーが順番に実行されること
        assertThat(listenerList.size(), is(3));
        assertThat(listenerList.get(0), is("after:testListener3"));
        assertThat(listenerList.get(1), is("after:exceptionListener"));
        assertThat(listenerList.get(2), is("after:testListener1"));

        // ログの確認
        assertThat(InMemoryAppender.getLogMessages("ALL"), contains(
                containsString("failed to execute listener. job=[testJob]")));
    }

    /**
     * {@link NablarchListenerExecutor#executeAfter(NablarchListenerExecutor.Runner)} のテストケース<br />
     * リスナー実行時にエラーが発生した場合、全てのリスナー実行後にエラーが呼び出し元に送出されること
     */
    @Test
    public void testExecuteAfter_error() throws Exception {

        LinkedList<String> stack = Deencapsulation.getField(sut, "executedListenerStack");
        stack.push("testListener1");
        stack.push("errorListener");
        stack.push("testListener3");

        try {
            sut.executeAfter(new Runner<String>() {
                @Override
                public void run(String listener, NablarchListenerContext context) {
                    listenerList.add("after:" + listener);
                    if (listener.startsWith("error")) {
                        throw new StackOverflowError("after error.");
                    }
                }
            });
            fail("ここには到達しない");
        } catch (Error e) {
            assertThat(e.getMessage(), is("after error."));
        }

        // スタックに積まれたリスナーが順番に実行されること
        assertThat(listenerList.size(), is(3));
        assertThat(listenerList.get(0), is("after:testListener3"));
        assertThat(listenerList.get(1), is("after:errorListener"));
        assertThat(listenerList.get(2), is("after:testListener1"));

        // 発生した例外のログが出力されること
        assertThat(InMemoryAppender.getLogMessages("ALL"), contains(
                containsString("failed to execute listener. job=[testJob]")));
    }

    /**
     * {@link NablarchListenerExecutor#executeAfter(NablarchListenerExecutor.Runner)} のテストケース<br />
     * 複数の例外が発生した場合に、最初に発生した例外が送出されること。
     */
    @Test
    public void testExecuteAfter_multiError() throws Exception {

        LinkedList<String> stack = Deencapsulation.getField(sut, "executedListenerStack");
        stack.push("testListener1");
        stack.push("exceptionListener");
        stack.push("testListener3");
        stack.push("errorListener");
        stack.push("testListener5");

        try {
            sut.executeAfter(new Runner<String>() {
                @Override
                public void run(String listener, NablarchListenerContext context) {
                    listenerList.add("after:" + listener);
                    if (listener.startsWith("exception")) {
                         throw new IllegalStateException("after exception.");
                    } else if (listener.startsWith("error")) {
                        throw new StackOverflowError("after error.");
                    }
                }
            });
            fail("ここには到達しない");
        } catch (Error e) {
            assertThat(e.getMessage(), is("after error."));
        }

        // スタックに積まれたリスナーが順番に実行されること
        assertThat(listenerList.size(), is(5));
        assertThat(listenerList.get(0), is("after:testListener5"));
        assertThat(listenerList.get(1), is("after:errorListener"));
        assertThat(listenerList.get(2), is("after:testListener3"));
        assertThat(listenerList.get(3), is("after:exceptionListener"));
        assertThat(listenerList.get(4), is("after:testListener1"));

        // 発生した全ての例外のログが出力されること
        assertThat(InMemoryAppender.getLogMessages("ALL"), contains(
                containsString("failed to execute listener. job=[testJob]"),
                containsString("failed to execute listener. job=[testJob]")));
    }

    /**
     * {@link NablarchListenerExecutor#executeAfter(NablarchListenerExecutor.Runner)} のテストケース<br />
     * ステップ名を指定して実行した場合に、ステップ名を含んだログが出力されること。
     */
    @Test
    public void testExecuteAfter_stepLevel() throws Exception {

        sut = new NablarchListenerExecutor<String>("testListeners", jobContext, stepContext);

        LinkedList<String> stack = Deencapsulation.getField(sut, "executedListenerStack");
        stack.push("testListener1");
        stack.push("exceptionListener");
        stack.push("testListener3");

        try {
            sut.executeAfter(new Runner<String>() {
                @Override
                public void run(String listener, NablarchListenerContext context) {
                    listenerList.add("after:" + listener);
                    if (listener.startsWith("exception")) {
                        throw new IllegalStateException("after exception.");
                    }
                }
            });
            fail("ここには到達しない");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("after exception."));
        }

        // スタックに積まれたリスナーが順番に実行されること
        assertThat(listenerList.size(), is(3));
        assertThat(listenerList.get(0), is("after:testListener3"));
        assertThat(listenerList.get(1), is("after:exceptionListener"));
        assertThat(listenerList.get(2), is("after:testListener1"));

        // 出力されるログにステップ名が表示されていること
        assertThat(InMemoryAppender.getLogMessages("ALL"), contains(
                containsString("failed to execute listener. job=[testJob], step=[testStep]")));
    }

    /**
     * {@link NablarchListenerExecutor#executeOnError(NablarchListenerExecutor.Runner)} のテストケース<br />
     * スタックに積まれている順番にリスナーが実行されること
     */
    @Test
    public void testExecuteOnError_basic() throws Exception {

        sut = new NablarchListenerExecutor<String>("testListeners", jobContext, stepContext);

        LinkedList<String> stack = Deencapsulation.getField(sut, "executedListenerStack");
        stack.push("testListener1");
        stack.push("testListener2");
        stack.push("testListener3");

        final Exception e = new IllegalStateException("testException");
        sut.executeOnError(new Runner<String>() {
            @Override
            public void run(String listener, NablarchListenerContext context) {
                listenerList.add("onError:" + listener + ":" + e.getMessage());
            }
        });

        // スタックに積まれたリスナーが順番に実行されること
        assertThat(listenerList.size(), is(3));
        assertThat(listenerList.get(0), is("onError:testListener3:testException"));
        assertThat(listenerList.get(1), is("onError:testListener2:testException"));
        assertThat(listenerList.get(2), is("onError:testListener1:testException"));
    }

    /**
     * {@link NablarchListenerExecutor#executeOnError(NablarchListenerExecutor.Runner)} のテストケース<br />
     * 実行可能なリスナーが存在しない場合、例外が発生せずに処理が終了すること
     */
    @Test
    public void testExecuteOnError_empty() throws Exception {

        final Exception e = new IllegalStateException("testException");
        sut.executeOnError(new Runner<String>() {
            @Override
            public void run(String listener, NablarchListenerContext context) {
                listenerList.add("onError:" + listener + ":" + e.getMessage());
            }
        });

        // リスナーが実行されていないこと
        assertThat(listenerList.size(), is(0));
    }

    /**
     * {@link NablarchListenerExecutor#executeOnError(NablarchListenerExecutor.Runner)} のテストケース<br />
     * リスナー実行時に例外が発生した場合、全てのリスナー実行後に例外が呼び出し元に送出されること
     */
    @Test
    public void testExecuteOnError_exception() throws Exception {

        sut = new NablarchListenerExecutor<String>("testListeners", jobContext, stepContext);

        LinkedList<String> stack = Deencapsulation.getField(sut, "executedListenerStack");
        stack.push("testListener1");
        stack.push("exceptionListener");
        stack.push("testListener3");

        try {
            final Exception e = new IllegalStateException("testException");
            sut.executeOnError(new Runner<String>() {
                @Override
                public void run(String listener, NablarchListenerContext context) {
                    listenerList.add("onError:" + listener + ":" + e.getMessage());
                    if (listener.startsWith("exception")) {
                        throw new IllegalStateException("onError " + e.getMessage() + " exception.");
                    }
                }
            });
            fail("ここには到達しない");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("onError testException exception."));
        }

        // スタックに積まれたリスナーが順番に実行されること
        assertThat(listenerList.size(), is(3));
        assertThat(listenerList.get(0), is("onError:testListener3:testException"));
        assertThat(listenerList.get(1), is("onError:exceptionListener:testException"));
        assertThat(listenerList.get(2), is("onError:testListener1:testException"));

        // 発生した例外のログが出力されること
        assertThat(InMemoryAppender.getLogMessages("ALL"), contains(
                containsString("failed to execute listener. job=[testJob], step=[testStep]")));
    }

    /**
     * {@link NablarchListenerExecutor#executeOnError(Runner)} のテストケース<br />
     * リスナー実行時にエラーが発生した場合、全てのリスナー実行後にエラーが呼び出し元に送出されること
     */
    @Test
    public void testExecuteOnError_error() throws Exception {

        sut = new NablarchListenerExecutor<String>("testListeners", jobContext, stepContext);

        LinkedList<String> stack = Deencapsulation.getField(sut, "executedListenerStack");
        stack.push("testListener1");
        stack.push("testListener2");
        stack.push("errorListener");

        try {
            final Exception e = new IllegalStateException("testException");
            sut.executeOnError(new Runner<String>() {
                @Override
                public void run(String listener, NablarchListenerContext context) {
                    listenerList.add("onError:" + listener + ":" + e.getMessage());
                    if (listener.startsWith("error")) {
                        throw new StackOverflowError("onError " + e.getMessage() + " error.");
                    }
                }
            });
            fail("ここには到達しない");
        } catch (Error e) {
            assertThat(e.getMessage(), is("onError testException error."));
        }

        // スタックに積まれたリスナーが順番に実行されること
        assertThat(listenerList.size(), is(3));
        assertThat(listenerList.get(0), is("onError:errorListener:testException"));
        assertThat(listenerList.get(1), is("onError:testListener2:testException"));
        assertThat(listenerList.get(2), is("onError:testListener1:testException"));

        // 発生した例外のログが出力されること
        assertThat(InMemoryAppender.getLogMessages("ALL"), contains(
                containsString("failed to execute listener. job=[testJob], step=[testStep]")));
    }

    /**
     * {@link NablarchListenerExecutor#executeOnError(Runner)} のテストケース<br />
     * 複数の例外が発生した場合に、最初に発生した例外が送出されること。
     */
    @Test
    public void testExecuteOnError_multiError() throws Exception {

        sut = new NablarchListenerExecutor<String>("testListeners", jobContext, stepContext);

        LinkedList<String> stack = Deencapsulation.getField(sut, "executedListenerStack");
        stack.push("testListener1");
        stack.push("errorListener");
        stack.push("testListener3");
        stack.push("exceptionListener");
        stack.push("testListener5");

        try {
            final Exception e = new IllegalStateException("testException");
            sut.executeOnError(new Runner<String>() {
                @Override
                public void run(String listener, NablarchListenerContext context) {
                    listenerList.add("onError:" + listener + ":" + e.getMessage());
                    if (listener.startsWith("exception")) {
                        throw new IllegalStateException("onError " + e.getMessage() + " exception.");
                    } else if (listener.startsWith("error")) {
                        throw new StackOverflowError("onError " + e.getMessage() + " error.");
                    }
                }
            });
            fail("ここには到達しない");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("onError testException exception."));
        }

        // スタックに積まれたリスナーが順番に実行されること
        assertThat(listenerList.size(), is(5));
        assertThat(listenerList.get(0), is("onError:testListener5:testException"));
        assertThat(listenerList.get(1), is("onError:exceptionListener:testException"));
        assertThat(listenerList.get(2), is("onError:testListener3:testException"));
        assertThat(listenerList.get(3), is("onError:errorListener:testException"));
        assertThat(listenerList.get(4), is("onError:testListener1:testException"));

        // 発生した例外のログが全て出力されること
        assertThat(InMemoryAppender.getLogMessages("ALL"), contains(
                containsString("failed to execute listener. job=[testJob], step=[testStep]"),
                containsString("failed to execute listener. job=[testJob], step=[testStep]")));
    }

    /**
     * {@link NablarchListenerExecutor#executeBefore(NablarchListenerExecutor.Runner)}のテストケース
     * <br>
     * リポジトリに個別に登録されているリスナーリストの順番でリスナーが順次実行されていくこと。
     */
    @Test
    public void testExecuteBefore_customStep() {
        sut = new NablarchListenerExecutor<String>("testListeners", jobContext, stepContext);
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                List<String> defaultListeners = new ArrayList<String>();
                defaultListeners.add("testListener1");
                defaultListeners.add("testListener2");

                List<String> customListeners = new ArrayList<String>();
                customListeners.add("testListener3");
                customListeners.add("testListener4");

                List<String> customStepListeners = new ArrayList<String>();
                customStepListeners.add("testStep1");
                customStepListeners.add("testStep2");

                Map<String, Object> objects = new HashMap<String, Object>();
                objects.put("testJob.testStep.testListeners", customStepListeners);
                objects.put("testJob.testListeners", customListeners);
                objects.put("testListeners", defaultListeners);
                return objects;
            }
        });

        sut.executeBefore(new Runner<String>() {
            @Override
            public void run(String listener, NablarchListenerContext context) {
                listenerList.add("before:" + listener);
            }
        });

        // デフォルトで定義されたリスナーリストが設定された順番に実行されること
        assertThat(listenerList.size(), is(2));
        assertThat(listenerList.get(0), is("before:testStep1"));
        assertThat(listenerList.get(1), is("before:testStep2"));

        // スタックに積まれていること
        LinkedList<String> stack = Deencapsulation.getField(sut, "executedListenerStack");
        assertThat(stack.size(), is(2));
        assertThat(stack.pop(), is("testStep2"));
        assertThat(stack.pop(), is("testStep1"));
    }
}