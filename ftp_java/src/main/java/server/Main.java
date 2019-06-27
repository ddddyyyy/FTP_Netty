package server;

import codec.CustomDecoder;
import codec.CustomEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.java.Log;
import util.InitData;

@Log
public class Main {


    private static void command_server() {

        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // (3)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
//                            //解码器，通过Google Protocol Buffers序列化框架动态的切割接收到的ByteBuf
//                            pipeline.addLast(new ProtobufVarint32FrameDecoder());
//                            //Google Protocol Buffers编码器
//                            pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
//                            pipeline.addLast(new ProtobufDecoder(Command.Request.getDefaultInstance()));
//                            //Google Protocol Buffers编码器
//                            pipeline.addLast(new ProtobufEncoder());

                            pipeline.addLast("decoder", new CustomDecoder());
                            pipeline.addLast("encoder", new CustomEncoder());
                            //自定义解析器，用于处理命令
                            pipeline.addLast(new CommandHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            // 绑定端口，开始接收进来的连接
            Integer port = 666;
            b.bind(port).sync().channel().closeFuture().sync(); // (7)

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
//            JedisUtil.close();
        }
    }


    /**
     * 数据传输
     *
     * @param port 端口
     */
    static boolean data_server(Integer port) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap(); // (2)
        try {
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // (3)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            pipeline.addLast("decoder", new CustomDecoder());
                            pipeline.addLast("encoder", new CustomEncoder());
                            //自定义解析器，用于处理命令
                            pipeline.addLast(new server.FileHandler("../ftp_file/"));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)
            // 绑定端口，开始接收进来的连接
            b.bind(port).sync(); // (7)
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) throws Exception {
        InitData.init();
        command_server();
    }
}
