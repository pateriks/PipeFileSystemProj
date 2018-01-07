package client;

/**
 * Trådsäker standard output
 * Behövs för att utskrifterna inte ska skriva samtidigt
 */
class SafeStandardOut {
    synchronized void println(String s){
        System.out.println(s);
    }
    synchronized void print(String s){
        System.out.print(s);
    }
}
