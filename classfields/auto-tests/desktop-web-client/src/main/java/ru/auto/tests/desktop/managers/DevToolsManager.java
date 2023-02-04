package ru.auto.tests.desktop.managers;

import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v101.network.model.Request;

import java.util.List;

public interface DevToolsManager {

    void startDevTools();

    void startRecordRequests();

    void stopRecordRequests();

    DevTools getDevTools();

    List<Request> getAllRequests();

}
