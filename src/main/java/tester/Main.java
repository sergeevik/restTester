package tester;

import tester.exception.ExecuteFailException;
import tester.model.Properties;
import tester.model.Request;
import tester.model.Scenarios;
import tester.service.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;


public class Main {
    private static ResultLogger requestLogger = new ResultLogger();
    private static ResultLogger scenarioLogger = new ResultLogger();
    public static void main(String[] args) throws IOException {
        try {
            String propertiesFile;
            if (args.length < 1 || args[0] == null || args[0].isEmpty()) {
                System.out.println("path to property file:");
                Scanner scanner = new Scanner(System.in);
                propertiesFile = scanner.nextLine();
            } else {
                propertiesFile = args[0];
            }

            Properties commonProperties = ParserConfig.getCommonProperties(propertiesFile);
            PropertiesService propertiesService = new PropertiesService(commonProperties);
            propertiesService.addLogAppender();
            propertiesService.setRestAssuredProperties();

            List<String> requestsFiles = getAllFile(commonProperties.getRequestsFolder());
            requestsFiles.addAll(commonProperties.getRequestList());

            List<String> scenarioFiles = getAllFile(commonProperties.getScenariosFolder());
            scenarioFiles.addAll(commonProperties.getScenarioList());

            for (String requestFile : requestsFiles) {
                req(commonProperties, requestFile);
            }
            for (String scenarioFile : scenarioFiles) {
                scenario(commonProperties, scenarioFile);
            }
            requestLogger.setOutFile(propertiesService.getFullPathToLogFile() + "resultRequest.log");
            scenarioLogger.setOutFile(propertiesService.getFullPathToLogFile() + "resultScenario.log");
            requestLogger.printResultToLog("Requests");
            scenarioLogger.printResultToLog("Scenarios");
        }catch (Exception ex){
            ex.printStackTrace();
        }finally {
            System.out.println("press any key and Enter");
            new Scanner(System.in).nextLine();
        }

    }

    private static List<String> getAllFile(String requestsFolder) throws IOException {
        if (requestsFolder == null || requestsFolder.isEmpty()){
            return new ArrayList<>();
        }
        try(Stream<Path> pathStream = Files.walk(Paths.get(requestsFolder))){
            return pathStream
                    .filter(Files::isRegularFile)
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .collect(toList());
        }
    }

    private static void scenario(Properties properties, String scenarioFile) throws IOException {
        Scenarios scenario = ParserConfig.getScenariosSortByID(scenarioFile);
        ScenarioService scenarioService = new ScenarioService(scenario, properties);
        try {
            scenarioService.execute();
            scenarioLogger.saveOk(scenarioFile);
        }catch (ExecuteFailException fail){
            scenarioLogger.saveFail(scenarioFile + ":" + fail.getRequestRelativeUrl());
        }
    }

    private static void req(Properties properties, String requestFile) throws IOException {
        Request request = ParserConfig.getRequest(requestFile);
        RequestService requestService = new RequestService(request, properties);
        try {
            requestService.execute();
            requestLogger.saveOk(request.getRelativeUrl());
        }catch (ExecuteFailException fail){
            requestLogger.saveFail(request.getRelativeUrl());
        }
    }
}
