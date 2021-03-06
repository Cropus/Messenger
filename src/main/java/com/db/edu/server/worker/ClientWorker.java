package com.db.edu.server.worker;

import com.db.edu.exception.*;
import com.db.edu.exception.CommandProcessException;
import com.db.edu.exception.MessageTooLongException;
import com.db.edu.server.model.User;
import com.db.edu.server.service.Service;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class ClientWorker extends Thread {
    private Socket socket;
    private Service userService;
    private User user;
    private PrintWriter out;
    private BufferedReader in;
    private boolean running;

    /**
     * Initialize ClientWorker and define input and output streams.
     * @param socket (Socket)
     * @param user (User)
     * @param userService (Service)
     */
    public ClientWorker(Socket socket, User user, Service userService) {
        this.socket = socket;
        this.userService = userService;
        this.user = user;
        this.running = true;
        try {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Can't get Reader or Writer from socket");
        }
    }

    public User getUser() {
        return user;
    }

    @Override
    public void run() {
        sendGreeting();
        while (running) {
            try {
                processCommand(in.readLine());
            } catch (SocketException e) {
                disconnectUser();
            } catch (UserNotIdentifiedException e) {
                sendMessage("You have not set your nickname yet! Please do so by using '/chid nickname'.");
            } catch (UnknownCommandException e) {
                sendMessage("Unknown command: " + e.getMessage());
            } catch (NicknameSettingException e) {
                sendMessage("Error setting nickname: " + e.getMessage());
            } catch (MessageTooLongException e) {
                sendMessage("Your message is too long! Our chat only supports messages up to 150 symbols.");
            } catch (RoomNameTooLongException e) {
                sendMessage("Your room name is too long, should be at most 20 characters!");
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    /**
     * Boolean flag for stop running.
     */
    public void stopRunning() {
        this.running = false;
        try {
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("Can't stop worker correctly");
        }
    }

    private void disconnectUser() {
        userService.disconnectUser(user);
    }

    private void sendGreeting() {
        sendMessage("Welcome to the chat!");
        sendMessage("Before you start chatting, please set your nickname using the '/chid nickname' command.");
        help();
    }

    public void help() {
        sendMessage("Available commands:");
        sendMessage("/help - list all possible commands");
        sendMessage("/snd message - sends a message to all users in the chat; message cannot be empty or longer than "
                 + "150 characters");
        sendMessage("/hist - get the full chat history");
        sendMessage("/chid nickname - set your nickname; nickname cannot be empty or longer than 20 characters");
        sendMessage("/chroom roomname - change a room; room name cannot be empty or longer than 20 characters");
    }

    private void processCommand(String command) throws CommandProcessException, IOException {
        if (command == null) {
            disconnectUser();
            return;
        }
        String[] tokens = command.trim().split("\\s+");
        String commandType = tokens[0];

        switch (commandType) {
            case "/hist":
                if (tokens.length > 1) {
                    throw new UnknownCommandException("/hist takes no arguments!");
                }
                userService.getMessagesFromRoom(user);
                break;
            case "/chid":
                if (tokens.length == 1) {
                    throw new UnknownCommandException("nickname is empty!");
                }
                if (tokens.length > 2) {
                    throw new InvalidNicknameException("nickname contains a whitespace character!");
                }
                userService.setUserNickname(tokens[1], user);
                break;
            case "/chroom":
                if (tokens.length != 2) {
                    throw new UnknownCommandException("room name contains a whitespace character!");
                }
                userService.setUserRoom(tokens[1], user);
                break;
            case "/snd":
                if (tokens.length == 1) {
                    throw new UnknownCommandException("message is empty!");
                }
                userService.saveAndSendMessage(extractMessage(command), user);
                break;
            case "/help":
                help();
                break;
            default:
                throw new UnknownCommandException("command not recognized!");
        }
    }

    private String extractMessage(String command) {
        return command.substring(command.indexOf(' ') + 1);
    }
}
