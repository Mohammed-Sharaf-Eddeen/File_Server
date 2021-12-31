package server;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FilesIdentifiers implements Serializable {
    private static final long serialVersionUID = 7L;
    public static final String serializedFileName = String
            .valueOf(Path.of(System.getProperty("user.dir"), "src", "server", "FilesIdentifiers.data"));

    private final Map<Long, String> filesMap = new HashMap<>();
    private final Map<String, Long> filesMapReversed = new HashMap<>();
    public volatile long numberOfFiles = 0;

    public synchronized Long addFileName(String fileName) {
        numberOfFiles++;
        filesMap.put(numberOfFiles, fileName);
        filesMapReversed.put(fileName, numberOfFiles);
        return numberOfFiles;
    }

    public synchronized void deleteFileName(String fileName) {
        Long ID = filesMapReversed.get(fileName);
        filesMap.remove(ID);
        filesMapReversed.remove(fileName);
        numberOfFiles--;
    }

    public String alwaysGetFileName(String fileNameOrID) {
        try {
            Long ID = Long.parseLong(fileNameOrID);
            return getFileName(ID);
        } catch (NumberFormatException e) {
            return fileNameOrID;
        }
    }

    public String getFileName(Long ID) {
        return filesMap.getOrDefault(ID, null);
    }

    public boolean hasFileName(String fileName) {
        return filesMapReversed.containsKey(fileName);
    }

    public String createFileName() {
            Random random = new Random();
            return random.nextInt() + "-" + System.currentTimeMillis();
    }

    /**
     * Serialize the given object to the file
     */
    public static void serialize(FilesIdentifiers obj) {
        try {
            FileOutputStream fos = new FileOutputStream(serializedFileName);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deserialize to an object from the file
     */
    public static FilesIdentifiers deserialize() {
        try {
            FileInputStream fis = new FileInputStream(serializedFileName);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);
            FilesIdentifiers obj = (FilesIdentifiers) ois.readObject();
            ois.close();
            return obj;
        } catch (IOException | ClassNotFoundException e) {
            return new FilesIdentifiers();
        }
    }

}
