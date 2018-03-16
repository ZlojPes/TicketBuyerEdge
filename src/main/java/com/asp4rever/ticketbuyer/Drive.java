package com.asp4rever.ticketbuyer;

import java.nio.file.Paths;

import org.openqa.selenium.*;
import org.openqa.selenium.edge.EdgeDriver;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Drive implements Runnable {

    private final WebDriver driver;
    private final Gui frame;
    private final int[] cupePriority = {3, 4, 6, 1, 7, 2, 5, 0, 8, 9};
    private List<Passenger[]> orderList;
    private List<String> messagePool;

    //label
    Drive(Gui frame) {
        System.out.println("====================================================");
        String pathToEdgeDriver = Paths.get(".\\lib\\edge_driver\\MicrosoftWebDriver.exe").toAbsolutePath().normalize().toString();
        System.setProperty("webdriver.edge.driver", pathToEdgeDriver);
        DesiredCapabilities capabilities = DesiredCapabilities.edge();
        driver = new EdgeDriver(capabilities);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        this.frame = frame;
        configureOrder();
        messagePool = new LinkedList<>();
    }


    public void run() {
        frame.print("Открытие браузера... ");
        driver.get("http://booking.uz.gov.ua/ru/");
        frame.println("успешно.");
        if (frame.authorizeBox.isSelected()) {
            authorize();
        }
        boolean run = true;
        prepareStations();
        driver.findElement(By.name("date-hover")).click();
        chooseDate();
        while (run) {
            List<WebElement> trainList = getTrainList();
            for (final WebElement currentTrain : trainList) {
                WebElement wagonTypeButton;
                driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
                try {
                    wagonTypeButton = currentTrain.findElement(By.cssSelector("div[title=\"" + frame.getWagonType() + "\"] > button"));
                } catch (org.openqa.selenium.NoSuchElementException e) {
                    System.out.println("нет такого типа вагона");
                    continue;
                }
                driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                String currentWebNum = currentTrain.findElement(By.tagName("a")).getText();
                if (frame.isConcreteTrainNumber()) {
                    if (currentWebNum.contains(frame.getTrainNumberField())) {
                        wagonTypeButton.click();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        waitForLoading();
                        if (exploreTrainPopUp()) {
                            run = false;
                            break;
                        }
                        driver.findElement(By.cssSelector("a[title='закрыть']")).click();
                        break;
                    }
                } else {
                    new WebDriverWait(driver, 5).until(ExpectedConditions.elementToBeClickable(wagonTypeButton));
                    wagonTypeButton.click();
                    waitForLoading();
                    if (exploreTrainPopUp()) {
                        run = false;
                        break;
                    }
                    driver.findElement(By.cssSelector("a[title='закрыть']")).click();
                }
            }
        }
    }

    private void prepareStations() {
        WebElement fromTitle = driver.findElement(By.name("from-title"));
        WebElement toTitle = driver.findElement(By.name("to-title"));
        fromTitle.clear();
        fromTitle.sendKeys(frame.getFromStation());
        fromTitle.sendKeys(Keys.ARROW_RIGHT);
        driver.findElement(By.className("ui-menu-item"));
        fromTitle.sendKeys(Keys.ARROW_DOWN);
        fromTitle.sendKeys(Keys.ENTER);
        toTitle.clear();
        toTitle.sendKeys(frame.getToStation());
        toTitle.sendKeys(Keys.ARROW_RIGHT);
        driver.findElement(By.className("ui-menu-item"));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        toTitle.sendKeys(Keys.ARROW_DOWN);
        driver.findElement(By.className("ui-menu-item ui-state-focus"));
        toTitle.sendKeys(Keys.ENTER, Keys.TAB);
    }

    private boolean exploreTrainPopUp() {
        if (isElementPresent(By.xpath(".//*[text()='Сервис временно недоступен. Приносим извинения за доставленные неудобства.']/.."))) {
            System.out.println("Обнаружено окно \"Сервис недоступен\"");
            frame.println("Отработана ошибка \"Сервис недоступен\"");
            List<WebElement> closeRefs = driver.findElements(By.cssSelector("a[title='закрыть']"));
            closeRefs.get(closeRefs.size() - 1).click();
        }
        WebElement popUp = driver.findElement(By.cssSelector("div[class='vToolsPopup coachScheme']"));
        WebElement webWagonsHolder = popUp.findElement(By.cssSelector("span[class='coaches']"));
        String trainNum = (popUp.findElement(By.tagName("strong")).getText());
        List<WebElement> webWagonsList = webWagonsHolder.findElements(By.tagName("a"));
        for (WebElement currentWagon : webWagonsList) {
            Wagon wagon = WagonFactory.createWagon(driver, frame, currentWagon);
            int numOfFreePlaces = wagon.explore();
            String wagonNum = "" + wagon.getNumber();
            if (checkOrder(wagon.getPlacesByCupe())) {
                if (tryToBuy()) {
                    System.out.println("exploreTrainPopUp()returns true");
                    return true;
                }
            } else {
                String result = "Поезд №" + trainNum + " вагон №" + wagonNum + " свободных мест: " + numOfFreePlaces;
                if (!messagePool.contains(result)) {
                    messagePool.add(result);
                    frame.println(result);
                    frame.println("Свободные места не соответствуют заявке\n");
                }
            }
        }
        System.out.println("exploreTrainPopUp()returns false");
        return false;
    }

    private List<WebElement> getTrainList() {
        boolean clickUnsuccessful = true;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            try {
                driver.findElement(By.name("search")).click();
                clickUnsuccessful = false;
            } catch (org.openqa.selenium.WebDriverException ex) {
                System.out.println("Кнопка \"Поиск не найдена!\"");
            }
        } while (clickUnsuccessful || isTrainNotFound());
        return driver.findElements(By.cssSelector("tr[class^='vToolsDataTableRow']"));
    }

    private boolean isTrainNotFound() {
        waitForLoading();
        WebElement notFound = (new WebDriverWait(driver, 15)).until(new ExpectedCondition<WebElement>() {
            @Override
            public WebElement apply(WebDriver d) {
                return d.findElement(By.id("ts_res_not_found"));
            }
        });
        return notFound.getText().equals("По заданному Вами направлению поездов нет");
    }

    private void chooseDate() {
        System.out.println("chooseDate() started");
        int[] date = frame.getDate();
        List<WebElement> monthList = driver.findElements(By.className("ui-calendar-month"));
        for (WebElement month : monthList) {
            if (month.getText().equals("Апрель")) {
                month.click();
                return;
            }
        }
        List<WebElement> daysList = driver.findElements(By.cssSelector("td[data-month='" + (date[1] - 1) + "']"));
        for (WebElement day : daysList) {
            if (day.getText().equals("" + date[0])) {
                day.click();
                return;
            }
        }
        frame.println("!!!Указанная дата неверна, либо выходит за пределы\n доступных для заказа дат!!!");
        driver.close();
    }

    private boolean isElementPresent(By by) {
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        try {
            driver.findElement(by);
            return true;
        } catch (org.openqa.selenium.NoSuchElementException e) {
            return false;
        } finally {
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        }
    }

    private void authorize() {
        if (isElementPresent(By.linkText("Выйти")))
            return;
        driver.findElement(By.linkText("Кабинет пользователя")).click();
        driver.findElement(By.name("login")).clear();
        driver.findElement(By.name("login")).sendKeys(frame.getLogin());
        driver.findElement(By.name("passwd")).clear();
        driver.findElement(By.name("passwd")).sendKeys(frame.getPassword());
        driver.findElement(By.cssSelector("button.button")).click();
        driver.findElement(By.linkText("Заказ билетов")).click();
    }

    private void waitForLoading() {
        try {
            (new WebDriverWait(driver, 10)).until(new ExpectedCondition<WebElement>() {
                @Override
                public WebElement apply(WebDriver d) {
                    return d.findElement(By.cssSelector("div[style*='visibility: hidden; position: fixed; z-index: 10000']"));
                }
            });
        } catch (org.openqa.selenium.TimeoutException ex) {
            frame.println("Ошибка ожидания обновления страницы!");
            System.out.println("TimeoutException");
        }
    }

    private void configureOrder() {
        orderList = new ArrayList<>();
        if (frame.getPassangerNumber() >= frame.getCupeSize()) {
            orderList.add(new Passenger[frame.getCupeSize()]);
            if (frame.getPassangerNumber() - frame.getCupeSize() > 0)
                orderList.add(new Passenger[frame.getPassangerNumber() - frame.getCupeSize()]);
        } else orderList.add(new Passenger[frame.getPassangerNumber()]);
    }

    private boolean checkOrder(int[][] cupe) {
        int prevOrderSize = 0;
        for (Passenger[] currentOrder : orderList) {
            System.out.println("currentOrder.length " + currentOrder.length);
            boolean success = false;
            for (int i : cupePriority) {
                if (i == 9 && frame.getWagonType().equalsIgnoreCase("Плацкарт"))
                    break;
                int freePlaces = 0;
                for (int j = 0; j < frame.getCupeSize(); j++)
                    if (cupe[i][j] != 0)
                        freePlaces++;
                if (freePlaces >= currentOrder.length) {
                    System.out.print("cupe№ " + (i + 1));
                    int orderLowSeatNum = frame.getOrderLowSeatNum(prevOrderSize, prevOrderSize + currentOrder.length);
                    System.out.print(" LowSeatNum: " + orderLowSeatNum);
                    int cupeLowSeatNum = getCupeLowSeatNum(cupe[i]);
                    System.out.println("/" + cupeLowSeatNum);
                    if (cupeLowSeatNum >= orderLowSeatNum) {
                        for (int k = 0; k < (currentOrder.length); k++) {
                            if (frame.lowSeatArray[k + prevOrderSize].isSelected()) {
                                for (int m = 0; m < cupe[i].length; m++) {
                                    int place = cupe[i][m];
                                    if (place % 2 != 0) {
                                        System.out.println("нижнее");
                                        currentOrder[k] = createPassenger(k + prevOrderSize, place);
                                        cupe[i][m] = 0;
                                        break;
                                    }
                                }
                            }
                        }
                        for (int k = 0; k < (currentOrder.length); k++) {
                            if (!frame.lowSeatArray[k + prevOrderSize].isSelected()) {
                                for (int m = 0; m < cupe[i].length; m++) {
                                    int place = cupe[i][m];
                                    if (place != 0) {
                                        if (place % 2 == 0)
                                            System.out.println("верхнее");
                                        else
                                            System.out.println("нижнее");
                                        currentOrder[k] = createPassenger(k + prevOrderSize, place);
                                        cupe[i][m] = 0;
                                        break;
                                    }
                                }
                            }
                        }
                        success = true;
                        System.out.println("success=true;");
                        break;
                    }
                }
            }
            if (!success) {
                return false;
            }
            prevOrderSize = currentOrder.length;
        }
        return true;
    }

    private Passenger createPassenger(int passIndex, int place) {
        System.out.println("passIndex:" + passIndex + "; place:" + place);
        return new Passenger(frame.surNameArray[passIndex].getText(), frame.nameArray[passIndex].getText(), frame.childBoxArray[passIndex].isSelected(), place);
    }

    private int getCupeLowSeatNum(int[] cupe) {
        int counter = 0;
        for (int i = 0; i < frame.getCupeSize(); i++) {
            int place = cupe[i];
            if (place != 0 && place % 2 != 0)
                counter++;
        }
        return counter;
    }

    private boolean tryToBuy() {
        System.out.println("tryToBuy ");
        try {
            WebElement holder = driver.findElement(By.id("ts_chs_tbl"));
            List<Passenger> totalOrder = new ArrayList<>();
            for (Passenger[] curOr : orderList) {
                totalOrder.addAll(Arrays.asList(curOr));
            }
            for (Passenger psng : totalOrder) {
                driver.findElement(By.cssSelector("div[place='" + psng.getPlace() + "']")).click();
            }
            List<WebElement> tickets = holder.findElements(By.cssSelector("tr[class^='vToolsDataTableRow']"));
            for (int i = 0; i < tickets.size(); i++) {
                Passenger passenger = totalOrder.get(i);
                WebElement ticket = tickets.get(i);
                WebElement surName = ticket.findElement(By.cssSelector("input[title='Фамилия']"));
                surName.clear();
                surName.sendKeys(passenger.getSurName());
                WebElement name = ticket.findElement(By.cssSelector("input[title='Имя']"));
                name.clear();
                name.sendKeys(passenger.getName());
                if (passenger.isChild()) {
                    ticket.findElement(By.cssSelector("input[value='child']")).click();
                    ticket.findElement(By.cssSelector("input[class='child_birthdate hasDatepicker'")).click();
//                        try {Thread.sleep(1000);} catch (InterruptedException ex) {}
                    driver.findElement(By.linkText("2015")).click();
                    driver.findElement(By.linkText("Октябрь")).click();
                    driver.findElement(By.linkText("31")).click();
                }
            }
            driver.findElement(By.cssSelector("button[class='complex_btn'")).click();
            if (isElementPresent(By.className("popup-title")))
                frame.println("Captcha detected!!!");
            else
                frame.println("Билеты успешно добавлены в корзину.");
            return true;
        } catch (Exception ex) {
            frame.println("Процесс покупки окончился неудачей!");
            System.out.println("tryToBuy failed");
            return false;
        }
    }

    WebDriver getWebDriver() {
        return driver;
    }
}
