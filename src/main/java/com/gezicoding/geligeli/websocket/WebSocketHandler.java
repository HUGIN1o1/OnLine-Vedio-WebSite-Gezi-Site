package com.gezicoding.geligeli.websocket;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.redis.core.StringRedisTemplate;

import com.alibaba.fastjson.JSON;
import com.gezicoding.geligeli.constants.SnowFlakeConstants;
import com.gezicoding.geligeli.constants.WebSocketConstant;
import com.gezicoding.geligeli.messagequeue.RocketMQProducter;
import com.gezicoding.geligeli.model.dto.video.SendBulletRequest;
import com.gezicoding.geligeli.model.vo.video.BulletScreenResponse;
import com.gezicoding.geligeli.model.vo.video.OnlineBulletResponse;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;


@SuppressWarnings("{all}")
@Slf4j
@AllArgsConstructor
public class WebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final RocketMQProducter producer;

    private final StringRedisTemplate stringRedisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);


    // 维护视频弹幕通道的映射
    private static final ConcurrentMap<String, ChannelGroup> videoChannelMap = new ConcurrentHashMap<>();

    private static final AttributeKey<String> VIDEOID = AttributeKey.valueOf("videoId");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        String videoId = ctx.channel().attr(VIDEOID).get();
        if (videoId != null) {
            boolean login = checkOnline(msg.text());
            System.out.println(login);
            if (login) {
                System.out.println("消息发送成功：" + msg.text());
                broadcastMessage(videoId, onlineMessage(msg.text()));
            } else {
                needLoginMessage(videoId, ctx.channel());
            }
        }
    }

    private void needLoginMessage(String videoId, Channel channel) {
        BulletScreenResponse bulletScreenResponse = new BulletScreenResponse();
        bulletScreenResponse.setType(WebSocketConstant.LOGIN_MESSAGE);

        bulletScreenResponse.setData("请先登录");
        String message = JSON.toJSONString(bulletScreenResponse);
        channel.writeAndFlush(new TextWebSocketFrame(message)).addListener(future -> {
            if (!future.isSuccess()) {
                logger.error("消息失败到房间：{}，原因：{}", videoId, future.cause().getMessage());
                cleanupInvalidChannels(videoChannelMap.get(videoId));
            }
        });
        

    }

    public String onlineMessage(String text) {
        BulletScreenResponse bulletScreenResponse = new BulletScreenResponse();
        bulletScreenResponse.setType(WebSocketConstant.ONLINE_BULLET);

        SendBulletRequest request = JSONUtil.toBean(text, SendBulletRequest.class);
        Snowflake snowflake = IdUtil.getSnowflake(SnowFlakeConstants.MACHINE_ID, SnowFlakeConstants.DATA_CENTER_ID);
        request.setBulletId(snowflake.nextId());

        String messageDTO = JSONUtil.parse(request).toString();
        producer.sendMessage("geligeli-topic", messageDTO);
        System.out.println("发送消息到MQ: " + messageDTO);
        OnlineBulletResponse onlineBulletResponse = new OnlineBulletResponse();

        onlineBulletResponse.setPlaybackTime(request.getPlaybackTime());
        onlineBulletResponse.setText(request.getContent());
        onlineBulletResponse.setUserId(request.getUserId().toString());
        onlineBulletResponse.setBulletId(request.getBulletId().toString());
        bulletScreenResponse.setData(onlineBulletResponse);
        return JSONUtil.parse(bulletScreenResponse).toString();
    }


    /**
     * 加入房间，如果房间不存在，则创建房间
     * @param videoId
     * @param channel
     */
    private void joinRoom(String videoId, Channel channel) {
        videoChannelMap.computeIfAbsent(videoId, k -> new DefaultChannelGroup(GlobalEventExecutor.INSTANCE)).add(channel);
    }


    /**
     * 广播消息到指定视频的ChannelGroup
     * @param videoId
     * @param message
     * @param channel
     */
    private void broadcastMessage(String videoId, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        ChannelGroup group = videoChannelMap.get(videoId);
        if (group != null && !group.isEmpty()) {
            group.writeAndFlush(new TextWebSocketFrame(message)).addListener(future -> {
                if (!future.isSuccess()) {
                    logger.error("消息失败到房间：{}，原因：{}", videoId, future.cause().getMessage());
                    cleanupInvalidChannels(group);
                }
            });
        }
    }


    /**
     * 广播在线人数，在线人数是ChannelGroup的大小
     * @param videoId
     */
    private void broadcastOnlineCount(String videoId) {
        ChannelGroup group = videoChannelMap.get(videoId);
        if (group != null) {
            BulletScreenResponse bulletScreenResponse = new BulletScreenResponse();
            bulletScreenResponse.setType(WebSocketConstant.ONLINE_NUMBER);
            bulletScreenResponse.setData(group.size());
            String message = JSONUtil.parse(bulletScreenResponse).toString();
            group.writeAndFlush(new TextWebSocketFrame(message));
        }
    }


    /**
     * 打扫不在使用的ChannelGroup,把没有开启的和不活跃的Channel关闭，并从ChannelGroup中移除
     * @param group
     */
    private void cleanupInvalidChannels(ChannelGroup group) {
        List<Channel> invalidChannels = group.stream().filter(ch -> !ch.isActive() || !ch.isOpen()).collect(Collectors.toList());
        invalidChannels.forEach(ch -> {
            group.remove(ch);
        });
    }

    /**
     * 通过发送弹幕请求，检查用户是否在线
     * @param text 发送弹幕请求
     * @return boolean 是否在线
     */
    private boolean checkOnline(String text) {
        SendBulletRequest request = JSONUtil.toBean(text, SendBulletRequest.class);
        String userId = request.getUserId().toString();
        System.out.println("userId: " + userId);
        String token = stringRedisTemplate.opsForValue().get(userId);
        return token != null;
    }

    /**
     * 从 URI 中提取房间 ID
     * @param uri
     * @return
     */
    private String extractRoomId(String uri) {
        String[] pathSegments = uri.split("/");
        return pathSegments[pathSegments.length - 1];
    }


    /**
     * 用户事件触发器，处理握手完成和心跳连接, 握手完成时，提取房间 ID 并加入房间
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.info("30 秒没有读取到数据，发送心跳保持连接: {}", ctx.channel());

                ctx.channel().writeAndFlush(new TextWebSocketFrame(JSON.toJSONString("ping"))).addListener(future -> {
                    if (!future.isSuccess()) {
                        log.error("发送心跳失败: {}", future.cause());
                    }
                });
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }

        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketServerProtocolHandler.HandshakeComplete handshake = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            String uri = handshake.requestUri();
            String videoId = extractRoomId(uri);
            if (videoId != null) {
                ctx.channel().attr(VIDEOID).set(videoId);
                joinRoom(videoId, ctx.channel());
                broadcastOnlineCount(videoId);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("WebSocket error: {}", cause.getMessage());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        log.info("WebSocket client connected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        log.info("WebSocket client disconnected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        log.info("WebSocket client added: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);

        String videoId = ctx.channel().attr(VIDEOID).get();
        if (videoId != null) {
            ChannelGroup group = videoChannelMap.get(videoId);
            if (group != null) {
                group.remove(ctx.channel());
                broadcastOnlineCount(videoId);
            }
        }
        log.info("WebSocket client removed: {}", ctx.channel().remoteAddress());
    }

}
