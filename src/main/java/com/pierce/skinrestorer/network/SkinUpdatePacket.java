package com.pierce.skinrestorer.network;

import com.pierce.skinrestorer.client.ClientSkinHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

public class SkinUpdatePacket implements IMessage {

    public String textureValue;
    public String textureSignature;
    public boolean clear;

    public SkinUpdatePacket() {}

    public SkinUpdatePacket(String value, String signature, boolean clear) {
        this.textureValue = value;
        this.textureSignature = signature;
        this.clear = clear;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.clear = buf.readBoolean();
        if (!clear) {
            int lenVal = buf.readInt();
            byte[] valBytes = new byte[lenVal];
            buf.readBytes(valBytes);
            this.textureValue = new String(valBytes, StandardCharsets.UTF_8);
            boolean hasSig = buf.readBoolean();
            if (hasSig) {
                int lenSig = buf.readInt();
                byte[] sigBytes = new byte[lenSig];
                buf.readBytes(sigBytes);
                this.textureSignature = new String(sigBytes, StandardCharsets.UTF_8);
            } else {
                this.textureSignature = null;
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(clear);
        if (!clear) {
            byte[] valBytes = textureValue.getBytes(StandardCharsets.UTF_8);
            buf.writeInt(valBytes.length);
            buf.writeBytes(valBytes);
            buf.writeBoolean(textureSignature != null);
            if (textureSignature != null) {
                byte[] sigBytes = textureSignature.getBytes(StandardCharsets.UTF_8);
                buf.writeInt(sigBytes.length);
                buf.writeBytes(sigBytes);
            }
        }
    }

    public static class Handler implements IMessageHandler<SkinUpdatePacket, IMessage> {
        @Override
        public IMessage onMessage(SkinUpdatePacket message, MessageContext ctx) {
            if (ctx.side.isClient()) {
                ClientSkinHandler.handle(message);
            }
            return null;
        }
    }
}
