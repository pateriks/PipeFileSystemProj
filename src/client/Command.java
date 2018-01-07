package client;

import company.common.AccountIntf;

import java.rmi.Naming;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Command implements Runnable{
    //Instansierar en standardout för att göra programmet mer läsbart
    SafeStandardOut out = new SafeStandardOut();
    //Standard prompt tecken
    final String PROMPT = ">";
    final LinkedList<ServerTask> serverTasks = new LinkedList();

    public static boolean lockedMode = true;
    public static String user = "";
    public ThreadLocal<ForkJoinTask> l = new ThreadLocal<>();


    ConcurrentLinkedQueue<ForkJoinTask> queue;

    public Command (ConcurrentLinkedQueue q, SafeStandardOut o){
        queue = q;
        out = o;
    }

    //Verktyg högst är satta på toppen
    //hasNext används för att iterera genom ord i en sträng
    private String hasNext(String[] s){
        if(s[0] == null){
            return null;
        }else {
            String ret = s[0];
            for(int i = 1; i <= s.length; i++){
                try{
                    s[i-1] = s[i];
                }catch (ArrayIndexOutOfBoundsException e){
                    s[i-1] = null;
                }
            }
            return ret;
        }
    }

    public void run(){
        Scanner UsrIn = new Scanner(System.in);
        String [] sQuery = UsrIn.nextLine().split(" ");

        ServerTask cServerTask = new ServerTask(out);
        //Delar upp förfrågan i delar
        //Itererar igenom delarna
        for (String c = hasNext(sQuery); c != null; c = hasNext(sQuery)){
            String a;
            try {
                if((a = hasNext(sQuery)) != null) {
                    c = cServerTask.add(c, a);
                    if(c != null){
                        if((a = hasNext(sQuery)) != null) {
                            cServerTask.add(c, a);
                        }else{
                            cServerTask.add(c, "");
                        }
                    }
                }else{
                    cServerTask.add(c, "");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Titta om föregående asynkrona uppgift är slutförd
        if(l.get() != null){
            ForkJoinTask join = l.get();
            if(!join.isDone()){
                out.println("system slow and threads need maintenance");
                out.println(Integer.toString(Thread.activeCount()));
            }
        }
        out.println("Threads: " + Integer.toString(Thread.activeCount()));
        queue.add(ForkJoinPool.commonPool().submit(cServerTask));
        RmiClient.lock.lock();
        RmiClient.complete.signal();
        RmiClient.lock.unlock();
        //Vi tar inte bort element från kön i command
        l.set(queue.peek());
        this.run();
    }
}
