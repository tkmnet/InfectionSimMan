package infectionsimman;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class AssimilatorRunner extends AbstractRunner {
    public static enum Mode { INITIALIZE, NEXT, INTERMEDIATE }

    private Mode mode;
    private JSONObject results;
    private int startTime;
    private int currentTime;
    private int intermediateId;
    private JSONArray simulationResult;

    public AssimilatorRunner(Mode mode) {
        super();
        this.mode = mode;
    }

    private String getModeFlag() {
        return mode.name().toLowerCase();
    }

    public void setIntermediateId(int intermediateId) {
        this.intermediateId = intermediateId;
    }

    public void setSimulationResult(JSONArray simulationResult) {
        this.simulationResult = simulationResult;
    }

    public void setCurrentTime(int currentTime) {
        this.currentTime = currentTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    private int getIntermediateId() {
        return intermediateId;
    }

    private JSONArray getSimulationResult() {
        return simulationResult;
    }

    private int getCurrentTime() {
        return currentTime;
    }

    private int getStartTime() {
        return startTime;
    }

    public double getCoefA(int id) {
        JSONArray jsonArray = results.getJSONArray("population");
        return jsonArray.getJSONObject(id).getJSONArray("alpha").getDouble(0);
    }

    public double getCoefB(int index, int id) {
        JSONArray jsonArray = results.getJSONArray("population");
        return jsonArray.getJSONObject(id).getJSONArray("beta").getDouble(index);
    }

    @Override
    public Path getWorkingDirectory() {
        return Paths.get(Main.LOCAL_WORKING_DIRECTORY).resolve(getCategory()).resolve(getId().toString());
    }

    @Override
    public String getCategory() {
        return "assimilator";
    }

    @Override
    public Path getSourcePath() {
        return Paths.get("src/main/resources/assimilator");
    }

    @Override
    public void createFiles(Communicator communicator) throws IOException {
        JSONObject input = new JSONObject();
        input.put("mode", getModeFlag());
        input.put("start_time", getStartTime());
        input.put("current_time", getCurrentTime());
        input.put("results", getSimulationResult());
        input.put("intermediate_id", getIntermediateId());
        communicator.putTextFile(getWorkingDirectory().resolve("_input.json"), input.toString());
    }

    @Override
    public String getScript() {
        return "sh ./start.sh " + Main.LOCAL_WORKING_DIRECTORY;
    }

    @Override
    public void postJob(Communicator communicator) {
        communicator.run(getMainScriptPath());
    }

    @Override
    public void loadResults(Communicator communicator) {
        try {
            results = new JSONObject(communicator.getFileContents(getWorkingDirectory().resolve("_output.json")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
