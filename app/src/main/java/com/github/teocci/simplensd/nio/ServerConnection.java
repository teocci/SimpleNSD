package com.github.teocci.simplensd.nio;

import android.os.Handler;

import com.github.teocci.simplensd.utils.LogHelper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Created by teocci on 3/22/17.
 */

public class ServerConnection
{
    private static final String TAG = LogHelper.makeLogTag(ServerConnection.class);

    private ServerSocket serverSocket = null;
    private Thread serverThread = null;

    private SocketConnection connection;

    protected ServerConnection(Handler handler)
    {
        serverThread = new Thread(new ServerThread());
        serverThread.start();
    }

    protected void setConnection(SocketConnection connection)
    {
        this.connection = connection;
    }

    public void tearDown()
    {
        serverThread.interrupt();
        try {
            serverSocket.close();
        } catch (IOException ioe) {
            LogHelper.e(TAG, "Error when closing server socket.");
        }
    }

    private class ServerThread implements Runnable
    {
        @Override
        public void run()
        {
            try {
                // Since discovery will happen via Nsd, we don't need to care which port is
                // used.  Just grab an available one  and advertise it via Nsd.
                serverSocket = new ServerSocket(0);
                connection.setLocalPort(serverSocket.getLocalPort());

                while (!Thread.currentThread().isInterrupted()) {
                    LogHelper.d(TAG, "ServerSocket Created, awaiting connection");
                    connection.setSocket(serverSocket.accept());
                    LogHelper.d(TAG, "Connected.");
                    if (connection.getClientConnection() == null) {
                        int port = connection.getSocket().getPort();
                        InetAddress address = connection.getSocket().getInetAddress();
                        connection.connectToServer(address, port);
                    }
                }
            } catch (IOException e) {
                LogHelper.e(TAG, "Error creating ServerSocket: ", e);
                e.printStackTrace();
            }
        }
    }
}
