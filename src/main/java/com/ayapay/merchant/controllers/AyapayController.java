package com.ayapay.merchant.controllers;

import java.io.IOException;
import java.util.Base64;

import com.ayapay.merchant.models.APAccessToken;
import com.ayapay.merchant.models.APMerchantToken;
import com.ayapay.merchant.models.APResponseQr;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@RestController
public class AyapayController {
  String baseUrl = "https://opensandbox.ayainnovation.com";
  String wsoBasePath ="/posmerchant/1.0.0";
  String accessTokenEndPoint = "/token";
  String userTokenEndPoint = "/thirdparty/merchant/login";
  String checkQrEndPoint = "/apiMerchant/qrcode/checkQrByMerchant";
  String createTxnEndPoint = "/thirdparty/merchant/createTransaction";
  String execTxnEndPoint = "/thirdparty/merchant/executeTransaction";

  String APP_KEY = "API_KEY"; //provided after set up
  String APP_SECRET = "API_SECRET"; //provided after set up
  String MERCHANT_MOBILE = "09xxxx"; //provided after set up
  String SUBMERCHANT_MOBILE = "09xxxx"; //provided after set up
  String MERCHANT_PIN = "xxxxxx"; //provided after set up
  String SHOP_ID = "SHOP_ID"; //provided after set up
  String DEVICE_ID = "POS_COUNTER_1"; //requesting device id
  String SERVICE_ID = "5ed25392e3bb1a044aad805b";
  String SERVICE_CODE = "QRCODEPAYMENT";
  String RECEIVER_NAME = "MERCHANT_NAME"; //provided after set up
  String RECEIVER_PHONE = "09xxxx"; //provided after set ups
  String SENDER_NAME = ""; //from check qr api
  String SENDER_PHONE = ""; //from check qr api
  String TXN_ID = ""; //from createTxn api
  String TXN_CODE = ""; //from execTxn api
  String TXN_STATUS = "init";

  public static final OkHttpClient client = new OkHttpClient();

  private final Moshi moshi = new Moshi.Builder().build();
  private final JsonAdapter<APAccessToken> accessTokenJsonAdapter = moshi.adapter(APAccessToken.class);
  private final JsonAdapter<APMerchantToken> userTokenJsonAdapter = moshi.adapter(APMerchantToken.class);
  private final JsonAdapter<APResponseQr> respQrJsonAdapter = moshi.adapter(APResponseQr.class);

  APAccessToken accessToken = new APAccessToken();
  APMerchantToken userToken = new APMerchantToken();
  APResponseQr respQr = new APResponseQr();

  //STEP 5
  void execTxn() throws IOException {
    MediaType mediaType = MediaType.parse("application/json");
    String reqBodyExecTxn = "{\n  \"MessageType\": \"FO\",\n  \"TRANSREFID\": \"" + TXN_ID + 
    "\",\n  \"DEVICEID\": \"" +  DEVICE_ID + 
    "\",\n  \"PIN\": \"\",\n  \"OTP\": \"\"\n}";    

    RequestBody body = RequestBody.create(reqBodyExecTxn, mediaType);

    Request request = new Request.Builder()
      .url(baseUrl + wsoBasePath + execTxnEndPoint)
      .method("POST", body)
      .addHeader("Authorization", "Bearer " + userToken.token.token)
      .addHeader("Token", "Bearer " + accessToken.access_token)
      .addHeader("Content-Type", "application/json")
      .build();
    
    try (Response response = client.newCall(request).execute()) {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode rootNode = mapper.readTree(response.body().byteStream());

      if (rootNode.get("err").asInt() == 200) {
        
        TXN_STATUS = "TXN_COMPLETED";

        TXN_CODE = "";
        TXN_CODE = rootNode.get("data").get("transaction").get("code").asText();

        // COMPLETED        
      }
      else{
        TXN_STATUS = "EXEC_FAILED";
      }
    }
  }
  
  //STEP 4
  void createTxn(String qr, Long amount) throws IOException {
    MediaType mediaType = MediaType.parse("application/json");
    String reqBodyCreateTxn = "{\n  \"QR\": \"" + qr + 
    "\",\n  \"SENDERNAME\": \"" + SENDER_NAME + 
    "\",\n  \"SENDERPHONE\": \"" + SENDER_PHONE + 
    "\",\n  \"SENDERCLIENT\": \"customer\",\n  \"RECEIVERNAME\": \"" + RECEIVER_NAME + 
    "\",\n  \"RECEIVERPHONE\": \"" + RECEIVER_PHONE + 
    "\",\n  \"RECEIVERCLIENT\": \"merchant\",\n  \"SUBUSERPHONE\": \"" + SUBMERCHANT_MOBILE + 
    "\",\n  \"SUBUSERCLIENT\": \"merchant\",\n  \"AMOUNT\": " + amount + 
    ",\n  \"CURRENCY\": \"MMK\",\n  \"DEVICEID\": \"" + DEVICE_ID + 
    "\",\n  \"MessageType\": \"FO\",\n  \"SERVICEID\": \"" + SERVICE_ID + 
    "\",\n  \"SERVICECODE\": \"" + SERVICE_CODE + "\",\n  \"VOUCHER\": \"\",\n  \"SHOPID\": \"" + SHOP_ID +"\"\n}";    

    RequestBody body = RequestBody.create(reqBodyCreateTxn, mediaType);

    Request request = new Request.Builder()
      .url(baseUrl + wsoBasePath + createTxnEndPoint)
      .method("POST", body)
      .addHeader("Authorization", "Bearer " + userToken.token.token)
      .addHeader("Token", "Bearer " + accessToken.access_token)
      .addHeader("Content-Type", "application/json")
      .build();
    
    try (Response response = client.newCall(request).execute()) {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode rootNode = mapper.readTree(response.body().byteStream());

      System.out.println("create_txn err: " + rootNode.get("err").asText());
      System.out.println("create_txn msg: " + rootNode.get("message").asText());

      if (rootNode.get("err").asInt() == 200) {
        TXN_STATUS = "TXN_CREATED";
        TXN_ID = "";
        TXN_ID = rootNode.get("data").get("TRANSREFID").asText();

        // Step 5: execute transaction
        execTxn();
      }
      else{
        TXN_STATUS = "CREATE_TXN_FAILED";
      }
    }
  }

