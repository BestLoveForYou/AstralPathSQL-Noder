package org.astralpathsql.server;

import org.astralpathsql.file.Filer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import static org.astralpathsql.server.MainServer.*;

public class NIOThread implements Runnable {					// 客户端处理线程
        SocketChannel clientChannel; 								// 客户端通道
        private boolean flag = true; 										// 循环标记
        public String uuid;
        private Deque<String> q = new LinkedList<>();
        public void addQ(String q) {
        this.q.offerFirst(q);
    }
        public Integer getCpu() {
            return this.cpu;
        }
        private int cpu ;
        public NIOThread(SocketChannel clientChannel) throws Exception {
            this.clientChannel = clientChannel;							// 保存客户端通道
            String ip = String.valueOf(clientChannel.getRemoteAddress());
            Filer.addIPFile(ip.replaceAll("/","").split(":")[0]);
        }
        @Override
        public void run() {												// 线程任务
            String ip = null;

            try {
                ip = String.valueOf(clientChannel.getRemoteAddress());
                ip = ip.replaceAll("/","").split(":")[0];
                System.out.println(regnode.contains(ip));
                if (regnode.contains(ip)) {
                    System.out.println("节点连接!来源:" + ip);
                } else {
                    this.clientChannel.close(); 								// 关闭通道
                    System.out.println("非法IP连接,来源:" + ip);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ByteBuffer buffer = ByteBuffer.allocate(10000000); 					// 创建缓冲区
            unconIP = unconIP.replaceAll(ip,"");
            try {
                buffer.clear();

                while (this.flag) { 										// 不断与客户端交互
                    // 由于可能重复使用一个Buffer，所以使用之前需要将其做出清空处理
                    buffer.clear();
                    int readCount = this.clientChannel.read(buffer); 		// 接收节点发送数据
                    String readMessage = new String(buffer.array(),
                            0, readCount).trim(); 						// 数据变为字符串
                    //System.out.println(readMessage);
                    this.cpu = Integer.parseInt(readMessage.split("\\|")[0].replaceAll("ping\\(",""));
                    String cmd = readMessage.split("\\|")[2].replaceAll("ping\\)","");
                    StringBuffer buffer1 = new StringBuffer(cmd);
                    cmd = buffer1.reverse().toString();
                    cmd = cmd.replaceFirst("\\)","");
                    buffer1 = new StringBuffer(cmd);
                    cmd = buffer1.reverse().toString();
                        String c[] = cmd.split(" ");
                        if (c[0].equals("+")) {
                            System.out.println(c[1]);
                        }
                        if (c[0].equals("-")) {
                            System.out.println(c[1]);
                        }
                        if (!cmd.equals("null")) {
                            String unip = readMessage.split("\\|")[1];
                            if (!unip.isEmpty()) {
                                String unipa[] = unip.split(",");
                                for (int x = 0; x < unipa.length; x++) {
                                    try {
                                        String old = nioThreadUNCMD.get(unipa[x]);
                                        if (old.equals("null")) {
                                            old = "";
                                        }
                                        nioThreadUNCMD.remove(unipa[x]);
                                        nioThreadUNCMD.put(unipa[x], old + ";" + cmd);
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        }
                    String writeMessage = null;
                    //当 节点之间存在断开时
                    String uncmd = nioThreadUNCMD.get(ip);
                    try {
                        if (!uncmd.isEmpty()) {
                            String unc[] = uncmd.split(";");
                            for (int x = 0 ;x < unc.length ; x ++) {
                                q.offerFirst(unc[x]);
                            }
                            nioThreadUNCMD.remove(ip);
                            nioThreadUNCMD.put(ip,"null");
                        }
                    } catch (Exception e) {

                    }

                    if(!q.isEmpty()) {
                        writeMessage = "ping(null|" + Share + "|" + q.pollLast() + ")";
                    } else {
                        writeMessage = "ping(null|" + Share + "|null)";
                    }


                    buffer.clear(); 										// 清空缓冲区
                    buffer.put(writeMessage.getBytes()); 					// 缓冲区保存数据
                    buffer.flip(); 										// 重置缓冲区

                    this.clientChannel.write(buffer);						// 回应信息
                }
                this.clientChannel.close(); 								// 关闭通道
            } catch (Exception e) {
                try {
                    unconIP = unconIP + String.valueOf(clientChannel.getRemoteAddress()).replaceAll("/","").split(":")[0] + ",";
                    nioThreadHashMap.remove(HashMapIPUUID.get(String.valueOf(clientChannel.getRemoteAddress()).replaceAll("/","").split(":")[0]));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                e.printStackTrace();
            }
        }
}
