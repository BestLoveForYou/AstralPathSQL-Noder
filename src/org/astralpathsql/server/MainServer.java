package org.astralpathsql.server;

import org.astralpathsql.autoC.DoIT;
import org.astralpathsql.autoC.form.Table;
import org.astralpathsql.autoC.form.TreeSearch;
import org.astralpathsql.been.CORETHREAD;
import org.astralpathsql.been.Cache;
import org.astralpathsql.been.COREINFORMATION;
import org.astralpathsql.been.Noder;
import org.astralpathsql.client.InputUtil;
import org.astralpathsql.file.Add;
import org.astralpathsql.file.Filer;
import org.astralpathsql.properties.ProRead;
import org.astralpathsql.node.BalancedBinaryTree;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.StampedLock;

import static org.astralpathsql.print.ColorTest.getFormatLogString;

/**
 *
 */
public class MainServer {
    public static String version = "节点至强版";
    public static BalancedBinaryTree<COREINFORMATION> tree = new BalancedBinaryTree<COREINFORMATION>();
    public static StampedLock lock = new StampedLock();
    public static Integer now_Connect = 0; //目前连接数
    public static Integer all_Connect = 0;//历史连接数
    public static Selector selector = null;
    public static ServerSocketChannel serverSocketChannel = null;
    public static Set<SelectionKey> selectedKeys = null; 	// 获取全部连接通道
    public static Iterator<SelectionKey> selectionIter = null;
    public static Cache<Integer, COREINFORMATION> cache = new Cache<>();
    public static Properties prop = null;
    public static Map<String,Table> ta = new HashMap<>();
    public static ExecutorService executorService = Executors.newFixedThreadPool(19999999);
    public static ExecutorService writePool = Executors.newFixedThreadPool(100);
    public static Map<String,BalancedBinaryTree<COREINFORMATION>> Mtree = new HashMap<>();
    public static Map<String,NIOThread> conpool = new HashMap<>();
    public static String ShareDatabase;
    public static String USER;
    public static String banip;
    public static File database;
    public static File table;
    public static boolean flag = true;
    public static int PORT1 = 13159;
    public static int PORT2 = 13160;
    public static int PORT3 = 13161;
    public static Map<String, Noder> noder_pool = new HashMap<>();
    public static String node;
    public static String core;
    public static String downNode = "";
    /**
     * @author Saturn
     * AstralPathSQL System
     *
     */
    public static void stopserver() throws Exception {
        Filer.writeSQL();
        ProRead.write();
        Table.write();
        System.exit(0);
        System.out.println("[INFO]Server closed");			// 结束消息
        executorService.shutdown();									// 关闭线程池
        serverSocketChannel.close();									// 关闭服务端通道
        selector.close();

    }
    public static void save() throws Exception {
        Filer.writeSQL();
        ProRead.write();
        Table.write();
    }
    public static void main(String[] args) {
        Map<String,String> arg = new TreeMap<String,String>();
        for (int x = 0; x < args.length; x ++) {
            arg.put(args[x].split("=")[0],args[x].split("=")[1]);
        }
        System.out.println(getFormatLogString("版本:" + version + "\n" + System.getProperty("os.name"),35,4));
        try {
            Thread.sleep(1000);
            long start = System.currentTimeMillis();
            System.out.println(getFormatLogString("初始化中",34,1));
            Filer.createInFirst();//对象保存文件
            Table.read();//生成表
            TreeSearch.load();

            prop = ProRead.read();//加载配置文件
            try {
                node = prop.getProperty("noder");
                core = prop.getProperty("corer");
                ShareDatabase = prop.getProperty("sd");
            } catch (Exception e) {
                ProRead.crNew();
            }

            System.out.println(core);
            System.out.println(getFormatLogString("数据,数据表 加载完成",34,1));

            System.out.println(getFormatLogString("平衡二叉树加载中",34,1));
            tree = Add.addin(tree);//二叉树加载
            System.out.println(getFormatLogString("成功!",32,1));
            System.out.println(getFormatLogString("线程池加载中",34,1));
            System.out.println(getFormatLogString("成功!",32,1));
            try {
                all_Connect = Integer.valueOf(prop.getProperty("all_connect"));
            } catch (Exception e) {
                all_Connect = 0;
            }

            executorService.submit(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                System.out.println(getFormatLogString("服务端命令集启动成功!",32,1));
                while (true) {
                    String msg = InputUtil.getString(">");	// 提示信息
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
                }
            });
            // 打开一个服务端的Socket的连接通道
            System.out.println(getFormatLogString("打开服务端连接通道",34,1));
            //这是客户端信息发送通道
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
                                    now_Connect ++;
                                    all_Connect ++;
                                    NIOThread a =new NIOThread(clientChannel);
                                    executorService.submit(a);
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
            //这是客户端信息接受通道
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
                                    now_Connect ++;
                                    all_Connect ++;
                                    NIOThread a = new NIOThread(clientChannel);
                                    a.setSt(1);
                                    executorService.submit(a);
                                    conpool.put(clientChannel.getRemoteAddress().toString().split(":")[0],a);
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
            //这是节点互联
            executorService.submit(() -> {
                try {
                    Selector selector = null;
                    ServerSocketChannel serverSocketChannel = null;
                    Set<SelectionKey> selectedKeys = null; 	// 获取全部连接通道
                    Iterator<SelectionKey> selectionIter = null;
                    serverSocketChannel = ServerSocketChannel.open();
                    serverSocketChannel.configureBlocking(false); 					// 设置非阻塞模式
                    serverSocketChannel.bind(new InetSocketAddress(PORT3)); 			// 服务绑定端口
                    // 打开一个选择器，随后所有的Channel都要注册到此选择器之中
                    selector = Selector.open();
                    // 将当前的ServerSocketChannel统一注册到Selector之中，接受统一的管理
                    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                    /**
                     *这里写一些需要IO操作的操作
                     */
                    writePool.submit(() -> {
                        try {
                            Filer.writeLog("Server started!port:" + PORT3,"\nServer");
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
                                    NIOThread a = new NIOThread(clientChannel);
                                    executorService.submit(a);
                                    a.setSt(3);
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
            try {
                if (node.equals("null")) {
                    System.out.println("不存在节点!");
                } else {
                    String nodea[] = node.split(",");
                    for (int x = 0; x < nodea.length; x++) {
                        try {
                            Noder n = new Noder();
                            n.clientChannel = SocketChannel.open(); 			// 获取客户端通道
                            n.clientChannel.connect(new InetSocketAddress(nodea[x], 13161));        // 连接服务端
                            noder_pool.put(nodea[x], n);
                        } catch (Exception e) {
                            downNode = downNode + nodea[x] + ",";
                        }
                    }
                }
            } catch (Exception e) {

            }
            executorService.submit(() -> {
                while (true) {
                    Thread.sleep(500);
                    if(downNode.equals("")) {
                        Thread.sleep(1000);
                    } else {
                        String wait[] = downNode.split(",");
                        for (int x = 0 ; x < wait.length;x++) {
                            try {
                                Noder n = new Noder();
                                n.clientChannel = SocketChannel.open(); 			// 获取客户端通道
                                n.clientChannel.connect(new InetSocketAddress(wait[x], 13161));        // 连接服务端
                                noder_pool.put(wait[x], n);
                                downNode = downNode.replaceAll(wait[x] + ",","");
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            });
            //这里之所以不使用connectcore()方法,是因为首次连接与掉线重连不同!
            CORETHREAD c = new CORETHREAD();
            c.clientChannel = SocketChannel.open(); 			// 获取客户端通道
            c.clientChannel.connect(new InetSocketAddress(core,13170));
            c.up();
        } catch (IllegalArgumentException e) {
            System.err.println("终端不存在");
            ProRead.crNew();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public static void connectcore(int t) throws IOException, InterruptedException {
        try {
            CORETHREAD c = new CORETHREAD();
            c.clientChannel = SocketChannel.open(); 			// 获取客户端通道
            c.clientChannel.connect(new InetSocketAddress(core,13170));
            System.out.println("重连成功!共掉线: " + t*3 + "s");
            c.up();

        }catch (Exception e) {
            try {
                Thread.sleep(3000);//每3s进行一次重连
            } catch (Exception ea) {

            }
            System.out.println("重连中.....");
            connectcore(t + 1);
        }

    }
}
