# java
非常感谢你们的测试机会。


## 达到目标
### 下单接口实现方法
检查库存，在数据库create pending order后，会在redis 记录pending order 和pending order时间。

### 订单支付接口
在数据库把order status改为PAID。
并且delete 此 pending order的redis记录。

### 取消交易
Schedule 每一分钟检查记录在redis的pending order 是否超过30分钟。如果超过就取消交易并且delete 此 pending order的redis记录。

#### 未来应该要实现的
因为schedule每一分钟取消交易只检查redis的pending order。应该还要实现另一个feature去schedule检查database pending order。

### redis缓存商品信息
每当query product 时，都会先去query redis，如果redis没有才query database再记录到redis然后return.

### compose.yaml
应该要有的service都在这边，如果已有运行中的service，可以在intellij自行选择运行自己欠缺的service.

## 无法完成之需求
rocketmq setup成功，rocketmq发送消息通知，rocketmq 发送消息timeout，没有其他的error log，查找bug当中。

## 需求
1. 订单表（id,订单编号，用户ID，商品编码，数据量，金额，总金额），商品表（id,商品编码，商品名，库存数量）
2. 实现一个下单接口（这里会有很多高并发订单）
3. 订单支付接口
4. 库存不能超卖
5. 查询订单信息
6. 30分钟没付款就自动关闭交易。

要求：
1. redis 缓存商品信息
2. rocketmq 订单交易成功，交易失败，发送消息通知




## Table
### Order
| Column Name   | Data Type        |
|---------------|------------------|
| id            | Long             |
| userId        | Long             |
| orderNumber   | String           |
| productCode   | String           |
| quantity      | int              |
| amount        | double           |
| totalAmount   | double           |
| status        | OrderStatus      |
| createdTime   | LocalDateTime    |

### Product
| Column Name   | Data Type        |
|---------------|------------------|
| id            | Long             |
| productCode   | String           |
| productName   | String           |
| stock         | int              |
| price         | double           |
