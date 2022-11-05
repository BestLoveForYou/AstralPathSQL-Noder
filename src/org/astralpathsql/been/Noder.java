package org.astralpathsql.been;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Noder {
    public SocketChannel clientChannel;
    public int nodecmd(String msg) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(10000000); 					// 开辟缓存
        buffer.clear();                                            // 清空缓冲区
        buffer.flip();                                            // 重置缓冲区
        boolean flag = true;         // 持续输入信息
        buffer.clear();// 清空缓冲区
        System.out.println(msg);
        buffer.put(msg.getBytes());                                // 数据保存在缓冲区
        buffer.flip();                                            // 重设缓冲区
        clientChannel.write(buffer);                                // 发送消息
        buffer.clear();                                            // 清空缓冲区
        int readCount = clientChannel.read(buffer);                // 读取服务端回应
        buffer.flip();                                            // 重置缓冲区
        String a = new String(buffer.array(), 0, readCount);
        System.out.println(a);
        return Integer.parseInt(a);
    }
}
