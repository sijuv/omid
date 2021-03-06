/**
 * Copyright 2011-2015 Yahoo Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yahoo.omid.tso;

import java.nio.channels.ClosedChannelException;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;

import com.yahoo.omid.proto.TSOProto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ChannelHandler for the TSO Server.
 * <p>
 * 
 * Incoming requests are processed in this class and by accessing a shared data structure, {@link TSOState}
 * 
 */
public class TSOHandler extends SimpleChannelHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TSOHandler.class);

    /**
     * Channel Group
     */
    private ChannelGroup channelGroup = null;
    private final RequestProcessor requestProcessor;

    /**
     * Constructor
     * 
     * @param channelGroup
     */
    public TSOHandler(ChannelGroup channelGroup,
                      RequestProcessor requestProcessor) {
        this.channelGroup = channelGroup;
        this.requestProcessor = requestProcessor;
     }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        channelGroup.add(ctx.getChannel());
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        channelGroup.remove(ctx.getChannel());
    }

    /**
     * Handle receieved messages
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        Object msg = e.getMessage();
        if (msg instanceof TSOProto.Request) {
            TSOProto.Request request = (TSOProto.Request)msg;
            if (request.hasHandshakeRequest()) {
                checkHandshake(ctx, request.getHandshakeRequest());
                return;
            }
            if (!handshakeCompleted(ctx)) {
                LOG.info("handshake not completed");
                ctx.getChannel().close();
            }

            if (request.hasTimestampRequest()) {
                requestProcessor.timestampRequest(ctx.getChannel());
            } else if (request.hasCommitRequest()) {
                TSOProto.CommitRequest cr = request.getCommitRequest();
                requestProcessor.commitRequest(cr.getStartTimestamp(),
                                               cr.getCellIdList(),
                                               cr.getIsRetry(),
                                               ctx.getChannel());
            } else {
                LOG.error("Invalid request {}", request);
                ctx.getChannel().close();
            }
        } else {
            LOG.error("Unknown message type", msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        if (e.getCause() instanceof ClosedChannelException) {
            return;
        }
        LOG.warn("TSOHandler: Unexpected exception from downstream.", e.getCause());
        Channels.close(e.getChannel());
    }

    private void checkHandshake(final ChannelHandlerContext ctx,
                                TSOProto.HandshakeRequest request) {
        TSOProto.HandshakeResponse.Builder response = TSOProto.HandshakeResponse.newBuilder();
        if (request.hasClientCapabilities()) {

            response.setClientCompatible(true)
                .setServerCapabilities(TSOProto.Capabilities.newBuilder().build());
            TSOChannelContext tsoCtx = new TSOChannelContext();
            tsoCtx.setHandshakeComplete();
            ctx.setAttachment(tsoCtx);
        } else {
            response.setClientCompatible(false);
        }
        ctx.getChannel().write(TSOProto.Response.newBuilder()
                               .setHandshakeResponse(response.build()).build());
    }

    private boolean handshakeCompleted(ChannelHandlerContext ctx) {
        Object o = ctx.getAttachment();
        if (o instanceof TSOChannelContext) {
            TSOChannelContext tsoCtx = (TSOChannelContext)o;
            return tsoCtx.getHandshakeComplete();
        }
        return false;
    }

    static class TSOChannelContext {
        boolean handshakeComplete;

        TSOChannelContext() {
            handshakeComplete = false;
        }

        boolean getHandshakeComplete() {
            return handshakeComplete;
        }

        void setHandshakeComplete() {
            handshakeComplete = true;
        }
    }
}
