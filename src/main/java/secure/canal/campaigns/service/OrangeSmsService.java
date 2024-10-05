package secure.canal.campaigns.service;

import secure.canal.campaigns.payload.TokenResponse;

public interface OrangeSmsService {

    public TokenResponse getOAuthToken();

    public void sendSms(String token, String recipient, String senderName, String message);

    int getSmsBalance(String token);
}
