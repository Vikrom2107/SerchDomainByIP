package ru.romanovvb;

import io.javalin.Javalin;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import io.javalin.http.Handler;
import j2html.tags.specialized.*;
import static j2html.TagCreator.*;

public class App {


    private static final int PORT = 7070;
    private static final String PATH = "/public/";
    private static final File allResults = new File("./src/main/resources/results/allResults.txt");
    private static final File onlyWithDomains = new File("./src/main/resources/results/OnlyWithDomains.txt");
    private static FindDomains findDomains;
    private static Javalin javalin;
    private static String ipMask;
    private static int threads = 0;
    private static boolean isCheckingRun = false;
    private static boolean isCheckingEnding= false;

    public static void main(String[] args) {
        javalin = Javalin.create(config -> config.addStaticFiles("/public").enableWebjars())
                .start(PORT);

        // Обработчики Javalin-ом HTTP-методов
        javalin.get("/", getMain);

        javalin.post("/send-ip", postSendRequest);

        javalin.after( "send-ip", afterSendRequest);

        javalin.get("/all-results", getAllResults);

        javalin.get("/only-with-domains", getOnlyDomainsResults);

        javalin.post("/new-request", postNewRequest);
    }

    private static String displayForChecking() {
        return isCheckingRun ? "display:block;" : "display:none;";
    }

    private static String displayForDownload() {
        return isCheckingEnding ? "display:block;" : "display:none;";
    }

    private static PTag createResult() {
        PTag result = p();
        if (threads > 0) {
            String status = "Идёт сканирование";

            if (findDomains.getThreadPoolExecutor().getActiveCount()==0) {
                status = "Сканирование завершено";
                findDomains.getAllResMap().saveTxt(allResults);
                findDomains.getOnlyWithDomainsMap().saveTxt(onlyWithDomains);
                isCheckingRun = false;
                isCheckingEnding = true;
            }
            String answer = "Запрос: " + ipMask + "  Число потоков: " + threads +
                    " \u0020\u0020 Статус: " + status;
            result = p(answer);
        }
        return result;
    }
    // Все пути Javalin
    private static final Handler getMain = ctx -> {
        HtmlTag content = html(
            head(
                script().withSrc("/webjars/htmx.org/1.9.2/dist/htmx.min.js"),
                meta().attr("http-equiv","Content-Type")
                        .attr("content", "text/html; charset=UTF-8"),
                link().withRel("stylesheet").withType("text/css").withHref("main.css"),
                title("Домен по IP")

            ),
            script("function reload_interval(check){\n" +
                    "if (check == true) {" +
                    "setTimeout(function(){location.reload();}, 3000);" +
                    "}}"),
            body(
                section(
                    h2("Программа для сканирования IP-диапазона для нахождения доменных имён"),
                    p("Введите IP-диапазон и количество потоков").withStyle("font-size:20px"),
                    form(
                        p("IP адрес с длиной маски подсети"),
                        input().withType("text").withName("mask").withClass("form-control")
                                .withPlaceholder("Например 58.58.58.0/24"),
                        p("Количество потоков"),
                        input().withType("number").withName("thread_count").withClass("form-control")
                                .withPlaceholder("Введите целое число"),
                        p(),
                        button("Отправить").withType("text").withClass("form-button")

                    ).withMethod("post").withAction("/send-ip"),

                    br(),
                    p(createResult()).withId("result"),
                    p(
                        img().withClass("animated-gif").withSrc("/loading.gif"),
                        br(),
                        a("Проверить, завершилось ли сканирование").withHref("/")
                    ).withId("checking").withStyle(displayForChecking()),

                    div(
                        p("Проверка завершена! Готовые файлы вы можете скачать по ссылкам ниже"),
                        p(
                            a("Скачать файл со всеми IP").withHref("/all-results").isDownload()
                        ),
                        p(
                            a("Скачать файл с найденными доменами").withHref("/only-with-domains").isDownload()
                        )
                    ).withId("download").withStyle(displayForDownload())
                )
            ).attr("onload", "reload_interval(" + isCheckingRun + ")")

        );
        String rendered = "<!DOCTYPE html>\n" + content.render();
        ctx.html(rendered);
    };
    private static final Handler postSendRequest = ctx -> {
        ipMask = ctx.formParam("mask");
        threads = Integer.parseInt(Objects.requireNonNull(ctx.formParam("thread_count")));
        findDomains = new FindDomains(threads);

        System.out.println("Запрос: " + ipMask + " ; " + threads + " принят");
        findDomains.checkIpAddresses(ipMask);
        isCheckingRun = true;
        isCheckingEnding =false;
        Thread.sleep(1000);
        ctx.redirect("/");
    };
    private static final Handler afterSendRequest = ctx -> {
        Thread thread = new Thread(() -> {
            while (true) {
                if (findDomains.getThreadPoolExecutor() != null) {
                    if (findDomains.getThreadPoolExecutor().getActiveCount() == 0) {
                        System.out.println("Работа окончена");
                        ctx.redirect("/");
                        break;
                    }
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
    };
    private static final Handler getAllResults = ctx -> {
        InputStream inputStream = new BufferedInputStream(Files.newInputStream(allResults.toPath()));
        ctx.header("Content-Disposition", "attachment; filename=\"" + allResults.getName() + "\"");
        ctx.header("Content-Length", String.valueOf(allResults.length()));
        ctx.result(inputStream);
    };
    private static final Handler getOnlyDomainsResults = ctx -> {
        InputStream inputStream = new BufferedInputStream(Files.newInputStream(onlyWithDomains.toPath()));
        ctx.header("Content-Disposition", "attachment; filename=\"" + onlyWithDomains.getName() + "\"");
        ctx.header("Content-Length", String.valueOf(onlyWithDomains.length()));
        ctx.result(inputStream);
    };
    private static final Handler postNewRequest = ctx -> {
        threads = 0;
        isCheckingRun = false;
        isCheckingEnding = false;
        ctx.redirect("/");
    };
}