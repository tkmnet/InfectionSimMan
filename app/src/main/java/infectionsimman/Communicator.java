package infectionsimman;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Permissions;
import java.util.stream.Stream;

public class Communicator {
    public void copyToRemote(Path sourceDirectory, Path remoteDestination) {
        copyDirectory(sourceDirectory, remoteDestination);
    }

    private void copyDirectory(Path source, Path destination) {
        try (Stream<Path> stream = Files.list(source)) {
            Files.createDirectories(destination);
            stream.forEach(path -> {
                if (Files.isDirectory(path)) {
                    copyDirectory(path, destination.resolve(path.getFileName()));
                } else {
                    try {
                        Files.copy(path, destination.resolve(path.getFileName()));
                    } catch (IOException e) {
                        e.printStackTrace(); // NOP
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace(); // NOP
        }
    }

    public String getFileContents(Path file) throws IOException {
        return new String(Files.readAllBytes(file));
    }

    public void putTextFile(Path file, String contents) throws IOException {
        Files.writeString(file, contents);
    }

    public String getScriptPreamble() {
        return "#!/bin/sh\n";
    }

    public void run(Path scriptPath) {
        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(scriptPath.getParent().toFile());
        scriptPath.toFile().setExecutable(true);
        builder.command(scriptPath.toString());
        try {
            builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean fileExists(Path path) {
        return Files.exists(path);
    }
}
