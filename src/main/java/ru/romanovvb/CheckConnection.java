package ru.romanovvb;

import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.security.Principal;
import java.security.cert.CertificateException;

public class CheckConnection {
    private static final int SSL_PORT = 443;

    public static String findDomain(String ipAddress) throws IOException {
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
                domain = "Ошибка SSL соединения";
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                assert sslSocket != null;
                sslSocket.close();
            }
        }
        return domain;
    }
}
