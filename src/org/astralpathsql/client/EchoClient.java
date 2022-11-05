package org.astralpathsql.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EchoClient {
    public static int PORT1 = 9999; 								// 绑定端口
    public static int PORT2 = 9999; 								// 绑定端口
    public static String code = "abcdef20060113";
    public static String CORE = "47.96.154.95";
    public static void main(String[] args) throws Exception {
        try {
            SocketChannel clientChannel1 = SocketChannel.open(); 			// 获取客户端通道
            clientChannel1.connect(new InetSocketAddress(CORE, 13180)); 		// 连接服务端s
            ByteBuffer buffer = ByteBuffer.allocate(10000000); 					// 开辟缓存
            buffer.clear();                                            // 清空缓冲区
            buffer.put(code.getBytes()); 					// 缓冲区保存数据

            buffer.flip();                                            // 重设缓冲区
            clientChannel1.write(buffer);                                // 发送消息
            buffer.clear();                                            // 清空缓冲区
            buffer.flip();                                            // 重置缓冲区
            buffer.clear();                                            // 清空缓冲区
            int readCount = clientChannel1.read(buffer);                // 读取服务端回应
            //System.out.println(readCount);
            buffer.flip();                                            // 重置缓冲区
            clientChannel1.close();										// 关闭通道
            String HOST = new String(buffer.array(), 0, readCount);
            HOST = HOST.replaceFirst(code,"");
            System.out.println(HOST);
            PORT1 = 13159;
            PORT2 = 13160;
            ExecutorService executorService = Executors.newFixedThreadPool(3);
            String finalHOST = HOST;
            executorService.submit(() -> {
                try {
                    String msg = null;
                    SocketChannel clientChannel = SocketChannel.open(); 			// 获取客户端通道
                    clientChannel.connect(new InetSocketAddress(finalHOST, 13159)); 		// 连接服务端
                    System.out.println("[INFO]连接成功!Connected successfully");// 开辟缓存
                    buffer.clear();                                            // 清空缓冲区
                    buffer.flip();                                            // 重置缓冲区
                    boolean flag = true;


                    while (flag) {                                                // 持续输入信息
                        buffer.clear();                                            // 清空缓冲区
                        msg = InputUtil.getString("");    // 提示信息
                        if ("stop".equals(msg)) {                                    // 结束指令
                            flag = false;                                        // 结束循环
                            System.out.println("服务端已暂停！");
                        }
                        if ("restart".equals(msg)) {                                    // 结束指令
                            flag = false;                                        // 结束循环
                            System.out.println("服务端正在重启！");
                        }
                        long start = System.currentTimeMillis();
                        buffer.put(msg.getBytes());                                // 数据保存在缓冲区
                        buffer.flip();                                            // 重设缓冲区
                        clientChannel.write(buffer);                                // 发送消息
                        buffer.clear();                                            // 清空缓冲区
                        int readCount1 = clientChannel.read(buffer);                // 读取服务端回应
                        buffer.flip();                                            // 重置缓冲区
                        String a = new String(buffer.array(), 0, readCount1);
                        System.out.println(a);
                        long end = System.currentTimeMillis();
                        long sd = end - start;
                        System.out.println("[" + sd+ " ms]");
                    }
                    clientChannel.close();										// 关闭通道
                } catch (Exception e) {}

            });
            executorService.submit(() -> {
                try {
                    String msg = null;
                    SocketChannel clientChannel = SocketChannel.open(); 			// 获取客户端通道
                    clientChannel.connect(new InetSocketAddress(finalHOST, 13160)); 		// 连接服务端
                    System.out.println("[INFO]连接成功!Connected successfully");
                    buffer.clear();                                            // 清空缓冲区
                    boolean flag = true;
                    while (flag) {                                                // 持续输入信息
                        buffer.clear();                                            // 清空缓冲区
                        if ("stop".equals(msg)) {                                    // 结束指令
                            flag = false;                                        // 结束循环
                            System.out.println("服务端已暂停！");
                            System.exit(1);
                        }
                        if ("restart".equals(msg)) {                                    // 结束指令
                            flag = false;                                        // 结束循环
                            System.out.println("服务端正在重启！");
                        }
                        buffer.flip();                                            // 重设缓冲区
                        buffer.clear();                                            // 清空缓冲区
                        int readCount1 = clientChannel.read(buffer);                // 读取服务端回应
                        System.out.println(1);
                        buffer.flip();                                            // 重置缓冲区
                        String a = new String(buffer.array(), 0, readCount1);
                        if (a.contains("§")) {
                            System.out.println(1);
                            String res[] = a.split("§");
                            for (int x = 1; x < res.length; x++) {
                                System.out.println(res[x]);
                            }
                        } else {
                            System.out.println(a);
                        }
                        if ("exit".equals(msg)) {                                    // 结束指令
                            flag = false;                                        // 结束循环
                        }
                    }
                    clientChannel.close();										// 关闭通道
                } catch (Exception e) {}

            });
        } catch (Exception e) {
            System.err.println("出现了错误!");
            e.printStackTrace();
            Thread.sleep(1000);
            String msg = InputUtil.getString("按Enter退出",1);	// 提示信息
            System.exit(1);
        }

    }

}
