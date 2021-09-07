package com.db.edu.client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {
    private final String HOST;
    private final int PORT;

    /**
     * Configure HOST and PORT for client.
     * @param host (String)
     * @param port (int)
     */
    public Client(String host, int port) {
        HOST = host;
        PORT = port;
    }

    /**
     * Set up connection with the server. Define input and output streams.
     * Settle up isolated thread for message receiving.
     */
    public void connect() {
        try (Socket connection = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

            final Scanner sc = new Scanner(System.in);
            String str;

            MessageReceiver receiver = new MessageReceiver(in);
            receiver.start();

            do {
                str = sc.nextLine();
                out.println(str);
            } while (!str.equals("exit"));

            receiver.setStop();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

}
