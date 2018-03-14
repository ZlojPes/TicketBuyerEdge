package com.asp4rever.ticketbuyer;

import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class kupeWagon extends Wagon {

    kupeWagon(WebDriver driver, WebElement wagon) {
        super(driver, wagon);
    }

    @Override
    public int[][] sortByCompartments(List<Integer> freePlaceList) {
        int[][] cupe = new int[10][4];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 4; j++) {
                int count = i * 4 + j + 1;
                if (freePlaceList.contains(count))
                    cupe[i][j] = count;
            }
        }
        return cupe;
    }
}
