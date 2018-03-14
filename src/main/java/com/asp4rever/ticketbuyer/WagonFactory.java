package com.asp4rever.ticketbuyer;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class WagonFactory {

    public static Wagon createWagon(WebDriver driver, Gui frame, WebElement wagon) {
        switch (frame.getWagonType()) {
            case "Плацкарт":
                return new plazkartWagon(driver, wagon);
            case "Купе":
                return new kupeWagon(driver, wagon);
            default:
                return new luxWagon(driver, wagon);
        }
    }
}
