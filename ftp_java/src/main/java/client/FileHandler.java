package client;

import cache.JedisUtil;
import com.google.protobuf.ByteString;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.java.Log;
import model.Command;
import util.ByteUtil;
import util.MD5Util;

import java.io.*;
import java.util.Objects;

import static cache.JedisUtil.md5_prefix;

/**
 * 客户端的文件处理器
 */
@Log
public class FileHandler extends SimpleChannelInboundHandler<Command.Data> {


    private String path;
    private RandomAccessFile out;
    private Boolean check = false;
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
                        out.setLength(ByteUtil.bytes2Long(msg.getData().toByteArray()));
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
                    //设置文件的md5值
                    Objects.requireNonNull(JedisUtil.getJedis()).set(md5_prefix + ctx.channel()
                            .localAddress().toString(), MD5Util.getFileMD5String(file));

                    byte[] buffer = new byte[MAX_SIZE];
                    FileInputStream input = new FileInputStream(file);
                    data.setStatus(Command.Data.Status.STORE);
                    long pos = 0;
                    long size = file.length();
                    while (input.read(buffer) != -1) {
                        data.setPos(pos++);
                        //说明到了文件尾了
                        if (size <= pos * MAX_SIZE) {
                            int i = buffer.length - 1;
                            //剔除00
                            while (i > 0 && buffer[i] == 0) --i;
                            //发送文件
                            data.setData(ByteString.copyFrom(buffer, 0, i + 1));
                        } else data.setData(ByteString.copyFrom(buffer)); //直接发送
                        ctx.writeAndFlush(data);
                        //重新为buffer分配内存空间
                        buffer = new byte[MAX_SIZE];
                    }
                    //文件终止
                    data.setPos(pos);
                    data.setData(ByteString.EMPTY);
                    input.close();
                } catch (FileNotFoundException e) {
                    data.setStatus(Command.Data.Status.NOT_FOUND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //发送
                ctx.writeAndFlush(data);
                break;
            //下载文件
            case STORE:
                log.info(String.valueOf(msg.getPos()));
                //使用md5进行判断
                if (msg.getData() == ByteString.EMPTY) {
                    //开始校验md5
                    check = true;
                } else {
                    out.seek(msg.getPos() * MAX_SIZE);
                    out.write(msg.getData().toByteArray());
                }
                if (check) {
                    if (md5 == null) {
                        md5 = JedisUtil.getJedis().get(md5_prefix
                                + ctx.channel().remoteAddress().toString());
                    }
                    if (md5 != null && md5.equals(MD5Util.getFileMD5String(out))) {
                        //终止连接
                        ctx.writeAndFlush(Command.Data.newBuilder().setStatus(Command.Data.Status.FIN));
                    }
                }
                break;
            case FIN:
                log.info("finish");
                ctx.channel().close();
        }
    }
}
