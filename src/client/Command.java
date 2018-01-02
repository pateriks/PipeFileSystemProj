package client;

import company.common.AccountIntf;

import java.rmi.Naming;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.*;

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

    public Command (ConcurrentLinkedQueue q, SafeStandardOut out){
        queue = q;
        this.out = out;
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
        out.print(user + PROMPT);
        UsrIn.useDelimiter(" ");

        ServerTask cServerTask = new ServerTask(UsrIn.nextLine(), out);
        //Delar upp förfrågan i delar
        String [] query = cServerTask.sQuery.split(" ");
        //Itererar igenom delarna
        for (String s = hasNext(query); s != null; s = hasNext(query)){
            try {
                cServerTask.add(s);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //Titta om föregående asynkrona uppgift är slutförd
        if(l.get() != null){
            ForkJoinTask join = l.get();
            if(!join.isDone()){
                out.println("System slow and Threads need maintenance");
                out.println(Integer.toString(Thread.activeCount()));
            }
        }
        queue.add(ForkJoinPool.commonPool().submit(cServerTask));
        //Vi tar inte bort element från kön i command
        l.set(queue.peek());
        this.run();
    }
}
