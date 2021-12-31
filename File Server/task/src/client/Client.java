package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class Client {
    private final static Scanner scanner = new Scanner(System.in);
    private final static String address = "127.0.0.1";
    private final static int port = 23456;

    public static void main (String[] args) {
        try (Socket socket = new Socket(InetAddress.getByName(address), port)) {
            try (DataInputStream input = new DataInputStream(socket.getInputStream());
                 DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

                String requestMessage = "";
                System.out.print("Enter action (1 - get a file, 2 - save a file, 3 - delete a file): ");

                String option = scanner.nextLine().strip();

                switch (option) {
                    case "1":
                        requestMessage = createGetRequest();
                        output.writeUTF(requestMessage);
                        executeGetRequest(requestMessage, input);
                        break;
                    case "2":
                        requestMessage = createSaveRequest();
                        output.writeUTF(requestMessage);
                        executeSaveRequest(requestMessage, output);
                        break;
                    case "3":
                        requestMessage = createDeleteRequest();
                        output.writeUTF(requestMessage);
                        break;
                    case "exit":
                        requestMessage = "exit";
                        output.writeUTF(requestMessage);
                        break;
                    default:
                        System.out.println("An error in the the request has occurred.");
                        break;
                }

                System.out.println("The request was sent.");
                if ("exit".equals(option)) {
                    return;
                }

                String receivedMsg = input.readUTF();
                processServerResponse(option, receivedMsg, requestMessage);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.out.println("The server may be down right now. Please, Try Again Later!");
        }
    }

    private static void processServerResponse(String option, String receivedMsg, String requestMessage) {
        switch (option) {
            case "1":
                if (receivedMsg.startsWith("200")) {
                    System.out.print("The file was downloaded! Specify a name for it: > ");
                    String newFileName = scanner.nextLine().strip();
                    changeDownloadedFileName(requestMessage, newFileName);
                    System.out.println("File saved on the hard drive!");

                } else {
                    System.out.println("The response says that this file is not found!");
                }
                break;
            case "2":
                if (receivedMsg.startsWith("200")) {
                    String[] parts = receivedMsg.split("\\s+", 2);
                    System.out.println("Response says that file is saved! ID = " + parts[1]);
                } else {
                    System.out.println("The response says that creating the file was forbidden!");
                }
                break;
            case "3":
                if (receivedMsg.startsWith("200")) {
                    System.out.println("The response says that the file was successfully deleted!");
                } else {
                    System.out.println("The response says that the file is not found!");
                }
                break;
        }
    }

    private static String createGetRequest() {
        System.out.print("Do you want to get the file by name or by id (1 - name, 2 - id): > ");
        int idOrName = Integer.parseInt(scanner.nextLine().strip());
        if (idOrName == 1) {
            System.out.print("Enter filename: ");
            String fileName = scanner.nextLine().strip();
            return "GET " + fileName;
        } else {
            System.out.print("Enter id: ");
            String id = scanner.nextLine().strip();
            return "GET " + id;
        }
    }

    private static String createSaveRequest() {
        System.out.print("Enter name of the file: > ");
        String fileName = scanner.nextLine().strip();
        System.out.print("Enter name of the file to be saved on server: > ");
        String serverFileName = scanner.nextLine().strip();

        return "SAVE " + fileName + " " + serverFileName;
    }

    private static String createDeleteRequest() {
        System.out.print("Do you want to delete the file by name or by id (1 - name, 2 - id): > ");
        int idOrName = Integer.parseInt(scanner.nextLine().strip());
        if (idOrName == 1) {
            System.out.print("Enter filename: ");
            String fileName = scanner.nextLine().strip();
            return "DELETE " + fileName;
        } else {
            System.out.print("Enter id: ");
            String id = scanner.nextLine().strip();
            return "DELETE " + id;
        }
    }

    private static void executeGetRequest(String requestMessage, DataInputStream input) {
        String[] parts = requestMessage.split("\\s+", 2);
        String fileName = parts[1];
        Path filePath = Path.of(System.getProperty("user.dir"), "src", "client", "data", fileName);

        try {
            int fileBytesLength = input.readInt();
            /*
            If the file doesn't exist on the server, the server would send zero to let
            the client prevent waiting for further connections
            */
            if (fileBytesLength == 0) {
                return;
            }
            byte[] fileBytes = new byte[fileBytesLength];
            input.readFully(fileBytes, 0, fileBytesLength);
            Files.write(filePath, fileBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //save a file by sending another two messages using byte stream
    private static void executeSaveRequest(String requestMessage, DataOutputStream output) {
        String[] parts = requestMessage.split("\\s+", 3);
        String fileName = parts[1];
        Path filePath = Path.of(System.getProperty("user.dir"),  "src", "client", "data", fileName);
        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            //to let the server know the length of the byte stream
            output.writeInt(fileBytes.length);
            //the byte stream itself
            output.write(fileBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //after downloading the file, change its name to a new name
    private static void changeDownloadedFileName(String requestMessage, String newFileName) {
        String[] parts = requestMessage.split("\\s+", 2);
        String fileName = parts[1];
        Path filePath = Path.of(System.getProperty("user.dir"), "src", "client", "data", fileName);
        File file = new File(String.valueOf(filePath));
        Path newFilePath = Path.of(System.getProperty("user.dir"), "src", "client", "data", newFileName);
        File newFile = new File(String.valueOf(newFilePath));
        boolean succeeded = file.renameTo(newFile);
        if (!succeeded) {
            System.out.println("Failed to save the file with the new name");
        }
    }
}