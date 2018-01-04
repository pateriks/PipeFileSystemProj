package company.server;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class HandlerTCP implements Runnable {

    private LinkedList <String> args = new <String> LinkedList();
    private StringBuilder hidden;
    private SocketChannel sC;
    private Map<Integer, ForkJoinTask<Integer>> pendingTasks;
    String fromClient;
    
    public HandlerTCP(SocketChannel k, Map t, String f){
        System.out.println("handler created");
        sC = k;
        pendingTasks = t;
        fromClient = f;
        System.out.println(fromClient);
    }

    public HandlerTCP(SocketChannel k){
        System.out.println("handler created");
        sC = k;
    }

    public void run(){
        System.out.println("computing");
        String ret;
        if(fromClient == null || fromClient.equals("null")){
            ret = getResponse("no request string");
        }else if(fromClient.equals("resend")){
            ret = "resend";
        }else if(fromClient.equals("bye")){
            ret = "bye";
        }else {
            System.out.println("gets response");
            ret = getResponse(fromClient);
        }if(!ret.equals("nosend")) {
            ByteBuffer buffer = ByteBuffer.wrap(ret.getBytes());
            long ofs = 0;
            try {
                System.out.println(ret);
                ofs = sC.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(ofs < ret.length()){
                System.out.println("time2stay " + ofs + " :: " + ret.length());
            }else if(!ret.equals("bye")){
                System.out.println("time2go");
                pendingTasks.replace(sC.hashCode(), ForkJoinPool.commonPool().submit(new HandlerTCP(sC, pendingTasks, "bye"), 1));
            }
            System.out.println("finished");
        }
    }
    
    private String sendNotImpl(){
        return "not implemented";
    }
    
    private String sendString(){
        return null;
    }
    
    private boolean isCommand(String s) {
        return (s.length()>0);
    }
    
    private boolean isLetter(String s) {
        return (s.length()==1);
    }

    private StringBuilder readFromFile(String s) throws FileNotFoundException{
        Scanner sc = new Scanner(new File(PipeServer.ROT.concat(s)));
        StringBuilder words = new StringBuilder();
        while(sc.hasNextLine()){
            String word = sc.nextLine();
            words.append(word);
        }
        return words;
    }
    
    public String getResponse(String s) {
        try {
            return readFromFile(fromClient).toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "bye";
        }
    }
}
