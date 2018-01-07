package client;

import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.concurrent.*;

/**
 * Enkeltrådat användargränssnitt som
 * använder sig av ServerTask för att hantera uppgifter
 */
public class NonBlockingInterpreter implements Runnable{
    //Command line pixeltäthet
    final int HEIGHT = (int) Math.pow(2, 4);
    final int WIDTH = (int) Math.pow(2, 7);
    //Exekveringsläge
    public boolean lockedMode = true;
    SafeStandardOut out;
    public ThreadLocal<ForkJoinTask> prevTask = new ThreadLocal<>();
    ConcurrentLinkedQueue<ForkJoinTask> queue;

    public NonBlockingInterpreter(ConcurrentLinkedQueue q, SafeStandardOut o){
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
     * Kallas för nytt kommando från användaren
     * @return
     */
    private String[] readNextLine(){
        if(!lockedMode) {
            if(RmiClient.account == null){
                RmiClient.lock.lock();
                try {
                    RmiClient.complete.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                RmiClient.lock.unlock();
            }
            try {
                print(RmiClient.account.getName() + RmiClient.PROMPT);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }else{
            print(RmiClient.PROMPT);
        }
        Scanner UsrIn = new Scanner(System.in);
        return UsrIn.nextLine().split(" ");
    }

    /**
     * Misc
     * @param s
     */
    private void print(String s){
        out.print(s);
    }

    /**
     * Misc
     * @param s
     */
    private void println(String s){
        out.println(s);
    }

    /**
     * Misc
     */
    private void println(){
        out.println("");
    }

    /**
     * Run metod implementaras som en del av interfacet Runnable
     */
    @Override
    public void run(){

        String [] sQuery = readNextLine();

        ServerTask cServerTask = new ServerTask(true, this);

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

        queue.add(ForkJoinPool.commonPool().submit(cServerTask));

        RmiClient.lock.lock();
        RmiClient.complete.signal();
        RmiClient.lock.unlock();

        prevTask.set(queue.peek());
        this.run();
    }
}
