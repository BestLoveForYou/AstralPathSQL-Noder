# AstralPath 节点至强版
   By BestLoveForYou
   website:[点击](http://www.godserver.cn/)
   email:yaoboyulove@163.com
   
   ## **节点版与普通版执行对比**
   
   项目  |  节点版本  |  普通版本
   ---| --- | ---
单连接多数据(1000)添加耗时|  34.5 | 47.4
三连接三节点下多数据添加| 3.7 | 47.4
数据量1000普通查询速度 | 0.09 | 0.08 |
数据量1000二叉树高速查询速度 | 0.03 | 0.05 |
高负载情况下的运行能力 | 0.8 | 0.6



## **二叉树下，搜索耗时与数据量对之：**
```echarts
option = {
    xAxis: {
        type: 'category',
        data: ['0', '100', '200', '300', '400', '500', '600']
    },
    yAxis: {
        type: 'value'
    },
    series: [{
        data: [0, 10, 14, 17, 20, 22, 23],
        type: 'line'
    }]
};
```

## 节点版好在哪里?
**无与伦比的多数据处理能力**
采用多节点结构,再使用负载分配技术,使得节点可以平均分配和处理客户端发送的指令,完美满足大部分的高并发场景.
**FIFO指令队列**
节点处使用FIFO队列处理sql命令,为单连接但短时多指令提供了自己的解决方案,且解决了IO阻塞队列的麻烦
**几乎无法出现完全崩溃的能力**
使用节点分配技术,每个节点相对独立,若是出现单个节点崩溃,并不会影响到其他正常运行中的节点
**安全,隐形**
您暴露的只有负责分配节点给客户端的终端IP,在客户端令牌认证成功之前,客户端不会获得关于节点的任何信息
**心跳信息**
终端与节点使用心跳信息交流,方便终端即时更新节点状态,避免了脏节点的问题

## 使用
Github:[点击](http://www.godserver.cn/)
