package org.astralpathsql.been;

import com.sun.management.OperatingSystemMXBean;
import org.astralpathsql.autoC.DoIT;
import org.astralpathsql.server.NIOThread;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Deque;
import java.util.LinkedList;

import static org.astralpathsql.server.MainServer.*;

public class CORETHREAD {
    public SocketChannel clientChannel;
    public static Deque<String> q = new LinkedList<>();
    private static OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    public static int cpuLoad() {

        double cpuLoad = osmxb.getCpuLoad();

        int percentCpuLoad = (int) (cpuLoad * 100);

        return percentCpuLoad;

    }
    public void up() throws IOException, InterruptedException {
        ByteBuffer buffer = ByteBuffer.allocate(10000000); 					// 开辟缓存

        while (true) {
            try {
                Thread.sleep(1000);
                String i = "ping(" + cpuLoad() + "|" + downNode + "|" + q.pollLast() + ")";
                buffer.clear();                                            // 清空缓冲区
                buffer.flip();                                            // 重置缓冲区
                boolean flag = true;         // 持续输入信息
                buffer.clear();// 清空缓冲区

                buffer.put(i.getBytes());                                // 数据保存在缓冲区
                buffer.flip();                                            // 重设缓冲区
                this.clientChannel.write(buffer);                                // 发送消息
                buffer.clear();                                            // 清空缓冲区
                int readCount = this.clientChannel.read(buffer);                // 读取服务端回应
                buffer.flip();                                            // 重置缓冲区
                String a = new String(buffer.array(), 0, readCount);
                String res[] = a.replaceAll("pong\\(","").split("|");
                ShareDatabase = a.split("\\|")[1];
                String sp[] = a.split("\\|");
                if (sp[2].equals("\\)")) {

                } else {
                    sp[2] = sp[2].replaceAll("\\)","");
                    DoIT.doit(sp[2],"DBA","127.0.0.1");
                }
            } catch (Exception e) {
                NIOThread.fla = false;
                System.out.println("与终端连接断开,正在尝试重连 ; 注意:所有客户端连接都已关闭!");
                connectcore(0);
            }
        }

    }
}
