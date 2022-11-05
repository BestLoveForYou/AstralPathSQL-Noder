package org.astralpathsql.server;

import org.astralpathsql.autoC.ClassInstanceFactory;
import org.astralpathsql.autoC.DoIT;
import org.astralpathsql.autoC.Hash;
import org.astralpathsql.autoC.form.Table;
import org.astralpathsql.been.COREINFORMATION;
import org.astralpathsql.been.User;
import org.astralpathsql.file.Filer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;

import static org.astralpathsql.file.Filer.checkIP;
import static org.astralpathsql.server.MainServer.*;

public class NIOThread implements Runnable {					// 客户端处理线程
        private SocketChannel clientChannel; 								// 客户端通道
        private boolean flag = true; 										// 循环标记

    public int getSt() {
        return st;
    }

    public void setSt(int st) {
        this.st = st;
    }

    private int st = 0;
        private Deque<String> q = new LinkedList<>();

    public void addQ(String q) {
        this.q.offerFirst(q);
    }
    public static boolean fla = true;

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
                //这条在企业版无用,只是没有删罢了
                if (banip.contains(ip)) {
                    this.clientChannel.close(); 								// 关闭通道
                    now_Connect --;
                    System.out.println("拦截了一次黑名单连接！IP来源:" + ip);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ByteBuffer buffer = ByteBuffer.allocate(10000000); 					// 创建缓冲区
            try {
                buffer.clear();
                if (st == 1) {
                    /*
                    负责回应客户端的信息
                     */
                    while (this.flag) { 										// 不断与客户端交互
                        // 由于可能重复使用一个Buffer，所以使用之前需要将其做出清空处理
                        buffer.clear();
                        try {
                            Thread.sleep(500);
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                        if (q.isEmpty()) {

                        } else {
                            System.out.println(1);
                            String writeMessage = q.pollLast();
                            System.out.println(writeMessage);
                            buffer.clear();                                        // 清空缓冲区
                            buffer.put(writeMessage.getBytes());                    // 缓冲区保存数据
                            buffer.flip();                                        // 重置缓冲区
                            this.clientChannel.write(buffer);                        // 回应信息
                        }

                        if(!fla) {
                            this.clientChannel.close();
                        }
                    }
                } else if (st == 0) {
                    /*
                    负责收集客户端连接的信息
                     */
                    while (this.flag) { 										// 不断与客户端交互
                        // 由于可能重复使用一个Buffer，所以使用之前需要将其做出清空处理
                        buffer.clear();
                        int readCount = this.clientChannel.read(buffer); 		// 接收客户端发送数据
                        String readMessage = new String(buffer.array(),
                                0, readCount).trim(); 						// 数据变为字符串

                        String writeMessage = DoIT.doit(readMessage,"ghest",ip);
                        if (readMessage.equals("exit")) {
                            this.flag = false;
                            this.clientChannel.close();
                            conpool.remove(clientChannel.getRemoteAddress().toString().split(":")[0]);
                        }
                        UUID a= UUID.randomUUID();
                        writeMessage = a + "§" + writeMessage;
                        //System.out.println(1);
                        NIOThread conn = conpool.get(clientChannel.getRemoteAddress().toString().split(":")[0]);
                        conn.addQ(writeMessage);

                        save();
                        buffer.clear(); 										// 清空缓冲区
                        buffer.put(String.valueOf(a).getBytes()); 					// 缓冲区保存数据
                        buffer.flip(); 										// 重置缓冲区
                        this.clientChannel.write(buffer);						// 回应信息

                        if(!fla) {
                            this.clientChannel.close();
                        }
                    }
                } else { //st == 3
                    /*
                    这里是节点互联中负责处理互联的节点的指令的
                     */
                    System.out.println(1);
                    try {
                        while (this.flag) { 										// 不断与客户端交互
                            // 由于可能重复使用一个Buffer，所以使用之前需要将其做出清空处理
                            buffer.clear();
                            int readCount = this.clientChannel.read(buffer); 		// 接收客户端发送数据
                            String readMessage = new String(buffer.array(),
                                    0, readCount).trim(); 						// 数据变为字符串
                            String cmd[] = readMessage.split(" ");
                            String writeMessage = "-1";
                            System.out.println(readMessage);
                            if (cmd[0].equals("+")) {
                                COREINFORMATION c = ClassInstanceFactory.create(COREINFORMATION.class, cmd[1]) ;	// 工具类自动设置
                                if (Mtree.containsKey(c.getTable())) {
                                    Mtree.get(c.getTable()).add(c);
                                } else {
                                    tree.add(c);
                                }
                                writeMessage = "1";
                            } if (cmd[0].equals("-")) {
                                writeMessage = DoIT.doit(readMessage.replaceAll("- ",""),"ADMIN",ip);
                            }
                            save();
                            buffer.clear(); 										// 清空缓冲区
                            buffer.put(writeMessage.getBytes()); 					// 缓冲区保存数据
                            buffer.flip(); 										// 重置缓冲区
                            this.clientChannel.write(buffer);						// 回应信息

                            if(!fla) {
                                this.clientChannel.close();
                            }
                        }
                    } catch (Exception e) {
                        downNode = downNode + String.valueOf(clientChannel.getRemoteAddress()).replaceAll("/","").split(":")[0] + ",";
                    }

                }

                this.clientChannel.close(); 								// 关闭通道
            } catch (Exception e) {
                System.out.println(1);
            }
        }
}
