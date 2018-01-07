package client;

import company.common.AccountIntf;
import company.server.Account;
import company.server.PipeServer;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RmiClient {

    public final static String HOST = "192.168.0.16";
    public final static String PROMPT = ">";
    public static AccountIntf acc = null;
    public static final Lock lock = new ReentrantLock();
    public static final Condition complete = lock.newCondition();

    public static void main(String args[]) throws Exception {

        lock.lock();

        ServerTask.setHost(HOST);

        SafeStandardOut out = new SafeStandardOut();
        ConcurrentLinkedQueue<ForkJoinTask> taskQ = new ConcurrentLinkedQueue<>();
        Command cmd = new Command(taskQ, out);
        Thread t = new Thread(cmd);
        t.start();

        boolean run = true;

        while(run) {

            if(acc == null){
                out.print(PROMPT);
            }else{
                out.print(acc.getName() + PROMPT);
            }

            lock.unlock();
            lock.lock();
            complete.await();

            while(taskQ.peek() != null){
                Object res;
                try {
                    res = taskQ.poll().get(1000, TimeUnit.MILLISECONDS);
                }catch (TimeoutException e){
                    res = null;
                }
                if(res != null) {
                    if (res instanceof AccountIntf) {
                        acc = (AccountIntf) res;
                        if (acc != null) {
                            cmd.lockedMode = false;
                        }
                    } else if (res instanceof String) {
                        out.println("File content:");
                        String s = (String) res;
                        out.println(s);
                    }
                }
            }

        }
    }
}