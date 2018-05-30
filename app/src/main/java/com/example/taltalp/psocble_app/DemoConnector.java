package com.example.taltalp.psocble_app;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class DemoConnector {
    private Manager manager;
    private Socket socket;
    private static final String BaseUri = "https://dasicdemo.herokuapp.com";

    public DemoConnector() {
    }

    public void InitConnection() {
        try {
            manager = new Manager(new URI(BaseUri));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        socket = manager.socket("/gw");
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
            }
        });
    }

    public void Connect() {
        socket.connect();
    }

    public boolean IsConnected() {
        return socket.connected();
    }

    public void Disconnect() {
        if (IsConnected()) {
            socket.disconnect();
        }
    }

    public void SendIllumination(int sensor_id, int illu) {
        // TODO: Connection check
        JSONObject req = new JSONObject();
        try {
            req.put("sensor_id", sensor_id);
            req.put("illumination", illu);
            req.put("timestamp", System.currentTimeMillis() / 1000L);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("illumination", req);
    }
}
