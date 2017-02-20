package com.devinl.hermes.models;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by Alcha on 1/28/2017.
 */

public class Client {
    /**
     * Log tag for debugging and error checking.
     */
    private static final String LOG_TAG = "Client";
    private BufferedReader mSocketInput;
    private OutputStream mSocketOutput;
    private ClientCallback mListener;
    private Socket mSocket;
    private String mIP;
    private int mPort;

    public Client(String IP, int port) {
        mIP = IP;
        mPort = port;
    }

    public void connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mSocket = new Socket();
                InetSocketAddress socketAddress = new InetSocketAddress(mIP, mPort);

                try {
                    mSocket.connect(socketAddress);
                    mSocketOutput = mSocket.getOutputStream();
                    mSocketInput = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

                    new ReceiveThread().start();

                    if (mListener != null)
                        mListener.onConnect(mSocket);

                    Log.d(LOG_TAG, "Connection begun.");
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error connecting to socket:");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void disconnect() {
        try {
            mSocket.close();
        } catch (IOException e) {
            if (mListener != null)
                mListener.onDisconnect(mSocket, e.getMessage());
        }
    }

    public void send(String message) {
        try {
            mSocketOutput.write(message.getBytes());
        } catch (IOException e) {
            if (mListener != null)
                mListener.onDisconnect(mSocket, e.getMessage());
        }
    }

    public void setClientCallback(ClientCallback listener) {
        mListener = listener;
    }

    public void removeClientCallback() {
        mListener = null;
    }

    public interface ClientCallback {
        void onMessage(String message);

        void onConnect(Socket socket);

        void onDisconnect(Socket socket, String message);

        void onConnectError(Socket socket, String message);
    }

    private class ReceiveThread extends Thread implements Runnable {
        public void run() {
            String message;
            try {
                while ((message = mSocketInput.readLine()) != null) {   // each line must end with a \n to be received
                    if (mListener != null)
                        mListener.onMessage(message);
                }
            } catch (IOException e) {
                if (mListener != null)
                    mListener.onDisconnect(mSocket, e.getMessage());
            }
        }
    }
}