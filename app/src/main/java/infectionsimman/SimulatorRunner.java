package infectionsimman;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SimulatorRunner extends AbstractRunner {
    private int popId;
    private double[] coef;
    private int start;
    private int end;
    private JSONArray preflog;
    private Consumer<JSONArray> intermediateResultUpdatedAction;
    private String lastIntermediate;

    public SimulatorRunner(int id) {
        this.popId = id;
        coef = new double[5];
    }

    public int getPopId() {
        return popId;
    }

    public void setCoefA(double value) {
        coef[0] = value;
    }

    public void setCoefB(int index, double value) {
        coef[1 + index] = value;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public double getCoefA() {
        return coef[0];
    }

    public double getCoefB(int index) {
        return coef[1 + index];
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public void setIntermediateResultUpdatedAction(Consumer<JSONArray> action) {
        intermediateResultUpdatedAction = action;
    }

    @Override
    public Path getWorkingDirectory() {
        return Paths.get(Main.ABCI_WORKING_DIRECTORY).resolve(getCategory()).resolve(getId().toString());
    }

    @Override
    public String getCategory() {
        return "simulator";
    }

    @Override
    public Path getSourcePath() {
        return Paths.get("src/main/resources/simulator");
    }

    @Override
    public void createFiles(Communicator communicator) throws IOException {
        JSONObject input = new JSONObject();
        input.put("id", getPopId());
        input.put("start", getStart());
        input.put("end", getEnd());
        input.put("coef_a", getCoefA());
        input.put("coef_0", getCoefB(0));
        input.put("coef_1", getCoefB(1));
        input.put("coef_2", getCoefB(2));
        input.put("coef_3", getCoefB(3));
        communicator.putTextFile(getWorkingDirectory().resolve("_input.json"), input.toString());
    }

    @Override
    public String getScript() {
        //return "sh ./start.sh " + Main.WORKING_DIRECTORY + " | grep '^infection' | tee " + getIntermediateFileName();
        return "sh ./start.sh " + Main.ABCI_WORKING_DIRECTORY + " | tee " + getIntermediateFileName();
    }

    @Override
    public void postJob(Communicator communicator) {
        communicator.run(getMainScriptPath());
        if (intermediateResultUpdatedAction != null) {
            Thread watcher = new Thread(() -> {
                while (!communicator.fileExists(getWorkingDirectory().resolve(getFinishedFileName()))) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        //NOP
                    }
                    Path intermediateFilePath = getWorkingDirectory().resolve(getIntermediateFileName());
                    try {
                        if (communicator.fileExists(intermediateFilePath)) {
                            String output = communicator.getFileContents(intermediateFilePath).replace("\n", "").replaceFirst("^.*\\[", "[");
                            if (output.startsWith("[") && output.endsWith("]") && !output.equals(lastIntermediate)) {
                                intermediateResultUpdatedAction.accept(new JSONArray(output));
                                lastIntermediate = output;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return;
            });
            watcher.start();
        }
    }

    private String getIntermediateFileName() {
        return getId().toString() + ".intermediate";
    }

    @Override
    public void loadResults(Communicator communicator) {
        try {
            preflog = new JSONArray(communicator.getFileContents(getWorkingDirectory().resolve("preflog.txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
