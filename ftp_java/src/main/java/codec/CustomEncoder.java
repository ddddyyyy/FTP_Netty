package codec;

import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageLiteOrBuilder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.extern.java.Log;
import model.Command;
import util.ByteUtil;

import java.util.List;

import static io.netty.buffer.Unpooled.wrappedBuffer;

/**
 * 自定义的编码器
 * 数据包的格式
 * 包长度（8B） + 包数据类型（1B）+ 数据
 */
@Log
public class CustomEncoder extends MessageToMessageEncoder<MessageLiteOrBuilder> {

    private byte[] encodeHeader(MessageLite msg, int length) {
        byte messageType = 0x0f;//数据类型
        //定义数据的类型
        if (msg instanceof Command.Request) {
            messageType = 0x00;
        } else if (msg instanceof Command.Response) {
            messageType = 0x01;
        } else if (msg instanceof Command.Data) {
            messageType = 0x02;
        }
        //数据包头
        byte[] header = new byte[5];
        //数据长度
        int i = 0;
        //将int32的4个字节提取到数组
        for (byte b : ByteUtil.int2Bytes(length)) {
            header[i++] = b;
        }
        //数据类型
        header[4] = messageType;
        return header;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, MessageLiteOrBuilder msg, List<Object> out) throws Exception {
        if (msg instanceof MessageLite) {
            byte[] body = ((MessageLite) msg).toByteArray();
            byte[] header = encodeHeader(((MessageLite) msg), body.length);
            //写数据
            out.add(wrappedBuffer(ByteUtil.concat(header, body)));
        } else if (msg instanceof MessageLite.Builder) {
            byte[] body = ((MessageLite.Builder) msg).build().toByteArray();
            byte[] header = encodeHeader(((MessageLite.Builder) msg).build(), body.length);
            out.add(wrappedBuffer(ByteUtil.concat(header, body)));
        }
    }
}
