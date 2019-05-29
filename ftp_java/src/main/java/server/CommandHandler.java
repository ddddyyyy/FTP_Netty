package server;

import cache.JedisUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.java.Log;
import model.Command;
import util.SocketUtil;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Objects;

import static cache.JedisUtil.*;


/**
 * 处理接收到的命令
 */
@Log
public class CommandHandler extends SimpleChannelInboundHandler<Command.Request> {

    private String user = null;
    private boolean isLogin = false;

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Command.Request request) {
        Command.Request.Type command = request.getCommand();
        //判断用户是否登录
        if (isLogin || command == Command.Request.Type.USER || command == Command.Request.Type.PASS || command == Command.Request.Type.BYE) {

            String args = request.getArgs().replaceAll(" ", "");
            Command.Response.Builder response = Command.Response.newBuilder();
            switch (command) {
                case USER:
                    if (null != Objects.requireNonNull(getJedis()).get(user_prefix + args)) {
                        //放入缓存
                        user = args;
                        channelHandlerContext.writeAndFlush(Command
                                .Response.newBuilder().setStatus(Command.Response.Status.OK));
                    } else {
                        channelHandlerContext.writeAndFlush(Command
                                .Response.newBuilder().setStatus(Command.Response.Status.USER_NOT_EXIST));
                    }
                    break;
                case PASS:
                    //没有用户名的话
                    if (user == null) {
                        channelHandlerContext.writeAndFlush(Command
                                .Response.newBuilder().setStatus(Command.Response.Status.USER_NOT_EXIST));
                    } else {
                        //从缓存里面提取用户密码
                        String password = Objects.requireNonNull(getJedis()).get(user_prefix + user);
                        if (null != password && password.equals(args)) {
                            channelHandlerContext.writeAndFlush(Command
                                    .Response.newBuilder().setStatus(Command.Response.Status.OK));
                            isLogin = true;
                        } else {
                            channelHandlerContext.writeAndFlush(Command
                                    .Response.newBuilder().setStatus(Command.Response.Status.PASS_ERROR));
                        }
                    }
                    break;
                case BYE:
                    break;
                case LS:
                    String path = "../ftp_file/" + args;
                    StringBuilder str = new StringBuilder();
                    //得到当前文件夹下的文件
                    try {
                        Files.list(Paths.get(path)).forEach(obj -> str.append(obj.getFileName()).append('\n'));
                        response.setMsg(str.toString());
                        response.setStatus(Command.Response.Status.FILE_LIST);
                    } catch (NoSuchFileException e) {
                        response.setStatus(Command.Response.Status.FILE_LIST);
                    } catch (Exception e) {
                        e.printStackTrace();
                        response.setStatus(Command.Response.Status.FILE_LIST);
                    }
                    break;
                case PUT:
                case GET:
                    //缓存要下载的文件
                    //key为地址加端口号
                    Integer port = SocketUtil.GetFreePort();
                    //打开端口
                    //这里不考虑代理。。。
                    if (port != -1 && Main.data_server(port)) {
                        response.setStatus(Command.Response.Status.PASV);
                        response.setMsg(port.toString());
                        Objects.requireNonNull(JedisUtil.getJedis())
                                .set(file_prefix + channelHandlerContext.channel()
                                        .remoteAddress().toString().split(":.*")[0] + String.format(":%d", port), args);
                    } else {
                        response.setStatus(Command.Response.Status.EPSV);
                    }
                    break;
                case PORT:
                    break;

                default:
                    break;
            }
            channelHandlerContext.writeAndFlush(response);
        } else {
            channelHandlerContext.writeAndFlush(Command
                    .Response.newBuilder().setStatus(Command.Response.Status.NOT_LOGIN));
        }
    }


}
