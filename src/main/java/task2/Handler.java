package task2;

import java.io.IOException;
import java.io.OutputStream;

public interface Handler {
     void handle(Request request, OutputStream responseStream) throws IOException;
}
