package task2;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.util.Map;

class SocketHandler implements Runnable{

    private final Socket socket;
    private Map<String, Map<String, Handler>> handlers;

    public SocketHandler(Socket socket, Map<String, Map<String, Handler>> handlers) {
        this.socket = socket;
        this.handlers = handlers;
    }

    @Override
    public void run() {
        try(
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                OutputStream out = socket.getOutputStream()
        ) {
            Request req = new Request(in);
            if (!req.parse()) {
                respond(HttpURLConnection.HTTP_BAD_GATEWAY, "Con not parse request", out);
                return;
            }

            Map<String, Handler> handlerMethods = handlers.get(req.getMethod());
            if (handlerMethods == null) {
                respond(HttpURLConnection.HTTP_BAD_METHOD, "Method no supported", out);
                return;
            }
            boolean handlerExists = false;
            for (String hPath: handlerMethods.keySet())
                if (req.getPath().equals(hPath)){
                    handlerMethods.get(hPath).handle(req, out);
                    handlerExists = true;
                    break;
                }
            if (!handlerExists)
                if (handlerMethods.get("/*") != null)
                    handlerMethods.get("/*").handle(req, out);
                else respond(HttpURLConnection.HTTP_NOT_FOUND, "Not Found", out);
        } catch (IOException e) {
            try {
                e.printStackTrace();
                if (!socket.isOutputShutdown()) {
                    respond(HttpURLConnection.HTTP_SERVER_ERROR, e.toString(), socket.getOutputStream());
                }
            } catch (IOException e2)  {
                e2.printStackTrace();
                // We tried
            }
        }
    }

    private void log(String msg)  {
        System.out.println(msg);
    }

    private void respond(int statusCode, String message, OutputStream out) {
        PrintStream ps = new PrintStream(out);
        ps.printf("HTTP/1.1 %s %s%n", statusCode, message)
            .printf("Content-Type: %s%n", "text/plain")
            .printf("Connection: %s%n", "close")
            .printf("Content-Length:%s%n%n", message.getBytes().length)
                .printf("Connection: %s%n", message)
            .flush();
    }
}
