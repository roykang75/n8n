package xyz.oiio.n8n.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import xyz.oiio.n8n.push.PushWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final PushWebSocketHandler pushWebSocketHandler;

    public WebSocketConfig(PushWebSocketHandler pushWebSocketHandler) {
        this.pushWebSocketHandler = pushWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pushWebSocketHandler, "/rest/push")
                .setAllowedOrigins("*"); // Allow all origins for dev/proxy
    }
}
