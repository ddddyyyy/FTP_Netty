package util;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * socket相关的工具类
 */
public class SocketUtil {

    /**
     * @return 可用的端口号，无则返回-1
     */
    public static Integer GetFreePort() {
        Integer port;
        try {
            //读取空闲的可用端口
            ServerSocket socket = new ServerSocket(0);
            port = socket.getLocalPort();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            port = -1;
        }
        return port;
    }
}
