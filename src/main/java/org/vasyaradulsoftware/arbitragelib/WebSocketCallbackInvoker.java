package org.vasyaradulsoftware.arbitragelib;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class WebSocketCallbackInvoker extends WebSocketClient {

    private Consumer<String> callback;
    
    public WebSocketCallbackInvoker(String url, Consumer<String> callback) throws URISyntaxException {
        super(new URI(url));

        this.callback = callback;

        System.out.println("Connecting to " + url);
        try {
            this.connectBlocking();
        } catch (InterruptedException e) {
            System.out.println(url + " connection error.");
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(String message) {
        callback.accept(message);
    }

    @Override
	public void onOpen(ServerHandshake handshakedata) {
        System.out.println(this.uri.getPath() + " connected successful. Handshake: " + handshakedata);
	}

    @Override
	public void onMessage(ByteBuffer message) {
	}

    @Override
	public void onClose(int code, String reason, boolean remote) {
        System.out.println("closed with exit code " + code + " additional info: " + reason);
	}

	@Override
	public void onError(Exception e) {
        System.out.println(e);
	}
}
