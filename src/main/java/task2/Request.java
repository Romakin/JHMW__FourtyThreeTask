package task2;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;

public class Request {

    private static final String INDEX_FILE_NAME = "index.html";
    private static final String DEFAULT_UPLOAD_PATH = "Public/";
    private String protocol;
    private String method;
    private String path;
    private String fullUrl;
    private Map<String, String> headers;
    private Map<String, Object> queryParameters;
    private byte[] body;
    private BufferedReader in;

    public Request(BufferedReader in) {
        this.in = in;
        queryParameters = new HashMap<>();
        headers = new HashMap<>();
    }

    public String getProtocol() {
        return protocol;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getFullUrl() {
        return fullUrl;
    }

    public String getHeaders(String headerName) {
        return headers.get(headerName);
    }

    public Map<String, Object> getQueryParams()  {
        return queryParameters;
    }

    public Object getQueryParam(String parName) { return queryParameters.get(parName); }

    public Object getPostParam(String name) { return getQueryParam(name); }
    public Map<String, Object> getPostParams() { return getQueryParams(); }

    public boolean parse() throws IOException {
        String firstLine = in.readLine();
        System.out.println(firstLine);
        String[] components = firstLine.split("[\\s\\t]");
        method = components[0];
        fullUrl = components[1];
        protocol = components[2];
        if (setHeaders()) return false;
        setPath();
        return true;
    }

    private boolean setHeaders() throws IOException {
        while (true) {
            String line = in.readLine();
            if (line.length() == 0) break;
            int ind = line.indexOf(":");
            if (ind == -1) return true;
            headers.put(line.substring(0, ind), line.substring(ind + 1).trim());
        }
        return false;
    }

    private boolean setPath() {
        try {
            URI uri = new URI(fullUrl);
            path = fullUrl.endsWith("/") ? fullUrl + INDEX_FILE_NAME : fullUrl;
            if ("GET".equals(method)) setGetParams(uri);
            if ("POST".equals(method)) if (!setPostParams()) return false;
            return true;
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    private boolean setPostParams() throws IOException, URISyntaxException {
        StringBuilder sb = new StringBuilder();
        String encodeStr;
        if (headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
            if (ContentType.APPLICATION_FORM_URLENCODED.getMimeType()
                    .equals(headers.get(HttpHeaders.CONTENT_TYPE))) {
                while ((encodeStr = in.readLine()) != null) {
                    if ("".equals(encodeStr.trim())) break;
                    sb.append(encodeStr);
                }
                setGetParams(new URI(path + "?" + sb.toString()));
                return true;
            }
            if (ContentType.MULTIPART_FORM_DATA.getMimeType()
                    .equals(headers.get(HttpHeaders.CONTENT_TYPE).split(";")[0])) {

                String boundaryName = headers.get(HttpHeaders.CONTENT_TYPE).split(";")[1].trim().split("=")[1];
                DiskFileItem fileItem = null;
                OutputStream outputStream = null;
                boolean breakOnEnd = false;
                while ((encodeStr = in.readLine()) != null) {
                    if (("--" + boundaryName + "--").equals(encodeStr)) break;
                    if (("--" + boundaryName).equals(encodeStr)) continue;
                        int ind = encodeStr.indexOf(":");
                        if (ind == -1) continue;
                        String attrName = null, attrFilename = null;
                        List<String> attrs = Arrays.asList(encodeStr.substring(ind).split(";\\s"));
                        for (String attr: attrs) {
                            ind = attr.indexOf("=");
                            if (ind == -1) continue;
                            if (attr.substring(0, ind).equals("name"))
                                attrName = attr.substring(ind + 1);
                            if (attr.substring(0, ind).equals("filename"))
                                attrFilename = attr.substring(ind + 1).replaceAll("\\\"", "");
                        }
                        if (attrFilename != null) {
                            String contentType = in.readLine().split(":\\s")[1];
                            if (fileItem == null) {
                                fileItem = new DiskFileItem(
                                        attrName,
                                        contentType,
                                        queryParameters.size() > 0,
                                        attrFilename, 0, new File(DEFAULT_UPLOAD_PATH));
                                outputStream = fileItem.getOutputStream();
                            }
                            breakOnEnd = processFileItem(boundaryName, outputStream, breakOnEnd);
                        } else if(attrName != null) {
                            breakOnEnd = processParam(boundaryName, breakOnEnd, attrName);
                        }
                    if (breakOnEnd) break;
                }
                if (outputStream != null) {
                    createFileFromItem(fileItem, outputStream);
                }

            }
        }

        return false;
    }

    private boolean processFileItem(String boundaryName, OutputStream outputStream, boolean breakOnEnd) throws IOException {
        in.readLine(); //empty string between head and body
        String buff = null;
        boolean breaker = false;
        while (!breaker && (buff = in.readLine()) != null) {
            if (("--" + boundaryName).equals(buff)||
                    ("--" + boundaryName + "--").equals(buff)) breaker = true;
            else outputStream.write((buff + "\n").getBytes());
        }
        if (("--" + boundaryName + "--").equals(buff)) breakOnEnd = true;
        return breakOnEnd;
    }

    private boolean processParam(String boundaryName, boolean breakOnEnd, String attrName) throws IOException {
        in.readLine(); //empty string between head and body
        StringBuilder body = new StringBuilder();
        String buff = null;
        boolean breaker = false;
        while (!breaker && (buff = in.readLine()) != null) {
            if (("--" + boundaryName).equals(buff)||
                    ("--" + boundaryName + "--").equals(buff)) breaker = true;
            else body.append(buff + "\n");
        }
        if (("--" + boundaryName + "--").equals(buff)) breakOnEnd = true;
        if (queryParameters.containsKey(attrName)) {
            Object qPar = queryParameters.get(attrName);
            if(qPar instanceof String) {
                String finalBody = body.toString();
                List<String> pqsVals = new ArrayList<String>() {{
                    add((String) qPar);
                    add(finalBody);
                }};
                queryParameters.replace(attrName, pqsVals);
            }
        }
        return breakOnEnd;
    }

    private void createFileFromItem(DiskFileItem fileItem, OutputStream outputStream) throws IOException {
        outputStream.flush(); // This actually causes the bytes to be written.
        outputStream.close();
        System.out.println(fileItem.getName());
        File file = new File(fileItem.getStoreLocation().getParent() + "/" + fileItem.getName());
        if (file.exists()) file = new File(
                fileItem.getStoreLocation().getParent() + "/"
                        + "(" + System.currentTimeMillis() + ") "
                        + fileItem.getName()
        );
        try {
            fileItem.write(file);
            fileItem.getStoreLocation().delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setGetParams(URI uri) throws URISyntaxException {
        List<NameValuePair> nameValuePairs = URLEncodedUtils.parse(uri, Charset.forName("utf-8"));
        for (NameValuePair nameValuePair : nameValuePairs)
            queryParameters.put(nameValuePair.getName(), nameValuePair.getValue());
    }

}
