package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final static String address = "127.0.0.1";
    private final static int port = 23456;
    private static ServerSocket serverSocket;
    private static boolean serverRunning;
    private static ExecutorService executorService;

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName(address));
            executorService = Executors
                    .newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            serverRunning = true;
            System.out.println("Server started!");

            while (serverRunning) {
                /*
                 serverRunning can't close the loop instantly as the execution would be blocked
                 on serverSocket.accept() call. serverSocket has to be closed so that the blocking call get Interrupted
                 This would through an exception which would close the loop. This is the best practice.
                */

                Socket socket = serverSocket.accept();
                executorService.submit(new ServerSession(socket));
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void close() {
        serverRunning = false;
        executorService.shutdown();
        //serializing the identifiers before closing the server
        FilesIdentifiers.serialize(ServerSession.filesIdentifiers);
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}