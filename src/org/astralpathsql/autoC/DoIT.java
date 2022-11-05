package org.astralpathsql.autoC;

import org.astralpathsql.file.Filer;
import org.astralpathsql.server.NIOThread;

import java.io.*;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import static org.astralpathsql.server.MainServer.*;

public class DoIT {
    /**
     * 这是实现模块,包括所有数据库功能
     */
    public static String doit(String readMessage, String Jurisdiction, String ip) throws IOException{
        writePool.submit(() -> {
            try {
                Filer.writeLog(readMessage,ip);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        String[] sp = readMessage.split(" ");
        String writeMessage = "-1";
        try {
            if (sp[0].equals("user")) {
                if (sp[1].equals("add")) {
                    if(sp[2].contains("DBA")) {
                        writeMessage = "DBA permission cannot be created!";
                    } else if (Jurisdiction.equals("ADMIN")|Jurisdiction.equals("DBA")) {
                        if(sp[2].contains(":")) {
                            Filer.addUser(sp[2]);
                            writeMessage = "用户更新-时间:" + new Date();
                        } else {
                            writeMessage = "null";
                        }
                    } else {
                        writeMessage = "权限不足!";
                    }
                }
                if (sp[1].equals("remove")) {
                    if (Jurisdiction.equals("ADMIN")|Jurisdiction.equals("DBA")) {
                        Filer.removeUser(sp[2]);
                        writeMessage = "用户更新-时间:" + new Date();
                    } else {
                        writeMessage = "权限不足!";
                    }

                }
                if (sp[1].equals("all")) {
                    if (Jurisdiction.equals("ADMIN")|Jurisdiction.equals("DBA")) {
                        writeMessage = Filer.getUser();
                    } else {
                        writeMessage = "权限不足!";
                    }
                }
                USER = Filer.readUser();
            }
            if ("exit".equals(readMessage)) { 						// 结束指令
                writeMessage = "[INFO]Connected closed...";			// 结束消息
            }
            if ("stop".equals(readMessage)) { 						// 结束指令
                Iterator<Map.Entry<String, NIOThread>> entries = nioThreadHashMap.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry<String, NIOThread> entry = entries.next();
                    NIOThread value = entry.getValue();
                    value.addQ("stop");
                }
                if (Jurisdiction.equals("ADMIN")) {
                    writeMessage = "[INFO]Connected closed...";			// 结束消息
                    System.out.println("[INFO]Server closed");
                    stopserver();
                } else {
                    writeMessage = "权限不足!";
                }
                if (Jurisdiction.equals("DBA")) {
                    writeMessage = "[INFO]Connected closed...";			// 结束消息
                    System.out.println("[INFO]Server closed");
                    stopserver();
                } else {
                    writeMessage = "权限不足!";
                }
            }
            if("wait".equals(readMessage)) {
                //查询等待节点连接再执行的指令
                Iterator<Map.Entry<String, String>> entries = nioThreadUNCMD.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry<String, String> entry = entries.next();
                    String key = entry.getKey();
                    String value = entry.getValue();
                    System.out.println(key + "|" + value);

                }
                writeMessage = "1";
            }
            if ("restart_core".equals(readMessage)) { 						// 结束指令
                writeMessage = "[INFO]Connected closed...";			// 结束消息
                System.out.println("[INFO]Server closed");
                restart();
                stopserver();
            }
            if ("restart_node".equals(readMessage)) { 						// 结束指令
                writeMessage = "[INFO]waiting";			// 结束消息
                Iterator<Map.Entry<String, NIOThread>> entries = nioThreadHashMap.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry<String, NIOThread> entry = entries.next();
                    NIOThread value = entry.getValue();
                    value.addQ("restart");
                }
            }

            if ("code".equals(sp[0])) { 						// 结束指令
                writeMessage = "[INFO]令牌改变!";
                code = sp[1];
            }

            if (readMessage.contains("reg ")) {//增加注册节点
                regnode = regnode + sp[1] + ";";
                writeMessage = "1";
            }
            if (readMessage.contains("remove ")) {//移除注册节点
                regnode = regnode.replaceAll(sp[1] + ";","");
                writeMessage = "1";
            }
            if (readMessage.contains("list")) {//获取所有注册节点
                writeMessage = regnode;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.gc();
        return writeMessage;
    }

    public static void restart() {
            try {
                Runtime rt = Runtime.getRuntime();
//            Process pr = rt.exec("cmd /c dir");
//            Process pr = rt.exec("D:/APP/Evernote/Evernote.exe");//open evernote program
                Process pr = rt.exec("java -jar .//AstralPathSQL.jar") ;//open tim program
                BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream(),"GBK"));
                String line = null;
                while ((line = input.readLine())!=null){
                    System.out.println(line);
                }
                int exitValue = pr.waitFor();
                System.out.println("Exited with error code "+exitValue);
            } catch (IOException e) {
                System.out.println(e.toString());
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
    }
}
