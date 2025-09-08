package chatapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.util.Base64;

public class Client extends Application {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private TextArea chat;
    private TextField input;
    private TextField targetField; // "@nick" or leave blank for group
    private String nickname = "User" + (int)(Math.random()*900+100);

    // DEMO PURPOSES ONLY
    private static final String AES_KEY_16 = "0123456789ABCDEF";

    @Override
    public void start(Stage stage) {
        // UI
        chat = new TextArea();
        chat.setEditable(false);
        chat.setWrapText(true);

        input = new TextField();
        input.setPromptText("Type a message... (use @nick for private)");

        targetField = new TextField();
        targetField.setPromptText("@nick or leave empty for group");
        targetField.setPrefWidth(200);

        Button send = new Button("Send");
        send.setDefaultButton(true);

        HBox bottom = new HBox(8, targetField, input, send);
        bottom.setPadding(new Insets(8));

        BorderPane root = new BorderPane(chat, null, null, bottom, null);
        Scene scene = new Scene(root, 640, 420);

        // Connect dialog
        Dialog<Boolean> dlg = new Dialog<>();
        dlg.setTitle("Connect to Server");
        TextField hostField = new TextField("127.0.0.1");
        TextField portField = new TextField("12345");
        TextField nickField = new TextField(nickname);

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10); gp.setPadding(new Insets(10));
        gp.addRow(0, new Label("Host:"), hostField);
        gp.addRow(1, new Label("Port:"), portField);
        gp.addRow(2, new Label("Nickname:"), nickField);
        dlg.getDialogPane().setContent(gp);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.setResultConverter(bt -> bt == ButtonType.OK);
        dlg.showAndWait().ifPresent(ok -> {});

        nickname = nickField.getText().trim().isEmpty() ? nickname : nickField.getText().trim();

        try {
            socket = new Socket(hostField.getText().trim(), Integer.parseInt(portField.getText().trim()));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // Handshake
            out.println("NICK|" + nickname);

            // Reader thread
            Thread reader = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        handleIncoming(line);
                    }
                } catch (IOException e) {
                    append("[System] Disconnected: " + e.getMessage());
                }
            });
            reader.setDaemon(true);
            reader.start();

        } catch (Exception e) {
            append("[System] Could not connect: " + e.getMessage());
        }

        // Send handlers
        Runnable sendAction = () -> {
            String txt = input.getText().trim();
            if (txt.isEmpty()) return;
            String to = "*";
            String tf = targetField.getText().trim();
            if (!tf.isEmpty()) {
                if (tf.startsWith("@")) to = tf.substring(1);
                else to = tf;
            }

            try {
                String payload = encryptAES(txt);
                String wire = "MSG|" + nickname + "|" + to + "|" + payload;
                out.println(wire);
                if ("*".equals(to)) {
                    append("[Me → All] " + txt);
                }
                input.clear();
            } catch (Exception ex) {
                append("[Error] " + ex.getMessage());
            }
        };

        send.setOnAction(e -> sendAction.run());
        input.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) sendAction.run(); });

        stage.setTitle("Chat Client - " + nickname);
        stage.setScene(scene);
        stage.show();
    }

    private void handleIncoming(String line) {
        try {
            if (line.startsWith("SYS|")) {
                String[] p = line.split("\\|", 4);
                append("[System] " + p[3]);
                return;
            }
            if (!line.startsWith("MSG|")) return;

            String[] parts = line.split("\\|", 4);
            if (parts.length < 4) return;
            String from = parts[1];
            String to = parts[2];
            String payload = parts[3];

            if ("*".equals(to) || to.equals(nickname) || from.equals(nickname)) {
                String msg = decryptAES(payload);
                String tag = "*".equals(to) ? "All" : ("@" + to);
                if (from.equals(nickname) && !"*".equals(to)) {
                    append("[Me → " + tag + "] " + msg);
                } else {
                    append("[" + from + " → " + tag + "] " + msg);
                }
            }
        } catch (Exception e) {
            append("[Error] " + e.getMessage());
        }
    }

    private void append(String s) {
        Platform.runLater(() -> chat.appendText(s + "\n"));
    }

    private String encryptAES(String plain) throws Exception {
        SecretKeySpec key = new SecretKeySpec(AES_KEY_16.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes()));
    }

    private String decryptAES(String base64) throws Exception {
        SecretKeySpec key = new SecretKeySpec(AES_KEY_16.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decoded = Base64.getDecoder().decode(base64);
        return new String(cipher.doFinal(decoded));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
