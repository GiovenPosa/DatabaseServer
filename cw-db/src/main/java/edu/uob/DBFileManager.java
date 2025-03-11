package edu.uob;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class DBFileManager {
    private final String storageFolderPath;
    private String selectedDatabase;

    public DBFileManager(String storageFolderPath) {
        this.storageFolderPath = storageFolderPath;
    }

    public String createDatabaseFile(String fileName) {
        File file = new File(storageFolderPath, fileName);

        if (file.exists()) {
            return "[ERROR]: Database already exits" + file.getAbsolutePath();
        }
        boolean created = file.mkdir();
        if (created) {
            return "[OK]: Database: '" + fileName + "' created successfully.";
        } else {
            return "[ERROR]: Database: '" + fileName + "' could not be created.";
        }
    }

    public String dropDatabaseFile(String fileName) {
        File file = new File(storageFolderPath, fileName);

        if (!file.exists()) {
            return "[ERROR]: Database '" + fileName + "' does not exist.";
        }

        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            boolean allFilesDeleted = true;
            if (childFiles != null) {
                for (File childFile : childFiles) {
                    boolean deletedChild = childFile.delete();
                    if (!deletedChild) {
                        allFilesDeleted = false;
                    }
                }
            }
            if (!allFilesDeleted) {
                return "[ERROR]: Not all files in database '" + fileName + "' could not be deleted.";
            }
            boolean deletedDatabase = file.delete();
            selectedDatabase = null;
            if (deletedDatabase) {
                return "[OK]: Database: '" + fileName + "' deleted successfully.";
            }
        }

        return "[ERROR]: Database: '" + fileName + "' could not be deleted.";
    }

    public String readDatabaseFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "[ERROR]: No filename provided. Use 'USE' <filename>' first.";
        }

        String destinationPath = storageFolderPath + File.separator + filename;
        String databaseName = filename + ".tab";

        File file = new File(destinationPath, databaseName);

        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException ioe) {
            return "[ERROR]: reading file: " + ioe.getMessage();
        }

        return content.toString();
    }

    public String switchDatabaseFile (String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "[ERROR]: No database provided. Use 'USE' <databaseName>' first.";
        }
        File databaseFile = new File(storageFolderPath, filename);

        if (!databaseFile.exists() || !databaseFile.isDirectory()) {
            return "[ERROR]: Database '" + filename + "' does not exist.";
        }

        this.selectedDatabase = filename;
        return this.selectedDatabase;
    }

}
