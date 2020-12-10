package task2;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileHandlerImpl implements Handler {

    private final String workingDirectory;

    private static final Map<Integer, String> MESSAGES = new HashMap<Integer, String>() {{
        put( HttpURLConnection.HTTP_NOT_FOUND, "NOT FOUND");
    }};

    private static final List<String> FILES_WITH_TEMPLATES = new ArrayList<String>() {{
        add("classic.html");
    }};

    public FileHandlerImpl(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public void handle(Request request, OutputStream responseStream) throws IOException {
        int code = HttpURLConnection.HTTP_OK;

        String url = request.getPath().substring(1);
        Path filePath = Paths.get(this.workingDirectory, url);
        if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
            String type = Files.probeContentType(filePath);
            byte[] fileBytes = processFileAsTemplate(filePath);
            long length = fileBytes != null ? fileBytes.length : Files.size(filePath);
            this.sendHeader(responseStream, code, "OK", type, length);
            if (fileBytes != null) {
                responseStream.write(fileBytes);
            } else  {
                Files.copy(filePath, responseStream);
            }
        } else {
            // NOT FOUND
            code = HttpURLConnection.HTTP_NOT_FOUND;
            String msg = MESSAGES.get(code);
            this.sendHeader(responseStream, code, msg, "text/plain", msg.length());
            responseStream.write(msg.getBytes());
        }
    }

    private byte[] processFileAsTemplate(Path filePath) throws IOException {
        if (FILES_WITH_TEMPLATES.contains(filePath.getFileName().toString())) {
            List<String> lines = Files.readAllLines(filePath);
            StringBuilder sb = new StringBuilder();
            for (String line: lines) {
                sb.append(line.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ));
            }
            return sb.toString().getBytes();
        } else {
            return null;
        }
    }

    private void sendHeader(OutputStream out, int statusCode, String statusText, String type, long length) {
        PrintStream ps = new PrintStream(out);
        ps.printf("HTTP/1.1 %s %s%n", statusCode, statusText);
        ps.printf("Content-Type: %s%n", type);
        ps.print("Connection: close%n");
        ps.printf("Content-Length:%s%n%n", length);
    }

}
