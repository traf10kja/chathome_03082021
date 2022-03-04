package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class ClientHandler {
    public static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    Server server;
    Socket socket;
    ExecutorService service;
    DataInputStream in;
    DataOutputStream out;

    private boolean authenticated;
    private String nickname;
    private String login;

    public ClientHandler(Socket socket, Server server, ExecutorService service) {
        try {
            this.socket = socket;
            this.server = server;
            this.service = service;

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

//            new Thread(() -> {
            service.execute(() -> {
                try {
                    socket.setSoTimeout(120000);
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.equals("/end")) {
                            sendMsg("/end");
                            logger.info("Client disconnected");
                            break;
                        }
                        if (str.startsWith("/auth ")) {
                            String[] token = str.split("\\s+");
                            nickname = server.getAuthService().
                                    getNickNameByLoginAndPassword(token[1], token[2]);
                            login = token[1];
                            if (nickname != null) {
                                if (!server.isLoginAuthenticated(login)) {
                                    sendMsg("/authok " + nickname);
                                    server.subscribe(this);
                                    authenticated = true;

                                    break;
                                }
                                sendMsg("Authorized login");
                            } else {
                                sendMsg("Wrong login/password");
                            }
                        }
                        if (str.startsWith("/reg ")) {
                            String[] token = str.split("\\s+");
                            if (token.length < 4) {
                                continue;
                            }
                            boolean regOk = server.getAuthService().
                                    registration(token[1], token[2], token[3]);
                            if (regOk) {
                                sendMsg("/regok");
                            } else {
                                sendMsg("/regno");
                            }
                        }
                    }
                    //цикл работы
                    while (authenticated) {
                        socket.setSoTimeout(0);
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                sendMsg("/end");
                                logger.info("Client disconnected");
                                break;
                            }
                            if (str.startsWith("/w")) {
                                String[] token = str.split("\\s+", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                server.privateMsg(this, token[1], token[2]);
                            }
                        } else {
                            server.broadcastMsg(this, str);
                        }
                    }

                } catch (SocketTimeoutException esoc) {
                    sendMsg("/end");
                }
                catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unSubscribe(this);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
