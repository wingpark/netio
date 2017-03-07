package my.study.chapter02;

import my.study.chapter02.myServlet.IServlet;
import my.study.chapter02.myServlet.JdkCompiler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 实现一个上传下载的http服务器
 * Created by wangbeiying on 2017/3/2.
 */
public class UpDownloadHttpServer {

    private final String WEB_ROOT = UpDownloadHttpServer.class.getResource("").getPath();

    private final String ATT_ROOT = WEB_ROOT + File.separator + "attachments" + File.separator;

    private final String CLASS_ROOT = WEB_ROOT + File.separator + "tmp_class" + File.separator;

    private final Map<String, IServlet> mspCache = new HashMap<String, IServlet>();

    /**
     * 保存附件
     *
     * @param fileName
     * @param sb
     * @return 成功true ，失败false
     */
    private boolean saveFile(String fileName, StringBuffer sb) {
        File fDir = new File(ATT_ROOT);
        if (!fDir.exists()) {
            try {
                fDir.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File f = new File(ATT_ROOT + fileName);
        System.out.println("----" + f.getAbsolutePath());
        if (f.exists()) {//删除
            f.delete();
        }
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        try (FileWriter fileWriter = new FileWriter(f)) {
            fileWriter.write(sb.toString());
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 首字母大些
     *
     * @param str
     * @return
     */
    private static String upperCaseFirst(String str) {
        char[] ch = str.toCharArray();
        if (ch[0] >= 'a' && ch[0] <= 'z') {
            ch[0] = (char) (ch[0] - 32);
        }
        return new String(ch);
    }

    /**
     * 把msp文件名转为对应的class文件名
     *
     * @param mspName
     * @return
     */
    private static String transMsp2ClzFileName(String mspName) {
        return upperCaseFirst(mspName.replace(".", "_") + ".class");
    }

    /**
     * 转成javat文件名
     * @param mspName
     * @return
     */
    private static String transMsp2javaFileName(String mspName) {
        return upperCaseFirst(mspName.replace(".", "_") + ".javat");
    }

    private static String transMsp2ClzName(String mspName) {
        return upperCaseFirst(mspName.replace(".", "_"));
    }

    /**
     * Get实现展现下载列表和下载方法
     *
     * @param reader
     * @param requestPage
     * @param s
     */
    private void doGet(LineNumberReader reader, String requestPage, Socket s) throws IOException {
        String lineInput = null;
        Map<String, String> params = null;
        String strParams = null;
        int idxQ = 0;
        //封装请求参数
        if ((idxQ = requestPage.indexOf("?")) != -1) {
            params = new HashMap<String, String>();
            strParams = requestPage.substring(idxQ+1, requestPage.length());
            String[] arrParam = strParams.split("&");
            for (String param : arrParam) {
                String[] p = param.split("=");
                params.put(p[0], p[1]);
            }
        }

        IServlet servlet = null;
        if (!mspCache.containsKey("requestPage")) {
            String clzFileName = transMsp2javaFileName(requestPage);
            File mspClass = new File(CLASS_ROOT + clzFileName);
            if (mspClass.exists()) {
                InputStream in = new FileInputStream(mspClass);
                byte[] buf = new byte[in.available()];
                in.read(buf);
                in.close();
                //System.out.println(new String(buf));
                JdkCompiler jdkCompiler = new JdkCompiler();
                Class clazz = null;
                try {
                    clazz = jdkCompiler.doCompile("my.study.chapter02.tmp_class." + transMsp2ClzName(requestPage), new String(buf));
                    servlet = (IServlet) clazz.newInstance();
                    mspCache.put(requestPage, servlet);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                    String msg = "can't handle request " + requestPage + ",sorry.\r\n";
                    printOut(msg, s);
                    return;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    String msg = "can't handle request " + requestPage + ",sorry.\r\n";
                    printOut(msg, s);
                    return;
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    String msg = "can't handle request " + requestPage + ",sorry.\r\n";
                    printOut(msg, s);
                    return;
                }
            } else {
                String msg = "can't handle request " + requestPage + ",sorry.\r\n";
                printOut(msg, s);
                return;
            }
        } else {
            servlet = mspCache.get(requestPage);
        }
        servlet.service(params, s.getOutputStream());
    }

    /**
     * 上传文件，上传完跳转到附件列表
     *
     * @param reader
     * @param requestPage
     * @param s
     */
    private void doPost(LineNumberReader reader, String requestPage, Socket s) throws IOException {
        String lineInput = null;
        int contentLength = 0;
        String contentType = null;
        String boundary = null;
        if (!requestPage.equals("/upload")) {
            String msg = "can't handle request " + requestPage + ",sorry.\r\n";
            printOut(msg, s);
        }
        while ((lineInput = reader.readLine()) != null) {//read header
            System.out.println(lineInput);
            if (lineInput.startsWith("Content-Length:")) {
                contentLength = Integer.valueOf(lineInput.substring(lineInput.indexOf(":") + 1, lineInput.length()).trim());
                System.out.println("contentLength:" + contentLength);
            } else if (lineInput.startsWith("Content-Type:")) {
                contentType = lineInput.substring(lineInput.indexOf(":") + 1, lineInput.indexOf(";")).trim();
                boundary = lineInput.substring(lineInput.indexOf("boundary=") + 9, lineInput.length()).trim();//+9因为跳过boundary=
                System.out.println("contentType:" + contentType);
                System.out.println("boundary:" + boundary);
            } else if (lineInput.isEmpty()) {//跳出读取header
                break;
            }
        }
        boolean readDate = false;
        StringBuffer sbContent = null;
        String cName = null;
        String cFileName = null;
        while ((lineInput = reader.readLine()) != null) {//read content
            System.out.println(lineInput);
            if (lineInput.indexOf(boundary) != -1) {//开始一个multipart的读取周期
                //处理数据,如果是附件类，输出文件，否则，打印key：value
                if (sbContent != null) {
                    if (null != cFileName) {
                        System.out.println("--------fileName:" + cFileName + ";key:" + cName);
                        saveFile(cFileName, sbContent);
                    } else {
                        System.out.println("--------key:" + cName + ";value:" + sbContent.toString());
                    }
                }
                //复位状态位
                readDate = false;
                cName = null;
                cFileName = null;
            } else if (!readDate && lineInput.isEmpty()) {
                readDate = true;//进入读取数据模式
                sbContent = new StringBuffer();
            } else if (lineInput.startsWith("Content-Disposition:")) {
                String[] cDis = lineInput.split("; ");
                for (String s1 : cDis) {
                    if (s1.startsWith("name="))
                        cName = s1.substring(6, s1.length() - 1);
                    else if (s1.startsWith("filename="))
                        cFileName = s1.substring(10, s1.length() - 1);
                }
            } else {
                if (readDate) {
                    if (lineInput.isEmpty()) {
                        sbContent.append("\r\n");
                    } else {
                        sbContent.append(lineInput + "\r\n");
                    }
                } else {
                    System.out.println("---lineInput:" + lineInput);
                }
            }
        }
    }


    /**
     * 返回信息提示
     *
     * @param msg
     * @return
     */
    private String responseMsg(String msg) {
        StringBuffer sb = new StringBuffer("HTTP/1.1 200 OK\r\n");
        sb.append("Server: file Server/0.1\r\n");
        sb.append("Content-Length:" + msg.length() + "\r\n");
        sb.append("\r\n");
        sb.append(msg);
        return sb.toString();
    }

    private void printOut(String msg, Socket s) throws IOException {
        OutputStream out = s.getOutputStream();
        out.write(responseMsg(msg).getBytes());
        out.flush();
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
                            System.out.println(Thread.currentThread().getId() + "-Request:" + s.toString() + " connected");
                            LineNumberReader lr = new LineNumberReader(new InputStreamReader(s.getInputStream()));
                            String lineInput;
                            String method;
                            String requestPage = null;
                            //对第一行进行解析
                            if ((lineInput = lr.readLine()) != null) {
                                System.out.println("line input:" + lineInput);
                                method = lineInput.substring(0, 4).trim();
                                requestPage = lineInput.substring(lineInput.indexOf("/") + 1, lineInput.lastIndexOf(' '));
                                System.out.println("request page:" + requestPage);
                                if (method.equalsIgnoreCase("GET")) {
                                    doGet(lr, requestPage, s);
                                } else if (method.equalsIgnoreCase("POST")) {
                                    doPost(lr, requestPage, s);
                                } else {
                                    //返回不支持的方法
                                    String msg = "can't support method " + method + ",sorry.\r\n";
                                    printOut(msg, s);
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
        UpDownloadHttpServer server = new UpDownloadHttpServer();
        server.start();
    }
}
