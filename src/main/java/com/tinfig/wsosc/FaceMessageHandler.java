package com.tinfig.wsosc;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class FaceMessageHandler extends ChannelInboundMessageHandlerAdapter<TextWebSocketFrame> {
    private static final Logger LOG = LoggerFactory.getLogger(FaceMessageHandler.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<OSCPortOut> senders;
    private final Runnable onMessageProcessed;

    public FaceMessageHandler(List<OscTarget> targets, Runnable onMessageProcessed) throws SocketException, UnknownHostException {
        List<OSCPortOut> senders = new ArrayList<>();
        for (OscTarget target : targets) {
            senders.add(new OSCPortOut(InetAddress.getByName(target.address), target.port));
        }
        this.senders = Collections.unmodifiableList(senders);
        this.onMessageProcessed = onMessageProcessed;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOG.info("[{}] connected", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOG.info("[{}] disconnected", ctx.channel().remoteAddress());
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        String json = msg.getText();
        LOG.debug("[{}] received: {}", ctx.channel().remoteAddress(), json);

        FaceMessage faceMessage;
        try {
            faceMessage = mapper.readValue(json, FaceMessage.class);
        } catch (JsonParseException | JsonMappingException e) {
            LOG.error("[{}] invalid JSON", ctx.channel().remoteAddress(), e);
            return;
        }

        switch (faceMessage.type) {
            case "faceEvent":
                handleFaceEvent(faceMessage.data);
                break;
            default:
                LOG.warn("[{}] unknown event type '{}'", ctx.channel().remoteAddress(), faceMessage.type);
                break;
        }

        if (onMessageProcessed != null) {
            onMessageProcessed.run();
        }
    }

    private void handleFaceEvent(FaceMessage.FaceMessageData data) {
        if (data.faces.size() > 0) {
            // Process only the first face.
            FaceMessage.Face face = data.faces.get(0);

            face.emotions.forEach((k, v) -> {
                sendOscMessage(v, "emotions", k);
            });
            face.expressions.forEach((k, v) -> {
                sendOscMessage(v, "expressions", k);
            });
            face.appearance.forEach((k, v) -> {
                sendOscMessage(v, "appearance", k);
            });
            if (face.measurements != null) {
                sendOscMessage(face.measurements.interocularDistance, "measurements", "interocularDistance");
                face.measurements.orientation.forEach((k, v) -> {
                    sendOscMessage(v, "measurements", "orientation", k);
                });
            }
            face.featurePoints.forEach((k, v) -> {
                v.forEach((axis, value) -> {
                    sendOscMessage(value, "featurePoints", k, axis);
                });
            });
        }
    }

    private void sendOscMessage(Object value, String... addressParts) {
        if (value == null) {
            return;
        }

        String address = "/" + String.join("/", addressParts);
        OSCMessage msg = new OSCMessage(address, Collections.singletonList(value));

        LOG.debug("sending '{}': {}", msg.getAddress(), msg.getArguments());
        for (OSCPortOut sender : senders) {
            try {
                sender.send(msg);
            } catch (IOException e) {
                LOG.warn("error sending OSC", e);
            }
        }
    }
}
