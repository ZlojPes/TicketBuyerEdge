package com.asp4rever.ticketbuyer;

import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class luxWagon extends Wagon {

    luxWagon(WebDriver driver, WebElement wagon) {
        super(driver, wagon);
    }

    @Override
    public int[][] sortByCompartments(List<Integer> freePlaceList) {
        int[][] cupe = new int[10][2];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 2; j++) {
                int count = i * 2 + j + 1;
                if (freePlaceList.contains(count)) {
                    cupe[i][j] = count;
                }
            }
        }
        return cupe;
    }
}
