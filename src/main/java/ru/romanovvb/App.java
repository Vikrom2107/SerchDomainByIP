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
            System.out.println("Запрос: " + ipMask + " ; " + threads + " принят");
            searchDomains();

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
    private static void searchDomains() {
        String[] splitMask = ipMask.split("/");
        String ipAdress = splitMask[0];
        int mask = Integer.parseInt(Objects.requireNonNull(splitMask[1]));
        threadPoolExecutor =(ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
        String ip3 = ipAdress.substring(0, ipAdress.length()-1);

        for (int i = 0; i < 256; i++) {
            int finalI = i;
            threadPoolExecutor.submit(() -> {
                String anotherIpAddress = ip3 + finalI;
                String domain;
                try {
                    domain = CheckConnection.findDomain(anotherIpAddress);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (!domain.equals("-"))
                    onlyWithDomainsMap.getIpAndDomain().put(anotherIpAddress, domain);
                allResMap.getIpAndDomain().put(anotherIpAddress, domain);
            });
        }
    }
}