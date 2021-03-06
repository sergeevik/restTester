package tester.service;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import tester.exception.PropertiesConfigException;
import tester.model.Auth;
import tester.model.Properties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PropertiesService {
    private static Logger log = Logger.getLogger(PropertiesService.class);
    private Properties properties;
    private String fullPathToLogFile;

    public PropertiesService(Properties properties) {
        this.properties = properties;
        setFullPathToLog();
    }

    public void setRestAssuredProperties(){
        RequestSpecification specification = new RequestSpecBuilder().build();
        if (properties.getBaseUrl().isEmpty()){
            throw new PropertiesConfigException("baseUrl is required. You value = " + properties.getBaseUrl());
        }
        specification.baseUri(properties.getBaseUrl());
        if (properties.getPort() > 0) {
            specification.port(properties.getPort());
        }
        Auth auth = properties.getAuth();
        if (auth != null) {
            RestAssured.authentication = RestAssured.preemptive().basic(auth.getLogin(), auth.getPassword());
        }
        RestAssured.requestSpecification = specification;
    }

    public void addLogAppender(){
        try {
            appendLogWithLevel(fullPathToLogFile, Level.INFO);
            appendLogWithLevel(fullPathToLogFile, Level.DEBUG);
        } catch (IOException e) {
            if (!properties.getFullPathToLogFile().isEmpty()) {
                log.error("ошибка создания файлов для логов по пути: " + fullPathToLogFile +
                        ". Буду писать в user.home: " + System.getProperty("user.home"), e);
                properties.setFullPathToLogFile("");
                setFullPathToLog();
                try {
                    appendLogWithLevel(fullPathToLogFile, Level.INFO);
                    appendLogWithLevel(fullPathToLogFile, Level.DEBUG);
                } catch (IOException e1) {
                    log.error("я сделал все, что мог, логи не получится писать даже в user.home", e1);
                }
            }
        }
    }

    void appendLogWithLevel(String path, Level level) throws IOException {
        FileAppender fileAppender = new FileAppender();
        fileAppender.setName(level + ".log");
        String fileName = path + level.toString() + ".log";
        Path pathToLog = Paths.get(fileName);
        if (Files.notExists(pathToLog)) {
            Files.createDirectories(pathToLog.getParent());
            Files.createFile(pathToLog);
        }
        fileAppender.setFile(fileName);
        fileAppender.setThreshold(level);
        fileAppender.setAppend(true);
        fileAppender.setLayout(new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n"));
        fileAppender.activateOptions();
        Logger.getRootLogger().addAppender(fileAppender);
    }

    private void setFullPathToLog(){
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy/HH-mm-ss/");
        fullPathToLogFile = properties.getFullPathToLogFile();
        if (fullPathToLogFile.isEmpty()){
            fullPathToLogFile = System.getProperty("user.home") + "/rest-tester";
        }
        fullPathToLogFile = fullPathToLogFile + "/logs/" + format.format(new Date());
    }

    public String getFullPathToLogFile() {
        return fullPathToLogFile;
    }
}
