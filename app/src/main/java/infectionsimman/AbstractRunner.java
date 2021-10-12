package infectionsimman;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class AbstractRunner {
    UUID id;

    public abstract Path getWorkingDirectory(); {
    }
    public abstract String getCategory();
    public abstract Path getSourcePath();
    public abstract void createFiles(Communicator communicator) throws IOException;
    public abstract String getScript();
    public abstract void postJob(Communicator communicator);
    public abstract void loadResults(Communicator communicator);

    public AbstractRunner() {
        id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }


    public Path getMainScriptPath() {
        return getWorkingDirectory().resolve("J" + getId().toString() + ".sh");
    }

    public AbstractRunner run(Communicator communicator) {
        try {
            deploy(communicator);
            createFiles(communicator);
            putMainScript(communicator);
            postJob(communicator);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public AbstractRunner waitFor(Communicator communicator) {
        while (!communicator.fileExists(getWorkingDirectory().resolve(getFinishedFileName()))) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                //NOP
            }
        }
        return this;
    }

    private void deploy(Communicator communicator) {
        communicator.copyToRemote(getSourcePath(), getWorkingDirectory());
    }

    protected String getFinishedFileName() {
        return getId().toString() + ".finished";
    }

    private void putMainScript(Communicator communicator) throws IOException {
        StringBuilder script = new StringBuilder(communicator.getScriptPreamble());
        script.append(getScript());
        script.append('\n');
        script.append("touch " + getFinishedFileName());
        communicator.putTextFile(getMainScriptPath(), script.toString());
    }
}
