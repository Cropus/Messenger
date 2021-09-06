package com.db.edu.server;

import com.db.edu.dao.User;
import com.db.edu.server.worker.ClientWorker;
import com.db.edu.service.Service;
import com.db.edu.storage.DiscussionStorage;
import com.db.edu.storage.UsersController;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    public static void main(String[] args) {
        AtomicInteger ids = new AtomicInteger();
        Service userService = new Service();
        try (final ServerSocket listener = new ServerSocket(10000)) {
            while (true) {
                Socket userSocket = listener.accept();
                int id = ids.getAndIncrement();
                User user = new User(id);
                ClientWorker worker = new ClientWorker(userSocket, user, userService);
                UsersController.addUserConnection(id, worker);
                DiscussionStorage.addUserToDiscussion(id, 1);
                worker.start();
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}