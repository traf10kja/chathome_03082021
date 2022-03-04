package server;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    public static final Logger logger = Logger.getLogger(Server.class.getName());
    private ServerSocket server;
    private Socket socket;
    private final int PORT = 8202;

    private ExecutorService service = Executors.newFixedThreadPool(10);

    private List<ClientHandler> clients;
    private AuthService authService;


    public Server() {

        clients = new CopyOnWriteArrayList();
//        authService = new SimpleAuthService();
        if (!SQLHandler.connect()) {
            throw new RuntimeException("Не удалось подключиться к БД!");
        }
        authService = new DBAuthService();

        try {
            server = new ServerSocket(PORT);
            logger.info("Server start!");

            while (true) {
                socket = server.accept();
                logger.info("Client connected");
                new ClientHandler(socket, this,service);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            SQLHandler.disconnect();

            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
                logger.log(Level.SEVERE,"Close ",e);
            }
        }
    }

    public void broadcastMsg(ClientHandler sender, String msg) {
        String message = String.format("[ %s ]: %s ", sender.getNickname(), msg);
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }

    public void privateMsg(ClientHandler sender, String nName, String msg) {
        String message = String.format("[ %s ] to [ %s ]: %s ", sender.getNickname(),nName, msg);
        for (ClientHandler c : clients) {
            if(c.getNickname().equals(nName)) {
                c.sendMsg(message);
                if(!c.equals(sender)){
                    sender.sendMsg(message);
                }
                return;
            }
        }
        sender.sendMsg("Not found user: " + nName);
    }

    public boolean isLoginAuthenticated(String login) {
        for (ClientHandler c : clients) {
          if (c.getLogin().equals(login)){
              return true;
          }
        }
        return false;
    }

    public void broadcastClientList() {
        StringBuilder sb = new StringBuilder("/clientlist");
        for (ClientHandler c : clients) {
           sb.append(" ").append(c.getNickname());
        }

        String message = sb.toString();
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
    }

    public void unSubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public AuthService getAuthService() {
        return authService;
    }
}
