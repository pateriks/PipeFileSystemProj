package client;

class SafeStandardOut {
    synchronized void println(String s){
        System.out.println(s);
    }
    synchronized void print(String s){
        System.out.print(s);
    }
    synchronized void printli(){
    }
}
