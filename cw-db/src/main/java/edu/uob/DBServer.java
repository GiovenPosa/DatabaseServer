package edu.uob;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.nio.file.Files;


/** This class implements the DB server. */
public class DBServer {

    private static final char END_OF_TRANSMISSION = 4;
    private String storageFolderPath;
    private String selectedDatabase;

    public static void main(String args[]) throws IOException {
        DBServer server = new DBServer();
        server.blockingListenOn(8888);
    }

    /**
    * KEEP this signature otherwise we won't be able to mark your submission correctly.
    */
    public DBServer() {
        storageFolderPath = Paths.get("databases").toAbsolutePath().toString();
        try {
            // Create the database storage folder if it doesn't already exist !
            Files.createDirectories(Paths.get(storageFolderPath));
        } catch(IOException ioe) {
            System.out.println("Can't seem to create database storage folder " + storageFolderPath);
        }
    }

    /**
    * KEEP this signature (i.e. {@code edu.uob.DBServer.handleCommand(String)}) otherwise we won't be
    * able to mark your submission correctly.
    *
    * <p>This method handles all incoming DB commands and carries out the required actions.
    */

    private String sanitizeSpc(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replaceAll("\\s+", " ");
    }

    public String handleCommand(String command) {

        command = sanitizeSpc(command);
        if (!command.endsWith(";")) {
            return "[ERROR]: command must end with ';'";
        }
        command = command.substring(0, command.length() - 1).trim();

        DBFileManager fileManager = new DBFileManager(storageFolderPath);
        DBTableManager tableManager = new DBTableManager(storageFolderPath);
        UpdateQueryCmd updateQueryCmd = new UpdateQueryCmd(storageFolderPath, selectedDatabase);
        SelectQueryCmd selectQueryCmd = new SelectQueryCmd(storageFolderPath, selectedDatabase);
        DeleteQueryCmd deleteQueryCmd = new DeleteQueryCmd(storageFolderPath, selectedDatabase);
        JoinQueryCmd joinQueryCmd = new JoinQueryCmd(storageFolderPath, selectedDatabase);

        if (command.startsWith("CREATE DATABASE ")) {
            String filename = command.substring("CREATE DATABASE ".length());
            return fileManager.createDatabaseFile(filename);
        }

        if (command.startsWith("DROP DATABASE ")) {
            String filename = command.substring("DROP DATABASE ".length());
            return fileManager.dropDatabaseFile(filename);
        }

        if (command.startsWith("USE ")) {
            String filename = command.substring(4);
            String response = fileManager.switchDatabaseFile(filename);
            if (response.startsWith("[ERROR]:")) {
                return response;
            } else {
                selectedDatabase = response;
                return "[OK]: Switched to database file: " + filename;
            }
        }

        if (command.startsWith("CREATE TABLE ")) {
            String tableArgs = command.substring("CREATE TABLE ".length());
            return tableManager.createTable(tableArgs, selectedDatabase);
        }

        if (command.startsWith("DROP TABLE ")) {
            String tableName = command.substring("DROP TABLE ".length());
            return tableManager.dropTable(tableName, selectedDatabase);
        }

        if (command.startsWith("ALTER TABLE ")) {
            String tableArgs = command.substring("ALTER TABLE ".length());
            return tableManager.alterTable(tableArgs, selectedDatabase);
        }

        if (command.startsWith("INSERT INTO ")) {
            String tableArgs = command.substring("INSERT INTO ".length());
            return tableManager.insertAttribute(tableArgs, selectedDatabase);
        }

        if (command.startsWith("UPDATE ")) {
            String updateArgs = command.substring("UPDATE ".length());
            return updateQueryCmd.execute(updateArgs);
        }

        if (command.startsWith("SELECT ")) {
            String selectArgs = command.substring("SELECT ".length());
            return selectQueryCmd.execute(selectArgs);
        }

        if (command.startsWith("DELETE FROM ")) {
            String deleteArgs = command.substring("DELETE FROM ".length());
            return deleteQueryCmd.execute(deleteArgs);
        }

        if (command.startsWith("JOIN ")) {
            String joinArgs = command.substring("JOIN ".length());
            return joinQueryCmd.execute(joinArgs);
        }

        if (command.equals("READ")) {
            return fileManager.readDatabaseFile(selectedDatabase);
        }

        if (selectedDatabase == null) {
            return "[ERROR]: no database selected. Use 'USE <filename>' first.";
        }
        return "[ERROR]: unknown command: " + command;
    }


    //  === Methods below handle networking aspects of the project - you will not need to change these ! ===
    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(s);
                } catch (IOException e) {
                    System.err.println("Server encountered a non-fatal IO error:");
                    e.printStackTrace();
                    System.err.println("Continuing...");
                }
            }
        }
    }

    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {

            System.out.println("Connection established: " + serverSocket.getInetAddress());
            while (!Thread.interrupted()) {
                String incomingCommand = reader.readLine();
                System.out.println("Received message: " + incomingCommand);
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }
}
