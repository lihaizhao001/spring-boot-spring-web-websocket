package me.hz;


import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.*;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketSession;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class WsApplication {

    private JettyWebSocketClient client;

    private TestJettyWebSocketServer server;

    private String wsUrl;

    private WebSocketSession wsSession;

    private WebSocketSession serverSession;

    public static void main(String[] args) throws Exception{
        WsApplication THIS = new WsApplication();
        THIS.server = new TestJettyWebSocketServer(new ServerTextWebSocketHandler());
        THIS.server.start();

        THIS.client = new JettyWebSocketClient();
        THIS.client.start();

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.setSecWebSocketProtocol(Arrays.asList("echo"));
        THIS.wsUrl = "ws://localhost:" + THIS.server.getPort() + "/test";

        THIS.client.setTaskExecutor(new SimpleAsyncTaskExecutor());
        THIS.wsSession = THIS.client.doHandshake(new ClientTextWebSocketHandler(), headers, new URI(THIS.wsUrl)).get();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try{
                    while(true) {
                        THIS.wsSession.sendMessage(new BinaryMessage("Hellow world".getBytes()));
                        Thread.sleep(3000);
                    }
                }catch (Exception e){

                }
            }
        };
        new Thread(runnable).start();
    }



    private static class TestJettyWebSocketServer {

        private final Server server;


        public TestJettyWebSocketServer(final WebSocketHandler webSocketHandler) {

            this.server = new Server();
            ServerConnector connector = new ServerConnector(this.server);
            connector.setPort(0);

            this.server.addConnector(connector);
            this.server.setHandler(new org.eclipse.jetty.websocket.server.WebSocketHandler() {
                @Override
                public void configure(WebSocketServletFactory factory) {
                    factory.setCreator(new WebSocketCreator() {
                        @Override
                        public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
                            if (!CollectionUtils.isEmpty(req.getSubProtocols())) {
                                resp.setAcceptedSubProtocol(req.getSubProtocols().get(0));
                            }
                            JettyWebSocketSession session = new JettyWebSocketSession(null, null);
                            return new JettyWebSocketHandlerAdapter(webSocketHandler, session);
                        }
                    });
                }
            });
        }

        public void start() throws Exception {
            this.server.start();
        }

        public void stop() throws Exception {
            this.server.stop();
        }

        public int getPort() {
            return ((ServerConnector) this.server.getConnectors()[0]).getLocalPort();
        }
    }
    private static class ServerTextWebSocketHandler extends AbstractWebSocketHandler{
        public ServerTextWebSocketHandler() {
        }

        protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
            ByteBuffer byteBuffer =  message.getPayload();
            System.out.println("server:"+ new String(byteBuffer.array()));
        }

        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try{
                        while(true) {
                            session.sendMessage(new BinaryMessage("World Hellow".getBytes()));
                            Thread.sleep(3000);
                        }
                    }catch (Exception e){

                    }
                }
            };
            new Thread(runnable).start();
        }
    }
    private static class ClientTextWebSocketHandler extends AbstractWebSocketHandler{
        public ClientTextWebSocketHandler() {
        }

        protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
            ByteBuffer byteBuffer =  message.getPayload();
            System.out.println("client:"+ new String(byteBuffer.array()));
        }

        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            //session.sendMessage(new BinaryMessage("Hellow world".getBytes()));
        }
    }
}
