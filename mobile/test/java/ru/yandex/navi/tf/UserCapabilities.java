package ru.yandex.navi.tf;

import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.html5.Location;

public class UserCapabilities {
    public Platform platform;
    public String platformVersion;
    public boolean grantPermissions = true;
    public Location initLocation;
    public ScreenOrientation screenOrientation;
}
