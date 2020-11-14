package jpl.cws.core.web;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * A {@link X509TrustManager} and {@link HostnameVerifier} which trusts everything.
 * 
 * @author    Torleif Berger
 * @license   http://creativecommons.org/licenses/by/3.0/
 * @see       http://www.geekality.net/?p=2408
 */
public final class TrustAllCertificates implements X509TrustManager, HostnameVerifier
{
    public X509Certificate[] getAcceptedIssuers() {return null;}
    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
    public boolean verify(String hostname, SSLSession session) {return true;}
    
    /**
     * Installs a new {@link TrustAllCertificates} as trust manager and hostname verifier. 
     */
    public static void installGlobal()
    {
        try
        {
            TrustAllCertificates trustAll = new TrustAllCertificates();
            
            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, 
                    new TrustManager[]{trustAll}, 
                    new java.security.SecureRandom());          
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(trustAll);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("Failed setting up all trusting certificate manager.", e);
        }
        catch (KeyManagementException e)
        {
            throw new RuntimeException("Failed setting up all trusting certificate manager.", e);
        }
    }
}