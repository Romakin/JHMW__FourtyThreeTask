package task2;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class Main {
    public static void main(String[] args) {

        Integer port, maxThreads;
        String directory;
        if (args.length > 0) {
            port = Integer.parseInt(args[1]);
            maxThreads = Integer.parseInt(args[0]);
            directory = args[2];
        } else {
            port = 9989;
            maxThreads = 60;
            directory = "Public";
        }

        Server server = new Server(maxThreads);

        server.addHandler("GET", "/messages", new Handler() {
            @Override
            public void handle(Request request, OutputStream out) throws IOException {
                String resp = getMessages();
                PrintStream ps = new PrintStream(out);
                ps.printf("HTTP/1.1 %s %s%n", 200, "OK")
                        .printf("Content-Type: %s%n", "application/json")
                        .printf("Content-Length:%s%n%n", resp.getBytes().length)
                        .printf("%s%n", resp)
                        .flush();
            }
        });

        server.addHandler("POST", "/messages", new Handler() {
            @Override
            public void handle(Request request, OutputStream out) throws IOException {
                String resp = getMessages();
                PrintStream ps = new PrintStream(out);
                ps.printf("HTTP/1.1 %s %s%n", 200, "OK")
                        .printf("Content-Type: %s%n", "application/json")
                        .printf("Content-Length:%s%n%n", resp.getBytes().length)
                        .printf("%s%n", resp)
                        .flush();
            }
        });

        server.addHandler("GET", "/*", new FileHandlerImpl(directory));

        server.listen(port);
    }

    public static String getMessages() {
        return "[]";
    }
}
