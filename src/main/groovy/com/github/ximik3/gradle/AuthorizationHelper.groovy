package com.github.ximik3.gradle

import com.google.gson.Gson
import org.apache.commons.codec.binary.Base64;
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature

/**
 * Google OAuth 2.0 service account authorization helper
 * @see <a href="https://developers.google.com/identity/protocols/OAuth2ServiceAccount">
 *     https://developers.google.com/identity/protocols/OAuth2ServiceAccount
 *     </a>
 * Created by volodymyr.kukhar on 10/24/16.
 */
class AuthorizationHelper {

    private static final String DEFAULT_KEYSTORE_PASSWORD = "notasecret"
    private static final Long ACCESS_TOKEN_EXPIRE_TIME_SEC = 5 * 60        // + 5 minutes

    protected static AccessToken authorize(DrivePluginExtension extension) throws IOException {
        if (extension.serviceAccountEmail && extension.pk12File) {
            return authorizeWithServiceAccount(extension.serviceAccountEmail, extension.pk12File)
        }
        // TODO: add ability to authorize with json file
//        else if (extension.jsonFile) {
//            return authorizeWithServiceAccount(extension.jsonFile)
//        }
        throw new IllegalArgumentException("No credentials provided.")
    }

    /**
     * Authorization with serviceAccountEmail using keystore file (*.pk12)
     * @param serviceAccountEmail
     * @param pk12File
     * @return
     * @throws IOException
     */
    private static AccessToken authorizeWithServiceAccount(String serviceAccountEmail, File pk12File)
            throws IOException {

        def keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(pk12File.newInputStream(), DEFAULT_KEYSTORE_PASSWORD.toCharArray())

        def privateKey = keyStore.getKey(keyStore.aliases().nextElement(), DEFAULT_KEYSTORE_PASSWORD.toCharArray()) as PrivateKey

        def JWTHeader = '{"alg":"RS256","typ":"JWT"}'
        def JWTClaimSet = '{\n' +
                '  "iss":' + serviceAccountEmail + ',\n' +
                '  "scope":"https://www.googleapis.com/auth/drive",\n' +
                '  "aud":"https://www.googleapis.com/oauth2/v4/token",\n' +
                '  "exp":' + (System.currentTimeSeconds() + ACCESS_TOKEN_EXPIRE_TIME_SEC) + ',\n' +
                '  "iat":' + System.currentTimeSeconds() + '\n' +
                '}'
        def JWT = Base64.encodeBase64URLSafeString(JWTHeader.bytes) +
                '.' + Base64.encodeBase64URLSafeString(JWTClaimSet.bytes)

        def signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(JWT.bytes)
        JWT += '.' + Base64.encodeBase64URLSafeString(signature.sign())

        def authUrl = new URL("https://www.googleapis.com/oauth2/v4/token")
        def auth = authUrl.openConnection() as HttpURLConnection
        auth.setRequestMethod("POST")
        auth.setRequestProperty("Host", "www.googleapis.com")
        auth.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        auth.setDoOutput(true)
        def body = 'grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer' +
                '&assertion=' + JWT
        auth.outputStream.write(body.bytes)
        def reader = new BufferedReader(new InputStreamReader(auth.inputStream))

        StringBuilder sb = new StringBuilder()
        for (int c; (c = reader.read()) >= 0;)
            sb.append(c as char)

//        println sb.toString()

        def accessToken = new Gson().fromJson(sb.toString(), AccessToken.class)

        return accessToken; 
    }
}
