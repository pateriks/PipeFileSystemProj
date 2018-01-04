package client;

import company.common.AccountIntf;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinTask;

public class RmiClient {
    public final static String HOST = "192.168.0.16";
    public static AccountIntf acc = null;
    public static void main(String args[]) throws Exception {
        SafeStandardOut out = new SafeStandardOut();
        ConcurrentLinkedQueue<ForkJoinTask> taskQ = new ConcurrentLinkedQueue<>();
        Command cmd = new Command(taskQ, out);
        Thread t = new Thread(cmd);
        t.start();
        boolean bool = true;
        while(bool) {
            if(taskQ.peek() != null){
                if (cmd.lockedMode) {
                    acc = (AccountIntf) taskQ.poll().join();
                    if(acc != null) {
                        cmd.lockedMode = false;
                    }
                }else {
                    out.println((String) taskQ.poll().join());
                }
            }
            Thread.sleep(1000);
        }
    }
}