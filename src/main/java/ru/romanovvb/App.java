package ru.romanovvb;

import io.javalin.Javalin;
import io.javalin.plugin.rendering.template.JavalinThymeleaf;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class App {

    private static final int MAX_NET_MASK = 32;
    private static final int PORT = 7070;
    private static final String PATH = "/public/";
    private static File allResults = new File("./src/main/resources/public/download/allResults.txt");
    private static File onlyWithDomains = new File("./src/main/resources/public/download/OnlyWithDomains.txt");
    private static IpAndDomainMap allResMap = new IpAndDomainMap();
    private static IpAndDomainMap onlyWithDomainsMap = new IpAndDomainMap();
    private static ThreadPoolExecutor threadPoolExecutor = null;
    private static String ipMask = null;
    private static int threads = 0;

    public static void main(String[] args) {
        Javalin javalin = Javalin.create().start(PORT);

        JavalinThymeleaf.configure(getTemplateEngine());

        javalin.get("/", ctx -> ctx.render("index.html"));

        javalin.post("/send-ip", ctx -> {
            ipMask = ctx.formParam("mask");
            threads = Integer.parseInt(Objects.requireNonNull(ctx.formParam("thread_count")));
            threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
            System.out.println("Запрос: " + ipMask + " ; " + threads + " принят");
            checkIpAddresses(ipMask);

            Thread.sleep(1000);
            ctx.redirect("/result");
        });
//        javalin.after("/send-ip", context -> context.redirect("/result"));

        javalin.get("/result", ctx -> {
            String status = "Checking in progress...";
            boolean isFileReady = false;

            if (threadPoolExecutor.getActiveCount()==0) {
                status = "Checking ending";
                allResMap.saveTxt(allResults);
                onlyWithDomainsMap.saveTxt(onlyWithDomains);
                isFileReady = true;
            }
            String answer = "Request: " + ipMask + "  Threads: " + threads +
                    "  Status: " + status;

            Map<String, Object> model = new HashMap<>();
            model.put("answer", answer);
            model.put("isFileReady", isFileReady);
            ctx.render("/result.html", model);
        });

        javalin.get("/download/all-results", context -> {
            InputStream inputStream = new BufferedInputStream(Files.newInputStream(allResults.toPath()));
            context.header("Content-Disposition", "attachment; filename=\"" + allResults.getName() + "\"");
            context.header("Content-Length", String.valueOf(allResults.length()));
            context.result(inputStream);
        });
        javalin.get("/download/only-with-domains", context -> {
            InputStream inputStream = new BufferedInputStream(Files.newInputStream(onlyWithDomains.toPath()));
            context.header("Content-Disposition", "attachment; filename=\"" + onlyWithDomains.getName() + "\"");
            context.header("Content-Length", String.valueOf(onlyWithDomains.length()));
            context.result(inputStream);
        });

    }
    private static TemplateEngine getTemplateEngine() {
        TemplateEngine templateEngine = new TemplateEngine();

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix(App.PATH);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateEngine.addTemplateResolver(templateResolver);

        return templateEngine;
    }
    private static void checkConnectionIp(String ip) {

        threadPoolExecutor.submit(() -> {
            String domain = "-";
            try {

                domain = CheckConnection.findDomain(ip);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            finally {
                allResMap.getIpAndDomain().put(ip, domain);
            }
            if (!domain.equals("-"))
                onlyWithDomainsMap.getIpAndDomain().put(ip, domain);


        });
    }
    private static void checkIpAddresses(String ipMask) {
        String[] splitipMask = ipMask.split("/");
        String requestIp = splitipMask[0];
        int prefixMask = Integer.parseInt(Objects.requireNonNull(splitipMask[1]));

        if (prefixMask >=0 && prefixMask <= 32) {

            int difMasks = MAX_NET_MASK - prefixMask;
            String[] splitIp = requestIp.split("\\.");
            int secOne = Integer.parseInt(splitIp[0]);
            int secTwo = Integer.parseInt(splitIp[1]);
            int secThree = Integer.parseInt(splitIp[2]);
            int secFour = Integer.parseInt(splitIp[3]);

            int countSecOne = (int) Math.pow(2, 8);
            int countSecTwo = (int) Math.pow(2, 8);
            int countSecThree = (int) Math.pow(2, 8);
            int countSecFour = (int) Math.pow(2, 8);

            String resultIp = "";
            if (difMasks < 8) {
                countSecFour = (int) Math.pow(2, difMasks);
            }
            for (int i = 0; i < countSecFour; i++) {
                if (difMasks > 0)
                    secFour = i;

                if (difMasks > 8) {
                    if (difMasks-8 < 8)
                        countSecThree = (int) Math.pow(2, difMasks-8);

                    for (int j = 0; j < countSecThree; j++) {
                        secThree = j;
                        if (difMasks > 16) {
                            if (difMasks-16 < 8)
                                countSecTwo = (int) Math.pow(2, difMasks-16);

                            for (int k = 0; k < countSecTwo; k++) {
                                secTwo = k;
                                if (difMasks > 24) {
                                    if (difMasks - 24 < 8)
                                        countSecOne = (int) Math.pow(2, difMasks - 24);
                                    for(int l = 0; l < countSecOne; l++) {
                                        secOne = l;
                                        resultIp = secOne + "." + secTwo + "." + secThree + "." + secFour;
                                        checkConnectionIp(resultIp);
                                    }
                                } else {
                                    resultIp = secOne + "." + secTwo + "." + secThree + "." + secFour;
                                    checkConnectionIp(resultIp);
                                }
                            }
                        } else {
                            resultIp = secOne + "." + secTwo + "." + secThree + "." + secFour;
                            checkConnectionIp(resultIp);
                        }
                    }
                } else {
                    resultIp = secOne + "." + secTwo + "." + secThree + "." + secFour;
                    checkConnectionIp(resultIp);
                }
            }
        } else {
            System.out.println("Неверное значение префикса маски подсети");
        }
    }
}