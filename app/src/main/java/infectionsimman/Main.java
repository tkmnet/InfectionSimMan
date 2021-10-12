package infectionsimman;


import java.util.ArrayList;

public class Main {
    public static final String LOCAL_WORKING_DIRECTORY = "/tmp/infectionsimman";
    public static final String ABCI_WORKING_DIRECTORY = "~/infectionsimman";
    private static final int DAY_START = 0;
    private static final int DAY_MAX = 50;
    private static final int DAY_STEP = 5;
    private static final int POPULATION_SIZE = 1; //100;

    public static void main(String[] args) {
        Communicator localCommunicator = new Communicator();
        Communicator abciCommunicator = new AbciCommunicator();

        ArrayList<SimulatorRunner> simulatorRunnerList = new ArrayList<>();
        for (int day = DAY_START; day < DAY_MAX; day += DAY_STEP) {
            int end = day + DAY_STEP;

            AssimilatorRunner assimilatorRun = null;
            if (simulatorRunnerList.isEmpty()) {
                assimilatorRun = new AssimilatorRunner(AssimilatorRunner.Mode.INITIALIZE);
            } else {
                assimilatorRun = new AssimilatorRunner(AssimilatorRunner.Mode.NEXT);
                //for (SimulatorRunner simulatorRunner : simulatorRunnerList) { }
                // NOTE: currently, simulation results will be updated by the intermediate update action
            }
            assimilatorRun.setStartTime(DAY_START);
            assimilatorRun.setCurrentTime(day);
            assimilatorRun.run(localCommunicator).
                    waitFor(localCommunicator).
                    loadResults(localCommunicator);

            simulatorRunnerList.clear();
            for (int pop = 0; pop < POPULATION_SIZE; pop += 1) {
                SimulatorRunner simulatorRunner = new SimulatorRunner(pop);
                simulatorRunner.setStart(DAY_START);
                simulatorRunner.setEnd(end);
                simulatorRunner.setCoefA(assimilatorRun.getCoefA(pop));
                simulatorRunner.setCoefB(0, assimilatorRun.getCoefB(0, pop));
                simulatorRunner.setCoefB(1, assimilatorRun.getCoefB(1, pop));
                simulatorRunner.setCoefB(2, assimilatorRun.getCoefB(2, pop));
                simulatorRunner.setCoefB(3, assimilatorRun.getCoefB(3, pop));
                int finalDay = day;
                int finalPop = pop;
                simulatorRunner.setIntermediateResultUpdatedAction(r -> {
                    System.out.println(r.toString());
                    if (false) {
                        AssimilatorRunner runner = new AssimilatorRunner(AssimilatorRunner.Mode.INTERMEDIATE);
                        runner.setStartTime(DAY_START);
                        runner.setCurrentTime(finalDay);
                        runner.setIntermediateId(finalPop);
                        runner.setSimulationResult(r);
                    }
                });
                simulatorRunner.run(abciCommunicator);
                simulatorRunnerList.add(simulatorRunner);
            }

            simulatorRunnerList.forEach(s -> s.waitFor(abciCommunicator).loadResults(abciCommunicator));
        }
    }
}
