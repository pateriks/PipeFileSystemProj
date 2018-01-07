package company.server;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;
import java.util.concurrent.*;

public class TCP {
    private ServerSocketChannel ssC;
    private Selector selector;
    private boolean run;
    protected LinkedList <String> que = new LinkedList<>();
    private Map <Integer, ForkJoinTask<Integer>> lookup = Collections.synchronizedMap(new HashMap<Integer, ForkJoinTask<Integer>>());
    private long timer;
    private void initSelector() throws IOException {
        selector = Selector.open();
    }
    private void initSSC() throws IOException {
        ssC = ServerSocketChannel.open();
        ssC.configureBlocking(false);
        ssC.bind(new InetSocketAddress(8888));
        ssC.register(selector, SelectionKey.OP_ACCEPT);
    }
    public void send(SelectionKey key, String s) throws IOException {
        String path;
        if(que.peek() != null){
            path = que.poll();
        }else{
            path = "null";
        }
        ForkJoinTask<Integer> task = ForkJoinPool.commonPool().submit(new HandlerTCP((SocketChannel)key.channel(), lookup, path), 1);
        //if(!s.equals("resend")) {
        //    lookup.put(key.channel().hashCode(), task);
        //}

    }
    private String receive(SelectionKey key) throws IOException {
        SocketChannel sC = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        sC.read(buffer);
        buffer.flip();
        Charset charset = Charset.forName("UTF-8");
        CharsetDecoder decoder = charset.newDecoder();
        CharBuffer charBuffer = null;
        try {
            charBuffer = decoder.decode(buffer);
        } catch (CharacterCodingException e) {
            e.printStackTrace();
        }
        return charBuffer.toString();
    }
    public void start() throws IOException {
        initSelector();
        initSSC();
        createSocketServer();
        System.out.println("started");
    }

    private void startHandler(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel sC = serverSocketChannel.accept();
        sC.configureBlocking(false);
        sC.register(selector, SelectionKey.OP_READ);
        send(sC.keyFor(selector), "bla");
        //ForkJoinTask<Integer> task = ForkJoinPool.commonPool().submit(new HandlerTCP(sC), 1);
        //lookup.put(sC.hashCode(), task);
    }

    private void createSocketServer() throws IOException{
        run = true;
        new Thread(() -> {
            while (run) {
                try {
                    if (selector.select() == 0) {
                        System.out.println("no channels ready");
                        continue;
                    }
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext() && run) {
                        System.out.println("iterate");
                        SelectionKey key = iterator.next();
                        iterator.remove();

                        if (!key.isValid()) {
                            continue;
                        }
                        if (key.isAcceptable()) {
                            startHandler(key);
                            System.out.println("accepted");
                            timer = System.currentTimeMillis();
                        } else if (key.isReadable()) {
                            System.out.println("readable");
                            String getMsg = receive(key);
                            //ForkJoinTask<Integer> task = lookup.get(key.channel().hashCode());
                            if(getMsg.equals("bye")){
                                key.channel().close();
                                System.out.println("closed");
                                run = false;
                                //task.cancel(true);
                                break;
                            }
                            //else if (task.isDone()) {
                                send(key, getMsg);
                            //}else{
                            //    send(key, "resend");
                            //}
                        }
                        if(!run){
                            break;
                        }
                    }
                }catch(Exception e) {
                    System.out.println(e);
                }
            }
            try {
                ssC.close();
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
