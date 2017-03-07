package my.study.chapter02;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * cookie的实现,实现一个基于多线程的socket服务端
 * Created by wangbeiying on 2017/3/2.
 */
public class CookieHttpThreadPoolServer {

    private final String WEB_ROOT = CookieHttpThreadPoolServer.class.getResource("").getPath();

    /**
     * 把请求的文件通过socket传给客户端
     *
     * @param page
     * @param socket
     */
    private void doResponseGet(String page, Socket socket, String userInfo) throws IOException {
        //如果page存在，就输出，否则输出page不存在信息。
        File f = new File(WEB_ROOT + File.separator + "page", page + ".txt");
        System.out.println("file:"+f.getAbsolutePath());
        OutputStream out = socket.getOutputStream();
        if (f.exists()) {
            System.out.println("file:"+f.getAbsolutePath());
            InputStream in = new FileInputStream(f);
            byte[] buf = new byte[in.available()];
            in.read(buf);
            in.close();
            out.write(buf);
            out.flush();
            out.close();
        } else {
            String msg = "can't found file,sorry.\r\n" + ((userInfo == null) ? "new user." : userInfo);
            StringBuffer sb = new StringBuffer("HTTP/1.1 200 OK\r\n");
            sb.append("Server: file Server/0.1\r\n");
            if (userInfo == null) {
                this.genCookieHeader(sb, userInfo);
            }
            sb.append("Content-Length:" + msg.length() + "\r\n");
            sb.append("\r\n");
            sb.append(msg);
            out.write(sb.toString().getBytes());
            out.flush();

            //out.close();
        }
    }

    /**
     * 生成cookieHeader
     *
     * @param userInfo
     * @param sb
     */
    private void genCookieHeader(StringBuffer sb, String userInfo) {
        sb.append("Set-Cookie: jsessionid=" + System.currentTimeMillis() + ".mySession; domain=localhost\r\n");
        sb.append("Set-Cookie: autologin=true; domain=localhost\r\n");
    }

    public void start() {
        System.out.println("root path:" + WEB_ROOT + File.pathSeparator);
        try {
            ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
            ServerSocket ss = new ServerSocket(80);
            while (true) {

                Socket s = ss.accept();
                cachedThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            System.out.println(Thread.currentThread().getId()+"-Request:" + s.toString() + " connected");
                            LineNumberReader lr = new LineNumberReader(new InputStreamReader(s.getInputStream()));
                            String lineInput;
                            String requestPage = null;
                            String userInfo = null;
                            //循环读取每行数据，把request page读取出来，结束后，把请求页面信息返回
                            while ((lineInput = lr.readLine()) != null) {
                                System.out.println("line input:" + lineInput);
                                if (lr.getLineNumber() == 1) {
                                    requestPage = lineInput.substring(lineInput.indexOf("/") + 1, lineInput.lastIndexOf(' '));
                                    System.out.println("request page:" + requestPage);
                                } else {
                                    if (lineInput.startsWith("Cookie:")) {
                                        userInfo = lineInput;
                                        System.out.println("userInfo:" + lineInput);
                                    } else if (lineInput.isEmpty()) {
                                        System.out.println("header finish");
                                        doResponseGet(requestPage, s, userInfo);
                                        break;//跳出header read
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
                });

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }


    }

    public static void main(String[] args) {
        CookieHttpThreadPoolServer server = new CookieHttpThreadPoolServer();
        server.start();
    }
}
