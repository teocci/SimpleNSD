package com.github.teocci.simplensd.nio;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.github.teocci.simplensd.utils.LogHelper;
import com.github.teocci.simplensd.utils.NonBlocking.NonBlockingHashMap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by teocci on 3/22/17.
 */

public class SocketConnection
{
    private static final String TAG = LogHelper.makeLogTag(SocketConnection.class);

    private Handler updateHandler;
    private ServerConnection serverConnection;
//    private ClientConnection clientConnection;

    private AtomicInteger numThreads = new AtomicInteger(0);
    // the list of threads is kept in a linked list
//    private ArrayList<ClientConnection> clientConnections = new ArrayList<>();
//    private List clientConnections = Collections.synchronizedList(new ArrayList<ClientConnection>());
    private Map<Socket, ClientConnection> clientConnections = new NonBlockingHashMap<>();

//    private Socket socket;
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
        if (clientConnections != null) {
            long i = 0;
            Iterator<NonBlockingHashMap<Socket, ClientConnection>> it = clientConnections.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Integer> pair = it.next();
                i += pair.getKey() + pair.getValue();
            }
        }
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

    protected NonBlockingHashMap<Socket, ClientConnection> getClientConnections()
    {
        return clientConnections;
    }

    public AtomicInteger getNumThreads()
    {
        return numThreads;
    }
}