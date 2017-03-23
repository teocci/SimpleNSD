package com.github.teocci.simplensd.nio;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.github.teocci.simplensd.utils.LogHelper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by teocci on 3/22/17.
 */

public class SocketConnection
{
    private static final String TAG = LogHelper.makeLogTag(SocketConnection.class);

    private Handler updateHandler;
    private ServerConnection serverConnection;
    private ClientConnection clientConnection;

    private Socket socket;
    private int localPort = -1;

    public SocketConnection(Handler handler)
    {
        updateHandler = handler;
        serverConnection = new ServerConnection(handler);
        serverConnection.setConnection(this);
    }

    public void tearDown()
    {
        if (serverConnection != null)
            serverConnection.tearDown();
        if (clientConnection != null)
            clientConnection.tearDown();
    }

    public void connectToServer(InetAddress address, int port)
    {
        clientConnection = new ClientConnection(address, port);
        clientConnection.setConnection(this);
    }

    public void sendMessage(String msg)
    {
        if (clientConnection != null) {
            clientConnection.sendMessage(msg);
        }
    }

    public int getLocalPort()
    {
        return localPort;
    }

    public void setLocalPort(int port)
    {
        localPort = port;
    }

    public synchronized void updateMessages(String msg, boolean local)
    {
        LogHelper.e(TAG, "Updating message: " + msg);

        if (local) {
            msg = "me: " + msg;
        } else {
            msg = "them: " + msg;
        }

        Bundle messageBundle = new Bundle();
        messageBundle.putString("msg", msg);

        Message message = new Message();
        message.setData(messageBundle);
        updateHandler.sendMessage(message);

    }

    protected synchronized void setSocket(Socket socket)
    {
        LogHelper.d(TAG, "setSocket being called.");
        if (socket == null) {
            LogHelper.d(TAG, "Setting a null socket.");
        }
        if (this.socket != null) {
            if (this.socket.isConnected()) {
                try {
                    this.socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        this.socket = socket;
    }

    protected Socket getSocket()
    {
        return socket;
    }

    public ClientConnection getClientConnection()
    {
        return clientConnection;
    }
}