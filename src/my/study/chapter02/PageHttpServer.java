package my.study.chapter02;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by wangbeiying on 2017/3/2.
 */
public class PageHttpServer {

    private final String WEB_ROOT = PageHttpServer.class.getResource("").getPath();

    /**
     * 把请求的文件通过socket传给客户端
     *
     * @param page
     * @param socket
     */
    private void doResponseGet(String page, Socket socket) throws IOException {
        //如果page存在，就输出，否则输出page不存在信息。
        File f = new File(WEB_ROOT + File.separator + "page", page+".txt");
        OutputStream out = socket.getOutputStream();
        if(f.exists()){
            InputStream in = new FileInputStream(f);
            byte[] buf = new byte[in.available()];
            in.read(buf);
            in.close();
            out.write(buf);
            out.flush();
            out.close();
        } else {
            String msg = "can't found file,sorry.\r\n";
            StringBuffer sb = new StringBuffer("HTTP/1.1 200 OK\r\n");
            sb.append("Server: file Server/0.1\r\n");
            sb.append("Content-Length:" + msg.length() + "\r\n");
            sb.append("\r\n");
            sb.append(msg);
            out.write(sb.toString().getBytes());
            out.flush();
        }
    }

    public void start() {
        System.out.println("root path:" + WEB_ROOT + File.pathSeparator);
        Socket s = null;
        try {
            ServerSocket ss = new ServerSocket(80);
            while (true) {

                s = ss.accept();
                System.out.println("Request:" + s.toString() + " connected");
                LineNumberReader lr = new LineNumberReader(new InputStreamReader(s.getInputStream()));
                String lineInput;
                String requestPage = null;
                //循环读取每行数据，把request page读取出来，结束后，把请求页面信息返回
                while ((lineInput = lr.readLine()) != null) {
                    System.out.println("line input:" + lineInput);
                    if (lr.getLineNumber() == 1) {
                        requestPage = lineInput.substring(lineInput.indexOf("/") + 1, lineInput.lastIndexOf(' '));
                        System.out.println("request page:" + requestPage);
                    } else {
                        if (lineInput.isEmpty()) {
                            System.out.println("header finish");
                            this.doResponseGet(requestPage, s);
                            break;//跳出header read
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    public static void main(String[] args) {
        PageHttpServer server = new PageHttpServer();
        server.start();
    }
}
