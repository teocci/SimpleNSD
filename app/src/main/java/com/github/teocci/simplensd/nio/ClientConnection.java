package com.github.teocci.simplensd.nio;

import com.github.teocci.simplensd.utils.LogHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by teocci on 3/22/17.
 *
 */

public class ClientConnection
{
    private static final String TAG = LogHelper.makeLogTag(ClientConnection.class);

    private InetAddress mAddress;
    private int PORT;

    private Thread sendThread;
    private Thread receiveThread;

    private SocketConnection connection;

    protected ClientConnection(InetAddress address, int port)
    {
        LogHelper.d(TAG, "Creating chatClient");
        this.mAddress = address;
        this.PORT = port;

        sendThread = new Thread(new SendingThread());
        sendThread.start();
    }

    protected void setConnection(SocketConnection connection)
    {
        this.connection = connection;
    }

    public void tearDown()
    {
        try {
            connection.getSocket().close();
        } catch (IOException ioe) {
            LogHelper.e(TAG, "Error when closing server socket.");
        }
    }

    public void sendMessage(String msg)
    {
        try {
            Socket socket = connection.getSocket();
            if (socket == null) {
                LogHelper.d(TAG, "Socket is null, wtf?");
            } else if (socket.getOutputStream() == null) {
                LogHelper.d(TAG, "Socket output stream is null, wtf?");
            }

            PrintWriter out = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(connection.getSocket().getOutputStream())), true);
            out.println(msg);
            out.flush();
            connection.updateMessages(msg, true);
        } catch (UnknownHostException e) {
            LogHelper.d(TAG, "Unknown Host", e);
        } catch (IOException e) {
            LogHelper.d(TAG, "I/O Exception", e);
        } catch (Exception e) {
            LogHelper.d(TAG, "Error3", e);
        }
        LogHelper.d(TAG, "Client sent message: " + msg);
    }

    private class ReceivingThread implements Runnable
    {
        @Override
        public void run()
        {
            BufferedReader input;
            try {
                input = new BufferedReader(new InputStreamReader(
                        connection.getSocket().getInputStream()));
                while (!Thread.currentThread().isInterrupted()) {

                    String messageStr = null;
                    messageStr = input.readLine();
                    if (messageStr != null) {
                        LogHelper.d(TAG, "Read from the stream: " + messageStr);
                        connection.updateMessages(messageStr, false);
                    } else {
                        LogHelper.d(TAG, "The nulls! The nulls!");
                        break;
                    }
                }
                input.close();

            } catch (IOException e) {
                LogHelper.e(TAG, "Server loop error: ", e);
            }
        }
    }

    private class SendingThread implements Runnable
    {
        BlockingQueue<String> messageQueue;
        private int QUEUE_CAPACITY = 10;

        public SendingThread()
        {
            messageQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        }

        @Override
        public void run()
        {
            try {
                if (connection.getSocket() == null) {
                    connection.setSocket(new Socket(mAddress, PORT));
                    LogHelper.d(TAG, "Client-side socket initialized.");

                } else {
                    LogHelper.d(TAG, "Socket already initialized. skipping!");
                }

                receiveThread = new Thread(new ReceivingThread());
                receiveThread.start();

            } catch (UnknownHostException e) {
                LogHelper.d(TAG, "Initializing socket failed, UHE", e);
            } catch (IOException e) {
                LogHelper.d(TAG, "Initializing socket failed, IOE.", e);
            }

            while (true) {
                try {
                    String msg = messageQueue.take();
                    sendMessage(msg);
                } catch (InterruptedException ie) {
                    LogHelper.d(TAG, "Message sending loop interrupted, exiting");
                }
            }
        }
    }
}
