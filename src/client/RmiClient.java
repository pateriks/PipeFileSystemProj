package client;

import company.common.AccountIntf;
import company.server.PipeServer;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RmiClient {
    public final static String HOST = "192.168.0.16";
    public static AccountIntf acc = null;
    public static final Lock lock = new ReentrantLock();
    public static final Condition complete = lock.newCondition();
    public static void main(String args[]) throws Exception {
        SafeStandardOut out = new SafeStandardOut();
        ConcurrentLinkedQueue<ForkJoinTask> taskQ = new ConcurrentLinkedQueue<>();
        Command cmd = new Command(taskQ, out);
        Thread t = new Thread(cmd);
        t.start();

        //lock.lock();
        //complete.signal();
        //lock.unlock();
        boolean bool = true;
        while(bool) {
            if(acc == null){
                out.print(cmd.PROMPT);
            }else{
                out.print(acc.getName() + cmd.PROMPT);
            }
            lock.lock();
            complete.await();
            lock.unlock();
            if(taskQ.peek() != null){
                if (cmd.lockedMode) {
                    acc = (AccountIntf) taskQ.poll().join();
                    if(acc != null) {
                        cmd.lockedMode = false;
                    }
                }else if (ServerTask.graphics){
                    out.println("File content:");
                    String s;
                    out.println(s = (String) taskQ.poll().join());
                    ServerTask.graphics = false;
                }else{
                }
            }
        }
    }
}