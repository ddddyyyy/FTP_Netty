package codec;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.java.Log;
import model.Command;

import java.util.List;

@Log
public class CustomDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws InvalidProtocolBufferException {
        if (in.isReadable(5)) {
            //可以查看源码里面类的注释
            //文件操作的读写指针是分开的
            in.markReaderIndex();
            //长度
            int length = 0;
            for (int ix = 0; ix < 4; ++ix) {
                length <<= 8;
                length |= (in.readByte() & 0xff);
            }
            //类型
            byte type = in.readByte();

            //数据长度不够
            if (in.readableBytes() < length) {
                in.resetReaderIndex();
                return;
//                throw new InvalidProtocolBufferException("错误格式的数据包");
            }

            ByteBuf body = in.readBytes(length);

            byte[] array;
            int offset;

            int readableLen = body.readableBytes();
            //判断ByteBuf是否有支撑数组，如果没有则说明其是用的是直接缓存模式
            if (body.hasArray()) {
                array = body.array();
                offset = body.arrayOffset() + body.readerIndex();
            } else {
                //需要生成一个数组来保存
                array = new byte[readableLen];
                body.getBytes(body.readerIndex(), array, 0, readableLen);
                offset = 0;
            }

            switch (type) {
                case 0x00:
                    out.add(Command.Request.getDefaultInstance()
                            .getParserForType().parseFrom(array, offset, length));
                    break;
                case 0x01:
                    out.add(Command.Response.getDefaultInstance()
                            .getParserForType().parseFrom(array, offset, length));
                    break;
                case 0x02:
                    out.add(Command.Data.getDefaultInstance()
                            .getParserForType().parseFrom(array, offset, length));
                    break;
            }
        }

    }

}
