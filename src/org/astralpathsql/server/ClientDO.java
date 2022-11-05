package org.astralpathsql.server;

import org.astralpathsql.file.Filer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;

import static org.astralpathsql.server.MainServer.*;

public class ClientDO implements Runnable {
    private SocketChannel clientChannel; 								// 客户端通道
    private boolean flag = true; 										// 循环标记

    public ClientDO(SocketChannel clientChannel) throws Exception {
        this.clientChannel = clientChannel;							// 保存客户端通道
        String ip = String.valueOf(clientChannel.getRemoteAddress());
    }
    @Override
    public void run() {
        ByteBuffer buffer = ByteBuffer.allocate(10000000); 					// 创建缓冲区
        buffer.clear();

        int readCount = 0; 		// 接收客户端点发送令牌
        try {
            readCount = this.clientChannel.read(buffer);
            String readMessage = new String(buffer.array(),
                    0, readCount).trim(); 						// 数据变为字符串
            if (!readMessage.equals(code)) {
                this.clientChannel.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //System.out.println("成功!");

        String ip = "";

        Iterator<Map.Entry<String, Integer>> entries = nioThreadCPU.entrySet().iterator();
        int c = 101;
        while (entries.hasNext()) {
            Map.Entry<String, Integer> entry = entries.next();
            if (entry.getValue() < c) {
                c = entry.getValue();
                try {
                    ip = String.valueOf(nioThreadHashMap.get(entry.getKey()).clientChannel.getRemoteAddress()).replaceAll("/","").split(":")[0];
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }
        System.out.println(ip);
        buffer.put(ip.getBytes()); 					// 缓冲区保存数据
        buffer.flip(); 										// 重置缓冲区
        try {
            this.clientChannel.write(buffer);						// 回应信息
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
