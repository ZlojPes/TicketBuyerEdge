package com.asp4rever.ticketbuyer;

import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class plazkartWagon extends Wagon {
    plazkartWagon(WebDriver driver, WebElement wagon) {
        super(driver, wagon);
    }

    @Override
    public int[][] sortByCompartments(List<Integer> freePlaceList) {
        int[][] cupe = new int[9][6];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 4; j++) {
                int count = i * 4 + j + 1;
                if (freePlaceList.contains(count)) {
                    cupe[i][j] = count;
                }
            }
        }
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 2; j++) {
                int count = 54 - (i * 2 + j);
                if (freePlaceList.contains(count)) {
                    cupe[i][j + 4] = count;
                }
            }
        }
        return cupe;
    }
}
