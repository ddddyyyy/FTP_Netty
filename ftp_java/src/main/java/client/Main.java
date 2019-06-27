package client;

import codec.CustomDecoder;
import codec.CustomEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import model.Command;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main {

    static String fileName;
    static String method;

    public static void main(String[] args) throws Exception {
        connect(666, "127.0.0.1");
    }


    static void data_connect(int port, String host) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        // 配置客户端NIO线程组
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("decoder", new CustomDecoder());
                        pipeline.addLast("encoder", new CustomEncoder());
//                        //解码器，通过Google Protocol Buffers序列化框架动态的切割接收到的ByteBuf
                        pipeline.addLast(new ProtobufVarint32FrameDecoder());
//                        //Google Protocol Buffers编码器
//                        pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
//
//                        //Google Protocol Buffers编码器
//                        pipeline.addLast(new ProtobufEncoder());
//                        //data的解码器
//                        pipeline.addLast(new ProtobufDecoder(Command.Data.getDefaultInstance()));
                        //自定义解析器，用于处理命令
                        pipeline.addLast(new client.FileHandler("../ftp_file_receive/"));
                    }
                });
        ChannelFuture f = b.connect(host, port).sync();

        f.channel().closeFuture();
    }

    private static void connect(int port, String host) throws Exception {
        // 配置客户端NIO线程组
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
//                            //解码器，通过Google Protocol Buffers序列化框架动态的切割接收到的ByteBuf
//                            pipeline.addLast(new ProtobufVarint32FrameDecoder());
//                            //Google Protocol Buffers编码器
//                            pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
//
//                            //Google Protocol Buffers解码器
//                            pipeline.addLast(new ProtobufDecoder(Command.Response.getDefaultInstance()));
//                            //Google Protocol Buffers编码器
//                            pipeline.addLast(new ProtobufEncoder());

                            pipeline.addFirst(new CustomDecoder());
                            pipeline.addFirst(new CustomEncoder());
                            pipeline.addLast(new NettyClientHandler());

                        }
                    });

            // 发起异步连接操作
            ChannelFuture f = b.connect(host, port).sync();


            new Thread(() -> {
                try (Scanner in = new Scanner(System.in)) {
                    while (true) {
                        String str = in.nextLine();
                        List<String> list = Arrays.stream(str.trim().split(" "))
                                .map(String::toLowerCase)
                                .collect(Collectors.toList());
                        if (list.size() < 1)
                            System.out.println("命令格式错误");
                        Command.Request.Builder builder = Command.Request.newBuilder();
                        switch (list.get(0)) {
                            case "ls":
                                builder.setCommand(Command.Request.Type.LS);
                                f.channel().writeAndFlush(builder);
                                break;
                            case "put":
                                method = "put";
                                builder.setCommand(Command.Request.Type.PUT);
                                if (list.size() == 2) {
                                    builder.setArgs(list.get(1));
                                    f.channel().writeAndFlush(builder);
                                } else {
                                    System.out.println("命令格式错误");
                                }
                                fileName = list.get(1);
                                break;
                            case "get":
                                method = "get";
                                builder.setCommand(Command.Request.Type.GET);
                                if (list.size() == 2) {
                                    builder.setArgs(list.get(1));
                                    f.channel().writeAndFlush(builder);
                                } else {
                                    System.out.println("命令格式错误");
                                }
                                fileName = list.get(1);
                                break;
                            case "user":
                                builder.setCommand(Command.Request.Type.USER);
                                if (list.size() == 2) {
                                    builder.setArgs(list.get(1));
                                    f.channel().writeAndFlush(builder);
                                } else {
                                    System.out.println("命令格式错误");
                                }
                                break;
                            case "pass":
                                builder.setCommand(Command.Request.Type.PASS);
                                if (list.size() == 2) {
                                    builder.setArgs(list.get(1));
                                    f.channel().writeAndFlush(builder);
                                } else {
                                    System.out.println("命令格式错误");
                                }
                                break;
                            case "":
                            case "\r\n":
                            case "\n":
                                break;
                            default:
                                System.out.println("命令不存在");
                        }
                    }
                }
            }).start();

            // 当代客户端链路关闭
            f.channel().closeFuture().sync();

        } finally {
            // 优雅退出，释放NIO线程组
            group.shutdownGracefully().sync();
        }
    }
}