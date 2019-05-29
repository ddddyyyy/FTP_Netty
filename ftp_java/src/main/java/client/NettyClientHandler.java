package client;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.java.Log;
import model.Command;


@ChannelHandler.Sharable
@Log
public class NettyClientHandler extends SimpleChannelInboundHandler<Command.Response> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Command.Response response) {
        switch (response.getStatus()) {
            case FILE_LIST:
                System.out.println(response.getMsg());
                break;
            case PASV:
                try {
                    Main.data_connect(Integer.valueOf(response.getMsg()), "127.0.0.1");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.out.println(response.getStatus().name());
        }
    }
}
