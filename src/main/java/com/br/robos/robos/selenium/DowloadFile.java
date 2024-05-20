package com.br.robos.robos.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DowloadFile {

    static class DownloadInfo {
        private LocalDateTime dateTime;
        private int numberOfFiles;

        public DownloadInfo(LocalDateTime dateTime, int numberOfFiles) {
            this.dateTime = dateTime;
            this.numberOfFiles = numberOfFiles;
        }

        public LocalDateTime getDateTime() {
            return dateTime;
        }

        public int getNumberOfFiles() {
            return numberOfFiles;
        }
    }

    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "chromedriver-mac-x64/chromedriver");
        WebDriver driver = new ChromeDriver();
        driver.manage().window().maximize();

        List<DownloadInfo> downloadHistory = new ArrayList<>();

        // Verificar se hoje é um dia útil (segunda a sexta)
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
            // Verificar se é 22:00
            LocalTime time = now.toLocalTime();
            if (time.getHour() == 20) {
                // Executar o processo de download
                try {
                    driver.get("https://dje.tjsp.jus.br/cdje/consultaAvancada.do#buscaavancada#buscaavancada#buscaavancada");

                    // Removendo o atributo "disabled" do input de cadernos usando JavaScript
                    WebElement elementoCadernos = driver.findElement(By.id("cadernosCad"));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].removeAttribute('disabled')", elementoCadernos);

                    // Clicando no elemento de cadernos
                    elementoCadernos.click();

                    Thread.sleep(2000); // Aguarda 2 segundos

                    List<WebElement> opcoesCadernos = elementoCadernos.findElements(By.tagName("option"));

                    int totalDownloads = 0;

                    for (WebElement opcao : opcoesCadernos) {
                        String nomeCaderno = opcao.getText();
                        opcao.click();

                        // Removendo o atributo "disabled" do botão de download usando JavaScript
                        WebElement botaoDownload = driver.findElement(By.id("download"));
                        ((JavascriptExecutor) driver).executeScript("arguments[0].removeAttribute('disabled')", botaoDownload);

                        // Clicando no botão de download
                        botaoDownload.click();

                        // Esperar até que o download seja concluído
                        waitForDownloadComplete();

                        // Verificar se os arquivos foram baixados corretamente
                        if (!verifyDownloads()) {
                            System.out.println("Ruptura nos arquivos baixados.");
                            // Aqui você pode adicionar o tratamento para a situação de ruptura
                            break;
                        }

                        totalDownloads += opcoesCadernos.size();

                        // Navega de volta para a página principal
                        driver.get("https://dje.tjsp.jus.br/cdje/consultaAvancada.do#buscaavancada#buscaavancada#buscaavancada");
                        elementoCadernos = driver.findElement(By.id("cadernosCad"));
                        opcoesCadernos = elementoCadernos.findElements(By.tagName("option"));
                    }

                    // Adicionar informações de download ao histórico
                    downloadHistory.add(new DownloadInfo(now, totalDownloads));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // Aguardar um tempo antes de encerrar o navegador
        try {
            Thread.sleep(10000); // Aguarda 10 segundos
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        driver.quit();

        // Exibir histórico de downloads
        for (DownloadInfo info : downloadHistory) {
            System.out.println("Data: " + info.getDateTime().toLocalDate());
            System.out.println("Horário: " + info.getDateTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            System.out.println("Quantidade de Downloads: " + info.getNumberOfFiles());
        }
    }

    private static void waitForDownloadComplete() throws InterruptedException {
        String downloadDir = System.getProperty("user.home") + "/Downloads";
        Path dir = Paths.get(downloadDir);
        while (true) {
            Thread.sleep(10000); // Aguarda 10 segundos
            try {
                // Verifica se algum arquivo PDF foi baixado recentemente
                boolean fileDownloaded = Files.list(dir)
                        .anyMatch(path -> path.toString().toLowerCase().endsWith(".pdf") && !path.toString().endsWith(".crdownload"));
                if (fileDownloaded) {
                    break; // Sai do loop se um arquivo PDF foi baixado
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean verifyDownloads() {
        String downloadDir = System.getProperty("user.home") + "/Downloads";
        Path dir = Paths.get(downloadDir);
        File[] files = dir.toFile().listFiles((dir1, name) -> name.toLowerCase().endsWith(".pdf"));
        if (files != null) {
            for (File file : files) {
                if (file.length() < 1000) { // Verifica se o tamanho do arquivo é menor que 1KB (arquivo vazio ou corrompido)
                    return false;
                }
            }
        }
        return true;
    }
}