  //STEP 3
  void validateQr(String qr, Long amount) throws IOException {
    MediaType mediaType = MediaType.parse("application/json");
    String reqBodyValidateQr = "{\n  \"qrcode\": \"" + qr + 
    "\",\n  \"deviceId\": \"" + DEVICE_ID + 
    "\",\n  \"shopId\": \"" + SHOP_ID + 
    "\",\n  \"amount\": " + amount + 
    ",\n  \"currency\": \"MMK\"\n}\n";    
    RequestBody body = RequestBody.create(reqBodyValidateQr, mediaType);

    Request request = new Request.Builder()
      .url(baseUrl + wsoBasePath + checkQrEndPoint)
      .method("POST", body)
      .addHeader("Authorization", "Bearer " + userToken.token.token)
      .addHeader("Token", "Bearer " + accessToken.access_token)
      .addHeader("Content-Type", "application/json")
      .build();
    
    try (Response response = client.newCall(request).execute()) {
      respQr = respQrJsonAdapter.fromJson(response.body().source());
      System.out.println("data: " + respQr.err);
      
      if (respQr.err.equals("200")) {
        TXN_STATUS = "QR_VALIDATED";
        SENDER_NAME = ""; 
        SENDER_PHONE = "";

        //in production: parse data array and check with fieldMapping key
        SENDER_NAME = respQr.data.get(1).value;
        SENDER_PHONE = respQr.data.get(2).value;
        
        System.out.println("sender: " + SENDER_NAME + ", " + SENDER_PHONE);
        
        //Step 4: create transaction
        try {
          createTxn(qr, amount);
        } catch (IOException e) {
          e.printStackTrace();
        }
        System.out.println("txnId: " + TXN_ID);
      }
      else{
        TXN_STATUS = "QR_VALIDATION_FAILED";
      }
    }
  }

  //STEP 2
  void userToken() throws IOException {
    MediaType mediaType = MediaType.parse("application/json");    
    RequestBody body = RequestBody.create("{\n  \"phone\": \"" + MERCHANT_MOBILE + 
    "\",\n  \"password\": \"" + MERCHANT_PIN + "\"\n}", mediaType);
    Request request = new Request.Builder()
      .url(baseUrl + wsoBasePath + userTokenEndPoint)
      .method("POST", body)
      .addHeader("Token", "Bearer " + accessToken.access_token)
      .addHeader("apikey", "Basic 2PkEcH0KgRfuXQW7AwpEyGDBw7LKltKs")
      .addHeader("Content-Type", "application/json")
      .build();
    
    try (Response response = client.newCall(request).execute()) {
      userToken = userTokenJsonAdapter.fromJson(response.body().source());
    }
  }

  //STEP 1
  void accessToken() throws IOException {
    MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
    RequestBody body = RequestBody.create("grant_type=client_credentials", mediaType);
    String base64Key = Base64.getEncoder().encodeToString((APP_KEY + ":" + APP_SECRET).getBytes());

    System.out.println(base64Key);
    
    Request request = new Request.Builder()
        .url(baseUrl + accessTokenEndPoint)
        .post(body)
        .addHeader("Content-Type", "application/x-www-form-urlencoded")
        .addHeader("Authorization", "Basic " + base64Key)
        .build();
    try (Response response = client.newCall(request).execute()) {
      accessToken = accessTokenJsonAdapter.fromJson(response.body().source());
    }
  }

	@GetMapping("/ayapay")
  public String processAyaPay(@RequestParam(value = "qr", defaultValue = "") String qr,
  @RequestParam(value="amount", defaultValue = "0") Long amount){    
    TXN_STATUS = "init";
    
    //Step 1: generate access token, you can store this token locally and check for expiry to reduce server calls
    try {
      accessToken();
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("access_token: " + accessToken.access_token);

    //Step 2: generate user token, also can store and check for expiry
    try {
      userToken();
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("user_token: " + userToken.token.token);

    //Step 3: validate QR
    try {
      validateQr(qr, amount);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return TXN_STATUS;
  }
}