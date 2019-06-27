package client;

import com.google.protobuf.ByteString;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.java.Log;
import model.Command;
import util.ByteUtil;
import util.MD5Util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户端的文件处理器
 */
@Log
public class FileHandler extends SimpleChannelInboundHandler<Command.Data> {


    /**
     * 分片的序号
     */
    private List<Integer> tag;
    /**
     * 传输完成的数量
     */
    private Integer completeCount = 0;
    /**
     * 文件路径
     */
    private String path;
    /**
     * 保存的文件
     */
    private RandomAccessFile out;
    /**
     * 是否开始检查md5
     */
    private Boolean check = false;
    /**
     * 文件正确的md5
     */
    private String md5 = null;

    FileHandler(String path) {
        this.path = path;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Command.Data msg) throws Exception {
        int MAX_SIZE = 1024 * 2;
        switch (msg.getStatus()) {
            //连接服务器成功
            case SUCCESS:
                Command.Data.Builder builder = Command.Data.newBuilder();
                switch (Main.method) {
                    case "get":
                        File file = new File(path + Main.fileName);
                        if (file.exists() && file.delete()) {
                            log.info("file is exist");
                        }
                        out = new RandomAccessFile(path + Main.fileName, "rw");
                        //得到文件的大小
                        out.setLength(ByteUtil.bytes2Long(msg.getData().toByteArray()));
                        //初始化数组
                        tag = new ArrayList<>();
                        for (int i = 0; i < (int) Math.ceil(1.0 * out.length() / MAX_SIZE); ++i) {
                            tag.add(i);
                        }
                        builder.setStatus(Command.Data.Status.GET);
                        //通知服务器开始发送数据
                        ctx.writeAndFlush(builder);
                        break;
                    case "put":
                        builder.setStatus(Command.Data.Status.READY);
                        builder.setData(ByteString.copyFrom(ByteUtil
                                .long2Bytes(new File(path + Main.fileName).length())));
                        ctx.writeAndFlush(builder);
                        break;
                }
                break;
            case GET:
                Command.Data.Builder data = Command.Data.newBuilder();
                //二进制读取文件并传给客户端
                try {
                    File file = new File(path + Main.fileName);

                    data.setStatus(Command.Data.Status.MD5);
                    data.setData(ByteString.copyFromUtf8(MD5Util.getFileMD5String(file)));
                    ctx.writeAndFlush(data);

                    byte[] buffer = new byte[MAX_SIZE];
                    FileInputStream input = new FileInputStream(file);
                    data.setStatus(Command.Data.Status.STORE);
                    long pos = 0;
                    long size = file.length();
                    pos = server.FileHandler.transport(ctx, MAX_SIZE, data, buffer, input, pos, size, file.length());
                    //文件终止
                    data.setPos(pos);
                    data.setData(ByteString.EMPTY);
                    ctx.writeAndFlush(data);
                    input.close();
                } catch (FileNotFoundException e) {
                    data.setStatus(Command.Data.Status.NOT_FOUND);
                    ctx.writeAndFlush(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;
            //下载文件
            case STORE:
                //使用md5进行判断
                if (msg.getData().equals(ByteString.EMPTY)) {
                    //开始校验md5
                    check = true;
                } else {
                    out.seek(msg.getPos() * MAX_SIZE);
                    out.write(msg.getData().toByteArray());
                    //标志已经传完
                    tag.set((int) msg.getPos(), -1);
                    completeCount++;
                    PrintProcess();
                }
                if (check) {
                    if (md5 != null && md5.equals(MD5Util.getFileMD5String(out))) {
                        //终止连接
                        ctx.writeAndFlush(Command.Data.newBuilder().setStatus(Command.Data.Status.FIN));
                        ctx.close();
                    }
                }
                break;
            case MD5:
                md5 = msg.getData().toStringUtf8();
                if (md5.equals(MD5Util.getFileMD5String(out))) {
                    //终止连接
                    ctx.writeAndFlush(Command.Data.newBuilder().setStatus(Command.Data.Status.FIN));
                    ctx.close();
                }
                break;
            case FIN:
                log.info("finish");
                ctx.channel().close();
        }
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        //情空变量
        completeCount = 0;
        path = null;
        out = null;
        check = false;
        md5 = null;
    }

    /**
     * 打印进度
     */
    private void PrintProcess() {
        System.out.println(String.format("已经传输了%f%%\n", completeCount * 1.0 / tag.size() * 100));
    }
}
