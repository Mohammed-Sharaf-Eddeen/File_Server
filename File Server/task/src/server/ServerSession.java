package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class ServerSession implements Runnable {
    private DataOutputStream output;
    private DataInputStream input;
    //get and delete requests, the user specify the name or ID of the required file where it would be mapped to the name
    private String fileName;
    //put requests, the user can choose to re-name the file after sending it.
    private String fileNameOnServer;
    public static final FilesIdentifiers filesIdentifiers = FilesIdentifiers.deserialize();

    public ServerSession(Socket socket) {
        try {
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String receivedMsg = input.readUTF();
            String[] parts = receivedMsg.split("\\s+", 3);
            Commands command = Commands.valueOf(parts[0]);
            if (command == Commands.exit) {
                Server.close();
            }
            String fileNameOrID = parts[1];
            fileName = filesIdentifiers.alwaysGetFileName(fileNameOrID);
            if (parts.length > 2) {
                fileNameOnServer = parts[2];
            }
            String responseCode = handleRequest(command, input, output);
            output.writeUTF(responseCode);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String handleRequest(Commands command, DataInputStream input, DataOutputStream output) {
        String response;
        switch (command) {
            case GET:
                response = get(fileName, output);
                return response;
            case SAVE:
                response = save(fileNameOnServer, input);
                return response;
            case DELETE:
                if (!filesIdentifiers.hasFileName(fileName)) {
                    return "404";
                }
                response = delete(fileName);
                return response;
        }
        return "400";
    }

    private String get(String fileName, DataOutputStream output) {
        if (!filesIdentifiers.hasFileName(fileName)) {
            getNonExistentFile(output);
            return "404";
        }

        Path root = Path.of(System.getProperty("user.dir"), "src", "server", "data", fileName);
        try {
            byte[] fileBytes = Files.readAllBytes(root);
            System.out.println(fileBytes.length);
            //to let the client know the length of the byte stream
            output.writeInt(fileBytes.length);
            //the byte stream itself
            output.write(fileBytes);
            return "200";
        } catch (IOException e) {
            getNonExistentFile(output);
            return "404";
        }
    }

    //to send data for the waiting calls for receiving the file
    private void getNonExistentFile(DataOutputStream output) {
        try {
            output.writeInt(0);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private String save(String fileNameOnServer, DataInputStream input) {
        if (fileNameOnServer == null || fileNameOnServer.isBlank()) {
            fileNameOnServer = filesIdentifiers.createFileName();
        }
        Path root = Path.of(System.getProperty("user.dir"), "src", "server", "data", fileNameOnServer);
        if (Files.notExists(root)) {
            try {
                int fileBytesLength = input.readInt();
                byte[] fileBytes = new byte[fileBytesLength];
                input.readFully(fileBytes, 0, fileBytesLength);
                Files.write(root, fileBytes);
                Long ID = filesIdentifiers.addFileName(fileNameOnServer);
                return "200" + " " + ID;
            } catch (IOException e) {
                return "403";
            }

        } else {
            return "403";
        }
    }

    private String delete(String fileName) {
        Path root = Path.of(System.getProperty("user.dir"), "src", "server", "data", fileName);
        if (Files.exists(root)) {
            try {
                Files.deleteIfExists(root);
                filesIdentifiers.deleteFileName(fileName);
                return "200";
            } catch (IOException e) {
                return "404";
            }
        } else {
            return "404";
        }
    }
}
