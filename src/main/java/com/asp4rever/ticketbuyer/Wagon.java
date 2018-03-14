package com.asp4rever.ticketbuyer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class Wagon {
    private WebDriver driver;
    private int number;
    private WebElement wagon;
    private int[][] placesByCupe;
    private WebElement popUp;

    Wagon(WebDriver driver, WebElement wagon) {
        this.driver = driver;
        popUp = driver.findElement(By.cssSelector("div[class='vToolsPopup coachScheme']"));
        this.wagon = wagon;
        number = setNumber();
    }

    private int setNumber() {
        String source = wagon.getAttribute("href");
        Pattern p = Pattern.compile("\\d\\d?");
        Matcher m = p.matcher(source);
        if (m.find()) {
            return Integer.parseInt(m.group());
        }
        return -1;
    }

    public int getNumber() {
        return number;
    }

    public int[][] getPlacesByCupe() {
        return placesByCupe;
    }

    private List<Integer> getFreePlaceList(WebElement popUp) {
        boolean repeat;
        List<Integer> integerFreePlacesList = null;
        do {
            try {
                List<WebElement> webElementFreePlacesList = popUp.findElements(By.cssSelector("div[class$='place fr']"));
                integerFreePlacesList = new ArrayList<>();
                for (WebElement place : webElementFreePlacesList) {
//                    System.out.println("в цикле нумерации мест");
                    integerFreePlacesList.add(Integer.parseInt(place.getAttribute("place")));
                }
                repeat = false;
            } catch (org.openqa.selenium.StaleElementReferenceException e) {
                repeat = true;
                System.out.println("брошено исключение, давай по-новой");
            }
        } while (repeat);
        return integerFreePlacesList;
    }

    public int explore() {
        boolean repeat;
        do {
            new WebDriverWait(driver, 5).until(ExpectedConditions.elementToBeClickable(wagon));
            System.out.print("кликаем по ваго");
            try {
                wagon.click();
                System.out.println("ну");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                repeat = false;
                new WebDriverWait(driver, 15).until(ExpectedConditions.attributeContains(wagon, "class", "active"));
            } catch (org.openqa.selenium.WebDriverException e) {
                System.out.println("catch1");
                repeat = true;
                if (isElementPresent(By.xpath(".//*[text()='Сервис временно недоступен. Приносим извинения за доставленные неудобства.']/.."))) {
                    System.out.println("Обнаружено окно \"Сервис недоступен\"");
                    List<WebElement> closeRefs = driver.findElements(By.cssSelector("a[title='закрыть']"));
                    closeRefs.get(closeRefs.size() - 1).click();
                } else {
                    System.out.println("\"Сервис недоступен\" не найден");
                }
//                try{
//                    driver.findElement(By.cssSelector("a[title='закрыть']")).click();}
//                catch(Exception ex){System.out.println("catch2");}
            }
        } while (repeat);

        List<Integer> freePlacesList = getFreePlaceList(popUp);
        placesByCupe = sortByCompartments(freePlacesList);
        return freePlacesList.size();
    }

    private boolean isElementPresent(By by) {
        try {
            driver.findElement(by);
            return true;
        } catch (org.openqa.selenium.NoSuchElementException e) {
            return false;
        }
    }

    public abstract int[][] sortByCompartments(List<Integer> freePlaceList);
}
