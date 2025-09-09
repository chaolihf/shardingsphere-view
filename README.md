# 1、说明
基于ShardingSphere实现sql语句的转写，将对特定表的操作转化为等同对视图的操作

# 2、注入点
需要在XX位置对sql语句进行改写，不改变其他的流程# 2.1 MySQL解析插入
# 2.1 MySQL解析插入
MySQLComQueryPacketExecutor:先执行prepard生成statementId并存到register中
MySQLComStmtExecuteExecutor：根据id从register中获取语句，并获取参数进行执行
# 2.2 PostgresSQL解析插入




# 3、不支持语法
## 不指定插入列的语句 如：insert into tables values (); 
## 不指定更新条件的语句，如： update tables set name='a' ;
## 不允许更新租户字段，路update tables set tanent_id='a' where name='b'
## 不指定删除条件的语句，如： delete from tables;

# 4、待实现
## 可能需要对hint进行处理，防止通过hint越权访问数据

# 5、源代码注入方法
通过diff目录下的diff文件修改对应版本的源码，并编译成jar包，将jar包上传到对应版本的lib目录下

# 6、生成diff文件的方法
git log 查询出变更文件的记录
git diff ec25fe5044d0719b5ebf778b9cb2428ecaa8e01c aec5b939257aefb85
663061e0f097e9a11edc933 > MySQLComQueryPacket.diff
生成两次commitid之间的变化

# 7、开发环境搭建
# 7.1 编译shadingdirver项目
使用gradle jar 命令编译生成ShardingDriver-it.1.1.1.jar包
# 7.1 编译shardingsphere-proxy-frontend-mysql项目
根据diff目录下的diff文件修改对应版本的源码，并编译成jar包
调试时可以手动引入ShardingDriver.jar文件并绑定源代码