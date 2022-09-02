package com.sk.airuntime;

import com.sk.airuntime.utils.JsonParser;

import com.sk.airuntime.utils.SecurityUtil;
import com.sk.airuntime.vo.ApiTokenInfo;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Map;

public class InferenceService {

    private String baseEndpoint = "https://aiip.skcc.com";
    private long tokenValidMinutes = 10;

    private PublicKey apiPublicKey;
    private String apiKeyId;

    private ApiTokenInfo tokenInfo;

    public InferenceService(String apikeyId,
                            String encodePublicKey )
            throws GeneralSecurityException {
        this.apiKeyId = apikeyId;
        this.apiPublicKey = SecurityUtil.getPublicKeyFromBase64Encrypted(encodePublicKey);
    }

    public InferenceService(String baseEndpoint,
                            String apikeyId,
                            String encodePublicKey )
            throws GeneralSecurityException {
        this(apikeyId, encodePublicKey);
        this.baseEndpoint = baseEndpoint;
    }

    private  String getKeyAuthEndpoint(){
        return baseEndpoint + "/api/common/backend/admin/api/keyauth";
    }

    private String getPredictEndpoint(){
        return baseEndpoint + "/api/runtime/ifservice/predict";
    }


    private CloseableHttpClient buildClient() {
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        return clientBuilder.build();
    }

    private HttpUriRequest createRequestPost(String url, String jsonBody) {
        HttpPost request = new HttpPost(url);
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
        return request;
    }

    private HttpUriRequest createRequestPost(String url, String token, String jsonBody) {
        HttpPost request = new HttpPost(url);
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.setHeader("Api-Auth-Token", token);
        request.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

        return request;
    }

    private long getCurrentTime(){
        return System.currentTimeMillis()/1000;
    }

    private void accuireNewToken() throws IOException, GeneralSecurityException {

        long currentTime = getCurrentTime();
        String encryptMsg = SecurityUtil.encryptRSA(String.valueOf(currentTime), apiPublicKey);

        var requestData = Map.of(
                "keyId", apiKeyId,
                "encMessage", encryptMsg,
                "duration" , tokenValidMinutes);

        JsonParser jsonParser =  new JsonParser();
        CloseableHttpResponse response;

        response = buildClient().execute(createRequestPost(this.getKeyAuthEndpoint(), jsonParser.mapToJsonStr(requestData)));
        Map<String, Object> responseData = jsonParser.parse(EntityUtils.toString(response.getEntity()));

        if (response.getStatusLine().getStatusCode() != 200){
            this.tokenInfo = null;
            throw new RuntimeException(String.format("Fail to get authToken (resp status code:%d)"
                    , response.getStatusLine().getStatusCode()));
        }

        this.tokenInfo = new ApiTokenInfo((String) responseData.get("result"), currentTime, tokenValidMinutes);
    }

    public String predict(String modelId, String data) throws IOException, GeneralSecurityException {

        if(tokenInfo == null || !tokenInfo.valid()){  accuireNewToken(); }

        var endpoint = this.getPredictEndpoint() + '/' + modelId;
        HttpUriRequest request = createRequestPost(endpoint, tokenInfo.getToken(), data);

        var response = buildClient().execute(request);

        if (response.getStatusLine().getStatusCode() != 200){
            this.tokenInfo = null;
            throw new RuntimeException(String.format("Fail to predict (resp status code:%d)"
                    , response.getStatusLine().getStatusCode()));
        }
        return EntityUtils.toString(response.getEntity(),"UTF-8");
    }

    public static void main(String[] args) {

//        String apikeyId = "{input your apikeyId}";
//        String apikey = "{input your apikey}";
//        String endpoint = "{input your endpoint}";

        //example
        String baseEndpoint = "http://aiip-dev.skcc.com";
        String apikeyId = "ED23A3B575C2";
        String apikey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnYpPCa3MwqLj55DSrxCswp3rUgcMTuKFMCc76YtMjHB7h44r" +
                "330hadlHehCqIp9uUKGjg6f4u0pUTinK8CCB75/lrC94PPV0AgFHog3EX0BRfvI1GovIdhaJzJvqsAB9VKMRa9YJUbmNXfDddKfc" +
                "FLu87xgtVsF9linxeihchbUicFgOS3wOP26OyrHTXybYYLp5KjkegvFzF9LmI4ZBkyoNVJcr2Mm6lxqqEnOPdIawuPTToetzxEDJ" +
                "E9RrOmE5LPmJcgWIkMOC+v/ptZnnx/YkXj236e6NNGk+DeK1/i5gojpZ1x5IXiyaWhKb5uwgY1cXSBCdTENnl9ONqhh33QIDAQAB";

        String modelId =  "mdl-58aa2b86-d268-4c2b-889c-6eac2472abf5";

        //example data
        String data = "{" +
                "\"instances\": [" +
                "[4.7, 3.2, 1.3, 0.2]," +
                "[4.6, 3.1, 1.5, 0.2]" +
                "]," +
                "\"labels\"  : [ \"sepal_length\", \"sepal_width\", \"petal_length\", \"petal_width\" ]" +
                "}";

        try {

            InferenceService infService = new InferenceService(baseEndpoint, apikeyId, apikey);

            String output = infService.predict(modelId, data);

            System.out.println(output);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
