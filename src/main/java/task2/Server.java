package task2;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final ExecutorService executor;
    private Map<String, Map<String, Handler>> handlers;

    public Server(int maxThreadsNum) {
        this.executor = Executors.newFixedThreadPool(maxThreadsNum);
        handlers = new HashMap<>();
    }

    public void listen(int port) {
        if (!checkPort(port)) return;
        try (final ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server TASK2 started on port " + port);
            while (true) {
                SocketHandler handler = new SocketHandler(serverSocket.accept(), handlers);
                this.executor.submit(handler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addHandler(String httpMethod, String urlPath, Handler handler) {
        if (handlers.containsKey(httpMethod)) {
            handlers.get(httpMethod).put(urlPath, handler);
        } else {
            handlers.put(httpMethod, new HashMap<String, Handler>() {{
                put(urlPath, handler);
            }});
        }
    }

    public static boolean checkPort(int portNum) {
        try (
                ServerSocket serverSocket = new ServerSocket()
        ) {
            serverSocket.setReuseAddress(false);
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), portNum), 1);
            return true;
        } catch (IOException ex) {
            System.out.println("Порт занят, попробуйте другой");
            return false;
        }
    }
}
