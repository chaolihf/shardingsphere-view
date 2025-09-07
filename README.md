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