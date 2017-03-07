package my.study.chapter02.myServlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created by wangbeiying on 2017/3/5.
 */
public interface IServlet {
    public void service(Map<String,String> inputMap, OutputStream out)  throws IOException;
}
