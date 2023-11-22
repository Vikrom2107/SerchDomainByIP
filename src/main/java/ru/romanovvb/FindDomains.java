package ru.romanovvb;

import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class FindDomains {

    private static final int MAX_NET_MASK = 32;
    private static final int SSL_PORT = 443;
    private final IpAndDomainMap allResMap;
    private final IpAndDomainMap onlyWithDomainsMap;
    private final ThreadPoolExecutor threadPoolExecutor;
    private double percent = 0;

    public FindDomains(int threads) {
        threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
        allResMap = new IpAndDomainMap();
        onlyWithDomainsMap = new IpAndDomainMap();
    }

    // Данный метод прогоняется по всем Ip-адресам, соответсвующим заданным условиям
    public void checkIpAddresses(String ipMask) {
        String[] splitIpMask = ipMask.split("/");
        String requestIp = splitIpMask[0];
        int prefixMask = Integer.parseInt(Objects.requireNonNull(splitIpMask[1]));

        if (prefixMask < 0 || prefixMask > 32) {
            System.out.println("Невереная длина маски подсети ");
            return;
        }

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
                                    checkConnection(resultIp);
                                }
                            } else {
                                resultIp = secOne + "." + secTwo + "." + secThree + "." + secFour;
                                checkConnection(resultIp);
                            }
                        }
                    } else {
                        resultIp = secOne + "." + secTwo + "." + secThree + "." + secFour;
                        checkConnection(resultIp);
                    }
                }
            } else {
                resultIp = secOne + "." + secTwo + "." + secThree + "." + secFour;
                checkConnection(resultIp);
            }
            percent += 1.0/countSecFour;
        }

    }



    // Отправляет задачу в общий пул потоков
    private void checkConnection(String ip) {
        System.out.println("Началась проверка ip: " + ip);
        threadPoolExecutor.submit(() -> {
            String domain = "-";
            try {

                domain = findDomain(ip);
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

    // Проверяет соединение и ищет домены
    private String findDomain(String ipAddress) throws IOException {
        String domain = "-";
        InetAddress inetAddress = InetAddress.getByName(ipAddress);
        boolean status = inetAddress.isReachable(5000);
        if (status) {
            SSLSocket sslSocket = null;
            try {

                TrustManager[] trustManagers = {new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }
                }};

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagers, null);

                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                sslSocket = (SSLSocket) sslSocketFactory.createSocket(ipAddress, SSL_PORT);

                SSLSession sslSession = sslSocket.getSession();

                if (sslSession == null)
                    return "No SSL connection";

                X509Certificate[] certificates = sslSession.getPeerCertificateChain();
                Principal principal = certificates[0].getSubjectDN();
                String[] dnSplit = principal.getName().substring(3).split(",");
                domain = dnSplit[0];

            } catch (ConnectException ex) {
                System.out.println(ipAddress + " Ошибка SSL соединения");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                assert sslSocket != null;
                sslSocket.close();
            }
        }
        return domain;
    }

    //Геттеры
    public IpAndDomainMap getAllResMap() {
        return allResMap;
    }

    public IpAndDomainMap getOnlyWithDomainsMap() {
        return onlyWithDomainsMap;
    }

    public ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPoolExecutor;
    }

    public double getPercent() {
        return percent;
    }
}
