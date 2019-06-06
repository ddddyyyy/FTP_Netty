package server;

import cache.JedisUtil;
import com.alibaba.fastjson.JSON;
import com.google.protobuf.ByteString;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.java.Log;
import model.Command;
import util.ByteUtil;
import util.EhCacheUtil;
import util.MD5Util;

import java.io.*;
import java.util.Objects;

import static cache.JedisUtil.*;

@Log
public class FileHandler extends SimpleChannelInboundHandler<Command.Data> {


    private String path;


    FileHandler(String path) {
        this.path = path;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //通知客户端连接成功了
        super.channelActive(ctx);
        String fileName = Objects.requireNonNull(JedisUtil.getJedis()).get(file_prefix + ctx.channel()
                .localAddress().toString());

        //fileName必须为相对路径下的全路径
        File file = new File(path + fileName);
        //写入文件的长度，这里不管是上传还是下载
        ctx.writeAndFlush(Command.Data.newBuilder().setStatus(Command.Data.Status.SUCCESS)
                .setData(ByteString.copyFrom(ByteUtil.long2Bytes(file.length()))));
        //存入要操作的文件进入内存
        String remote = ctx.channel().remoteAddress().toString();

        EhCacheUtil.put(obj_file_prefix + remote, file);
        log.info(JSON.toJSONString((EhCacheUtil.get(obj_file_prefix + remote))));
        EhCacheUtil.put(obj_out_prefix + remote, new RandomAccessFile(path + fileName, "rw"));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command.Data msg) {

        String remote = ctx.channel().remoteAddress().toString();

        int MAX_SIZE = 1024 * 2;
        switch (msg.getStatus()) {
            //下载文件
            case GET:
                //fileName必须为相对路径下的全路径
                File file = (File) (EhCacheUtil.get(obj_file_prefix + remote));
                Command.Data.Builder data = Command.Data.newBuilder();
                //二进制读取文件并传给客户端
                try {
                    //设置文件的md5值
                    Objects.requireNonNull(JedisUtil.getJedis()).set(md5_prefix + ctx.channel()
                            .localAddress().toString(), MD5Util.getFileMD5String(file));

                    byte[] buffer = new byte[MAX_SIZE];
                    assert file != null;
                    FileInputStream input = new FileInputStream(file);
                    data.setStatus(Command.Data.Status.STORE);
                    long pos = 0;
                    long size = file.length();
                    log.info("start to transport file");
                    pos = transport(ctx, MAX_SIZE, data, buffer, input, pos, size);
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
            case READY:
                //fileName必须为相对路径下的全路径
                file = (File) (EhCacheUtil.get(obj_file_prefix + remote));
                //打开写文件
                //设置文件长度
                try {
                    //删除存在的文件
                    assert file != null;
                    if (file.exists() && file.delete()) {
                        log.info("file is exist");
                    }
                    RandomAccessFile out = (RandomAccessFile) (EhCacheUtil.get(obj_out_prefix + remote));
                    out.setLength(ByteUtil.bytes2Long(msg.getData().toByteArray()));
                    ctx.writeAndFlush(Command.Data.newBuilder().setStatus(Command.Data.Status.GET));
                } catch (FileNotFoundException e) {
                    ctx.writeAndFlush(Command.Data.newBuilder().setStatus(Command.Data.Status.NOT_FOUND));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            case STORE:
                RandomAccessFile out = (RandomAccessFile) (EhCacheUtil.get(obj_out_prefix + remote));
                try {
                    log.info(String.valueOf(msg.getPos()));
                    //使用md5进行判断
                    if (msg.getData() == ByteString.EMPTY) {
                        //开始校验md5
                        EhCacheUtil.put(obj_check_prefix + remote, Boolean.TRUE);
                        log.info("md5:" + MD5Util.getFileMD5String(out));
                    } else {
                        out.seek(msg.getPos() * MAX_SIZE);
                        out.write(msg.getData().toByteArray());
                    }
                    Boolean check = (Boolean) EhCacheUtil.get(obj_check_prefix + remote);
                    if (check != null && check) {
                        String md5 = Objects.requireNonNull(JedisUtil.getJedis()).get(md5_prefix
                                + ctx.channel().remoteAddress().toString());
                        if (md5 != null && md5.equals(MD5Util.getFileMD5String(out))) {
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
                log.info("finish transport");
//                ctx.channel().close();
                break;
            default:
                break;
        }
    }

    public static long transport(ChannelHandlerContext ctx, int MAX_SIZE, Command.Data.Builder data, byte[] buffer, FileInputStream input, long pos, long size) throws IOException {
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
        return pos;
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
