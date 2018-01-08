package client;

import company.common.AccountIntf;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RmiClient {

    private final static long ONE_SEC = 1000;

    protected final static String HOST = "localhost";
    protected final static String PROMPT = ">";

    protected static final Lock lock = new ReentrantLock();
    protected static final Condition complete = lock.newCondition();

    public static AccountIntf account = null;

    /**
     *
     * Program som använder sig utav ett Command UI interface med hjälp av
     * @NonBlockingInterpreter
     *
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {

        lock.lock();

        ServerTask.setHost(HOST);

        SafeStandardOut out = new SafeStandardOut();

        ConcurrentLinkedQueue<ForkJoinTask> taskQ = new ConcurrentLinkedQueue<>();

        NonBlockingInterpreter cmd = new NonBlockingInterpreter(taskQ, out);

        cmd.start();

        boolean run = true;

        while(run) {

            complete.signal();
            lock.unlock();
            lock.lock();
            complete.await(ONE_SEC, TimeUnit.MILLISECONDS);

            while(taskQ.peek() != null){
                Object res;
                try {
                    res = taskQ.poll().get(ONE_SEC*5, TimeUnit.MILLISECONDS);
                }catch (TimeoutException e){
                    res = null;
                }
                if(res != null) {
                    if (res instanceof AccountIntf) {
                        account = (AccountIntf) res;
                    }else if (res instanceof String) {
                        String s = (String) res;
                        out.println(s);
                    }
                }
            }

        }
    }
}