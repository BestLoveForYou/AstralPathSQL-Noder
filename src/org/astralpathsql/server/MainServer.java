package org.astralpathsql.server;

import org.astralpathsql.autoC.DoIT;
import org.astralpathsql.file.Filer;
import org.astralpathsql.properties.ProRead;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.astralpathsql.print.ColorTest.getFormatLogString;

/**
 *
 */
public class MainServer {
    public static String version = "终端v1";
    public static Properties prop = null;
    public static ExecutorService executorService = Executors.newFixedThreadPool(19999999);
    public static ExecutorService writePool = Executors.newFixedThreadPool(100);
    public static Map<String,NIOThread> nioThreadHashMap = new HashMap<>();
    public static Map<String,String> HashMapIPUUID = new HashMap<>();
    public static Map<String,String> nioThreadUNCMD = new HashMap<>();
    public static Map<String,Integer> nioThreadCPU = new HashMap<>();
    public static String unconIP = "";
    public static String Share;
    public static String USER;
    public static String banip;
    public static String regnode;
    public static String code;//这是令牌
    public static int PORT1 = 13170;
    public static int PORT2 = 13180;
    public static int PORT3 = 13181;
    /**
     * @author Saturn
     * AstralPathSQL System
     *
     */
    public static void stopserver() throws Exception {
        System.out.println("[INFO]Server closed");			// 结束消息
        ProRead.write();
        System.exit(0);
    }
    public static void main(String[] args) {
        Map<String,String> arg = new TreeMap<String,String>();
        for (int x = 0; x < args.length; x ++) {
            arg.put(args[x].split("=")[0],args[x].split("=")[1]);
        }
        System.out.println(getFormatLogString("版本:" + version + "\n" + System.getProperty("os.name"),35,4));
        try {
            System.out.println(getFormatLogString("初始化中",34,1));
            System.out.println(getFormatLogString("数据,数据表 加载完成",34,1));
            Filer.createInFirst();
            prop = ProRead.read();//加载配置文件
            try {
                regnode = prop.getProperty("regnode");
                Share = prop.getProperty("Share");
                if (regnode.isEmpty()) {
                    System.out.println("注册节点缺失!");
                }
                code = prop.getProperty("code");
            } catch (Exception e) {
                System.out.println("配置文件缺失!");
                ProRead.crNew();
                System.exit(1);
            }
            executorService.submit(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                System.out.println(getFormatLogString("服务端命令集启动成功!",32,1));
                while (true) {
                    String msg = InputUtil.getString("");	// 提示信息
                    String out = DoIT.doit(msg,"DBA","0.0.0.0");
                    if (out.contains("§")) {
                        String res[] = out.split("§");
                        for (int x = 0;x < res.length ;x ++) {
                            System.out.println(res[x]);
                        }
                    } else {
                        System.out.println(out);
                    }
                    if ("exit".equals(msg)) { 						// 结束指令
                        stopserver();
                    }
                    ProRead.write();
                }
            });
            // 打开一个节点连接点
            System.out.println(getFormatLogString("打开服务端连接通道",34,1));
            executorService.submit(() -> {
                try {
                    Selector selector = null;
                    ServerSocketChannel serverSocketChannel = null;
                    Set<SelectionKey> selectedKeys = null; 	// 获取全部连接通道
                    Iterator<SelectionKey> selectionIter = null;
                    serverSocketChannel = ServerSocketChannel.open();
                    serverSocketChannel.configureBlocking(false); 					// 设置非阻塞模式
                    serverSocketChannel.bind(new InetSocketAddress(PORT1)); 			// 服务绑定端口
                    // 打开一个选择器，随后所有的Channel都要注册到此选择器之中
                    selector = Selector.open();
                    // 将当前的ServerSocketChannel统一注册到Selector之中，接受统一的管理
                    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                    /**
                     *这里写一些需要IO操作的操作
                     */
                    USER = Filer.readUser();
                    Filer.checkIP();
                    long end = System.currentTimeMillis();
                    writePool.submit(() -> {
                        try {
                            Filer.writeLog("Server started!port:" + PORT1,"\nServer");
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    // 所有的连接处理都需要被Selector所管理，也就是说只要有新的用户连接，那么就通过Selector处理
                    int keySelect = 0; 											// 接收连接状态
                    while ((keySelect = selector.select()) > 0) { 					// 持续等待连接
                        selectedKeys = selector.selectedKeys();
                        selectionIter = selectedKeys.iterator();
                        while (selectionIter.hasNext()) {
                            SelectionKey selectionKey = selectionIter.next(); 		// 获取每一个通道
                            if (selectionKey.isAcceptable()) { 					// 模式为接收连接模式
                                SocketChannel clientChannel = serverSocketChannel.accept(); // 等待接收
                                if (clientChannel != null) { 						// 已经有了连接
                                    NIOThread a =new NIOThread(clientChannel);
                                    executorService.submit(a);
                                    String b = String.valueOf(UUID.randomUUID());
                                    a.uuid = b;
                                    nioThreadHashMap.put(b,a);
                                    nioThreadCPU.put(b,0);
                                    HashMapIPUUID.put(String.valueOf(clientChannel.getRemoteAddress()).replaceAll("/","").split(":")[0],b);
                                }
                            }
                            selectionIter.remove(); 								// 移除掉此通道
                        }
                    }
                    executorService.shutdown();									// 关闭线程池
                    serverSocketChannel.close();									// 关闭服务端通道
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            });
            executorService.submit(() -> {
                try {
                    Selector selector = null;
                    ServerSocketChannel serverSocketChannel = null;
                    Set<SelectionKey> selectedKeys = null; 	// 获取全部连接通道
                    Iterator<SelectionKey> selectionIter = null;
                    serverSocketChannel = ServerSocketChannel.open();
                    serverSocketChannel.configureBlocking(false); 					// 设置非阻塞模式
                    serverSocketChannel.bind(new InetSocketAddress(PORT2)); 			// 服务绑定端口
                    // 打开一个选择器，随后所有的Channel都要注册到此选择器之中
                    selector = Selector.open();
                    // 将当前的ServerSocketChannel统一注册到Selector之中，接受统一的管理
                    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                    /**
                     *这里写一些需要IO操作的操作
                     */
                    writePool.submit(() -> {
                        try {
                            Filer.writeLog("Server started!port:" + PORT2,"\nServer");
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    // 所有的连接处理都需要被Selector所管理，也就是说只要有新的用户连接，那么就通过Selector处理
                    int keySelect = 0; 											// 接收连接状态
                    while ((keySelect = selector.select()) > 0) { 					// 持续等待连接
                        selectedKeys = selector.selectedKeys();
                        selectionIter = selectedKeys.iterator();
                        while (selectionIter.hasNext()) {
                            SelectionKey selectionKey = selectionIter.next(); 		// 获取每一个通道
                            if (selectionKey.isAcceptable()) { 					// 模式为接收连接模式
                                SocketChannel clientChannel = serverSocketChannel.accept(); // 等待接收
                                if (clientChannel != null) { 						// 已经有了连接
                                    executorService.submit(new ClientDO(clientChannel));
                                }
                            }
                            selectionIter.remove(); 								// 移除掉此通道
                        }
                    }
                    executorService.shutdown();									// 关闭线程池
                    serverSocketChannel.close();									// 关闭服务端通道
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            });
            executorService.submit(() -> {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        Iterator<Map.Entry<String, NIOThread>> entries = nioThreadHashMap.entrySet().iterator();
                        while (entries.hasNext()) {
                            Map.Entry<String, NIOThread> entry = entries.next();
                            String key = entry.getKey();
                            NIOThread value = entry.getValue();
                            nioThreadCPU.remove(key);
                            nioThreadCPU.put(key,value.getCpu());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });

            String a[] = regnode.split(",");
            for (int x = 0 ; x < a.length ; x ++) {
                nioThreadUNCMD.put(a[x],"null");
            }
        } catch (NumberFormatException e) {
            System.out.println("启动参数缺失！");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
