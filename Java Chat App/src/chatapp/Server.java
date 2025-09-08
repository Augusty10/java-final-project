package chatapp;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chat Server
 * - Handles multiple clients (threads)
 * - Supports group and private messaging
 * - Keeps connection logs
 * - Forwards encrypted payloads without decrypting
 * Protocol (plaintext header + encrypted payload):
 *   MSG|fromNick|toNickOr*|<base64-ciphertext>
 */
public class Server {
    public static final int PORT = 12345;

    // nickname -> client writer
    private static final Map<String, PrintWriter> clients = new ConcurrentHashMap<>();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final File LOG_FILE = new File("server.log");

    public static void main(String[] args) {
        log("Server starting on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            log("FATAL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static void log(String line) {
        String out = "[" + TS.format(LocalDateTime.now()) + "] " + line;
        System.out.println(out);
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(out);
        } catch (IOException ignored) {}
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname = null;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

                // First line must be NICK|desiredNickname
                String first = in.readLine();
                if (first == null || !first.startsWith("NICK|")) {
                    out.println("ERR|Invalid handshake");
                    close();
                    return;
                }
                String desired = first.substring(5).trim();
                if (desired.isEmpty() || clients.containsKey(desired)) {
                    out.println("ERR|Nickname taken");
                    close();
                    return;
                }
                nickname = desired;
                clients.put(nickname, out);
                log(nickname + " connected from " + socket.getRemoteSocketAddress());
                broadcastRaw("SYS|Server|" + nickname + "|Welcome " + nickname + "! Type @user for private messages.", "*");
                broadcastRaw("SYS|Server|*|" + nickname + " joined the chat.", "*");

                String line;
                while ((line = in.readLine()) != null) {
                    // Expecting: MSG|from|to|ciphertext  (server doesn't decrypt)
                    if (line.startsWith("MSG|")) {
                        routeMessage(line);
                    } else if (line.startsWith("BYE")) {
                        break;
                    } else {
                        // ignore / unknown
                    }
                }
            } catch (IOException e) {
                log("IO error with " + nickname + ": " + e.getMessage());
            } finally {
                if (nickname != null) {
                    clients.remove(nickname);
                    broadcastRaw("SYS|Server|*|" + nickname + " left the chat.", "*");
                    log(nickname + " disconnected.");
                }
                close();
            }
        }

        private void routeMessage(String raw) {
            // raw form: MSG|from|to|payload
            try {
                String[] parts = raw.split("\\|", 4);
                if (parts.length < 4) return;
                String from = parts[1];
                String to = parts[2];
                String payload = parts[3];

                if ("*".equals(to)) {
                    // group message
                    for (Map.Entry<String, PrintWriter> entry : clients.entrySet()) {
                        if (!entry.getKey().equals(from)) {
                            entry.getValue().println(raw);
                        }
                    }
                    log("GROUP " + from + " -> * (" + payload.length() + " bytes)");
                } else {
                    PrintWriter target = clients.get(to);
                    if (target != null) {
                        target.println(raw);
                        // echo back to sender for uniform display
                        PrintWriter sender = clients.get(from);
                        if (sender != null) sender.println(raw);
                        log("PRIVATE " + from + " -> " + to + " (" + payload.length() + " bytes)");
                    } else {
                        PrintWriter sender = clients.get(from);
                        if (sender != null) sender.println("SYS|Server|" + from + "|User '" + to + "' not online.");
                    }
                }
            } catch (Exception e) {
                log("routeMessage error: " + e.getMessage());
            }
        }

        private void broadcastRaw(String sysLine, String to) {
            for (PrintWriter w : clients.values()) {
                w.println(sysLine);
            }
        }

        private void close() {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
