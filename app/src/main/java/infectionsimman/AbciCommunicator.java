package infectionsimman;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AbciCommunicator extends Communicator {
    public void copyToRemote(Path sourceDirectory, Path remoteDestination) {
        try {
            Process process = createSshProcess(null, "mkdir", "-p", remoteDestination.getParent().toString());
            process.waitFor();

            ProcessBuilder processBuilder = new ProcessBuilder("scp", "-r", sourceDirectory.toString(), "abci:" + remoteDestination.toString());
            processBuilder.start().waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getFileContents(Path file) throws IOException {
        Process process = createSshProcess(null, "cat", file.toString());
        InputStreamReader streamReader = new InputStreamReader(process.getInputStream());
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String result = new BufferedReader(streamReader).lines().collect(Collectors.joining());
        streamReader.close();
        return result;
    }

    public void putTextFile(Path file, String contents) throws IOException {
        Path tmpFile = Files.createTempFile("file", ".dat");
        Files.writeString(tmpFile, contents);

        ProcessBuilder processBuilder = new ProcessBuilder("scp", tmpFile.toString(), "abci:" + file.toString());
        try {
            processBuilder.start().waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Files.delete(tmpFile);
    }

    public String getScriptPreamble() {
        return "#!/bin/sh\n\n" +
                "#$ -l rt_C.small=1\n" +
                "#$ -l h_rt=0:30:00\n" +
                "#$ -j y\n" +
                "#$ -cwd\n\n";
    }

    public void run(Path scriptPath) {
        String group = "undefined";
        Path groupFile = Paths.get("abci_group.dat");
        if (Files.exists(groupFile)) {
            try {
                group = Files.readString(groupFile).trim();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            createSshProcess(scriptPath.getParent(), "chmod", "a+x", scriptPath.toString()).waitFor();
            Process p = createSshProcess(scriptPath.getParent(), "qsub", "-g", group, scriptPath.toString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean fileExists(Path path) {
        boolean result = false;
        try {
            Process process = createSshProcess(null, "test", "-e", path.toString());
            process.waitFor();
            result = (process.exitValue() == 0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    private Process createSshProcess(Path work, String... command) throws IOException {
        try {
            // elude attack protection
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String remoteCommand = "ssh abci \"sh -c '";
        if (work != null) {
            remoteCommand += "cd " + work.toString() + ";";
        }
        for (String s : command) {
            remoteCommand += s + " ";
        }
        remoteCommand += "'\"";
        ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", remoteCommand);
        return processBuilder.start();
    }
}
