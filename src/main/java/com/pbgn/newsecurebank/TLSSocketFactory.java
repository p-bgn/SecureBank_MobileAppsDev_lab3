package com.pbgn.newsecurebank;



import android.os.Build;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;



public class TLSSocketFactory extends SSLSocketFactory {



    // Attribute
    private SSLSocketFactory internalSSLSocketFactory;



    // Constructor
    public TLSSocketFactory(String publicKey) throws KeyManagementException, NoSuchAlgorithmException {
        SSLContext context = SSLContext.getInstance("TLS");
        TrustManager tm[] = {new PubKeyManager(publicKey)};
        context.init(null, tm, null);
        internalSSLSocketFactory = context.getSocketFactory();
    }



    // Methods

    @Override
    public String[] getDefaultCipherSuites() {
        return internalSSLSocketFactory.getDefaultCipherSuites();
    }
    @Override
    public String[] getSupportedCipherSuites() {
        return internalSSLSocketFactory.getSupportedCipherSuites();
    }

    
    
    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose));
    }
    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }
    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port, localHost, localPort));
    }
    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }
    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(address, port, localAddress, localPort));
    }

    
    
    private Socket enableTLSOnSocket(Socket socket) {
        if (socket != null && (socket instanceof SSLSocket)) {
            SSLSocket sslSocket = ((SSLSocket) socket);
            String[] enabledProtocols = sslSocket.getEnabledProtocols();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                ((SSLSocket) socket).setEnabledProtocols((enabledProtocols));
            else
                ((SSLSocket) socket).setEnabledProtocols(checkAndAddTLSv1_1Andv1_2(enabledProtocols));
        }
        return socket;
    }

    
    
    private String[] checkAndAddTLSv1_1Andv1_2(String[] currentProtocols) {
        boolean hasTLSv1_1 = false;
        boolean hasTLSv1_2 = false;
        String tlsv11 = "TLSv1.1";
        String tlsv12 = "TLSv1.2";

        List<String> list = new ArrayList<>(Arrays.asList(currentProtocols));

        for (String protocol : currentProtocols) {
            if (protocol.equals(tlsv11)) {
                hasTLSv1_1 = true;
            }
            if (protocol.equals(tlsv12)) {
                hasTLSv1_2 = true;
            }
        }
        if (!hasTLSv1_1) {
            list.add(tlsv11);
        }
        if (!hasTLSv1_2) {
            list.add(tlsv12);
        }
        return list.toArray(new String[list.size()]);
    }









    public final class PubKeyManager implements X509TrustManager {

        private String publicKey;

        public PubKeyManager(String publicKey) {
            this.publicKey = publicKey;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            if (chain == null) {
                throw new IllegalArgumentException("checkServerTrusted: X509Certificate array is null");
            }
            if (!(chain.length > 0)) {
                throw new IllegalArgumentException("checkServerTrusted: X509Certificate is empty");
            }

            // Perform customary SSL/TLS checks
            TrustManagerFactory tmf;
            try {
                tmf = TrustManagerFactory.getInstance("X509");
                tmf.init((KeyStore) null);

                for (TrustManager trustManager : tmf.getTrustManagers()) {
                    ((X509TrustManager) trustManager).checkServerTrusted(
                            chain, authType);
                }

            } catch (Exception e) {
                throw new CertificateException(e.toString());
            }

            RSAPublicKey pubkey = (RSAPublicKey) chain[0].getPublicKey();
            String encoded = new BigInteger(1 /* positive */, pubkey.getEncoded())
                    .toString(16);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
