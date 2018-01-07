package client;

import java.util.Scanner;
import java.util.concurrent.*;

/**
 * Enkeltrådat användargränssnitt som
 * använder sig av ServerTask för att hantera uppgifter
 */
public class Command implements Runnable{

    public static boolean lockedMode = true;
    public static String user = "";
    SafeStandardOut out;
    public ThreadLocal<ForkJoinTask> prevTask = new ThreadLocal<>();
    ConcurrentLinkedQueue<ForkJoinTask> queue;

    public Command (ConcurrentLinkedQueue q, SafeStandardOut o){
        queue = q;
        out = o;
    }

    /**
     * hasNext används för att iterera genom ord i en sträng
     * Gör så att String[] får ett list liknande gränssnitt
     */
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

    /**
     * Metod för att starta användargränssnittet i en egen tråd
     */
    public void start(){
        new Thread(this).start();
    }

    /**
     * Run metod implementaras som en del av interfacet Runnable
     */
    @Override
    public void run(){
        Scanner UsrIn = new Scanner(System.in);
        String [] sQuery = UsrIn.nextLine().split(" ");
        ServerTask cServerTask = new ServerTask();

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
        if(prevTask.get() != null){
            ForkJoinTask join = prevTask.get();
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
        prevTask.set(queue.peek());
        this.run();
    }
}
