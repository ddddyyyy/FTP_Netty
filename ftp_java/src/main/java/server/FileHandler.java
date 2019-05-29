package server;

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

import static cache.JedisUtil.file_prefix;
import static cache.JedisUtil.md5_prefix;

@Log
public class FileHandler extends SimpleChannelInboundHandler<Command.Data> {


    private String path;

    private RandomAccessFile out;

    private Boolean check = false;


    String md5 = null;

    private String fileName;


    FileHandler(String path) {
        this.path = path;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //通知客户端连接成功了
        super.channelActive(ctx);
        fileName = Objects.requireNonNull(JedisUtil.getJedis()).get(file_prefix + ctx.channel()
                .localAddress().toString());

        //fileName必须为相对路径下的全路径
        File file = new File(path + fileName);
        //写入文件的长度，这里不管是上传还是下载
        ctx.writeAndFlush(Command.Data.newBuilder().setStatus(Command.Data.Status.SUCCESS)
                .setData(ByteString.copyFrom(ByteUtil.long2Bytes(file.length()))));

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command.Data msg) {

        int MAX_SIZE = 1024 * 2;
        switch (msg.getStatus()) {
            //下载文件
            case GET:
                fileName = (Objects.requireNonNull(JedisUtil.getJedis())).get(file_prefix + ctx.channel()
                        .localAddress().toString());

                //fileName必须为相对路径下的全路径
                File file = new File(path + fileName);
                Command.Data.Builder data = Command.Data.newBuilder();
                //二进制读取文件并传给客户端
                try {
                    //设置文件的md5值
                    JedisUtil.getJedis().set(md5_prefix + ctx.channel()
                            .localAddress().toString(), MD5Util.getFileMD5String(file));

                    byte[] buffer = new byte[MAX_SIZE];
                    FileInputStream input = new FileInputStream(file);
                    data.setStatus(Command.Data.Status.STORE);
                    long pos = 0;
                    long size = file.length();
                    log.info("start to transport file");
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
            case READY:
                fileName = (JedisUtil.getJedis()).get(file_prefix + ctx.channel()
                        .localAddress().toString());

                //fileName必须为相对路径下的全路径
                file = new File(path + fileName);
                //打开写文件
                //设置文件长度
                try {
                    //删除存在的文件
                    if (file.exists() && file.delete()) {
                        log.info("file is exist");
                    }
                    out = new RandomAccessFile(path + fileName, "rw");
                    out.setLength(ByteUtil.bytes2Long(msg.getData().toByteArray()));
                    ctx.writeAndFlush(Command.Data.newBuilder().setStatus(Command.Data.Status.GET));
                } catch (FileNotFoundException e) {
                    ctx.writeAndFlush(Command.Data.newBuilder().setStatus(Command.Data.Status.NOT_FOUND));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            case STORE:
                try {
                    log.info(String.valueOf(msg.getPos()));
                    //使用md5进行判断
                    if (msg.getData() == ByteString.EMPTY) {
                        //开始校验md5
                        check = true;
                        log.info("md5:" + MD5Util.getFileMD5String(out));
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
                            log.info("finish transport");
                            //终止连接
                            ctx.writeAndFlush(Command.Data.newBuilder().setStatus(Command.Data.Status.FIN));
                        } else if (md5 == null) {
                            log.info("cache lose");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case FIN:
                ctx.channel().close();
                break;
            default:
                break;
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        //清空缓存
        Objects.requireNonNull(JedisUtil.getJedis()).del(md5_prefix + ctx.channel()
                .localAddress().toString());
        JedisUtil.getJedis().del(file_prefix + ctx.channel()
                .localAddress().toString());
    }
}
