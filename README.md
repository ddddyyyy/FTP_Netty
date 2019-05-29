# FTP_Netty
使用Netty实现的FTP服务器

- 使用redis缓存一些必要的数据，用于识别客户端
- 使用了谷歌的Google Protocol Buffer来自定义数据包的格式
- 自己自定义了用于解析Protocol的编码器和解码器，虽然用它自带的也是可以的2333
