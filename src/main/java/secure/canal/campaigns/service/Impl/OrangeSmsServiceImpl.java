package secure.canal.campaigns.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import secure.canal.campaigns.payload.TokenResponse;
import secure.canal.campaigns.service.OrangeSmsService;

import java.io.IOException;


@Service
@Slf4j
public class OrangeSmsServiceImpl implements OrangeSmsService {

    @Value("${orange.api.oauth.url}")
    private String oauthUrl;

    @Value("${orange.api.sms.url}")
    private String smsApiUrl;

    @Value("${orange.api.client.credentials}")
    private String clientCredentials;

    @Value("${orange.api.sender.address}")
    private String senderAddress;

    @Value("${orange.api.sms.balance.url}")
    private String smsBalanceUrl;

    private final ObjectMapper objectMapper;

    // Constructor injection for ObjectMapper for better testability
    public OrangeSmsServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public int getSmsBalance(String token) {
        try {
            HttpResponse<String> response = Unirest.get(smsBalanceUrl)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .asString();

            if (response.getStatus() == 200) {
                log.info("Retrieved SMS balance successfully");
                return extractAvailableUnits(response.getBody());
            } else {
                log.warn("Failed to retrieve SMS balance: " + response.getBody());
                return -1;  // Return -1 or handle an error case
            }
        } catch (Exception e) {
            log.error("Error retrieving SMS balance", e);
            throw new RuntimeException("Error retrieving SMS balance", e);
        }
    }

    private int extractAvailableUnits(String responseBody) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        if (jsonNode.isArray() && jsonNode.size() > 0) {
            JsonNode firstContract = jsonNode.get(0);
            if (firstContract.has("availableUnits")) {
                return firstContract.get("availableUnits").asInt();
            } else {
                throw new IllegalArgumentException("No 'availableUnits' field found in response");
            }
        } else {
            throw new IllegalArgumentException("Invalid response format or empty array");
        }
    }

    @Override
    public TokenResponse getOAuthToken() {
        try {
            HttpResponse<String> response = Unirest.post(oauthUrl)
                    .header("Authorization", clientCredentials)
                    .header("Accept", "application/json")
                    .field("grant_type", "client_credentials")
                    .asString();

            if (response.getStatus() == 200) {
                String token = extractToken(response.getBody());
                return new TokenResponse(token, (long) response.getStatus());
            } else {
                throw new RuntimeException("Failed to get OAuth token. Status: " + response.getStatus());
            }
        } catch (Exception e) {
            // Log the exception details using a logging framework
            log.error("Error getting OAuth token from Orange API", e);
            throw new RuntimeException("Error getting OAuth token", e);
        }
    }

    private String extractToken(String responseBody) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        if (jsonNode.has("access_token")) {
            return jsonNode.get("access_token").asText();
        } else {
            throw new IllegalArgumentException("No 'access_token' field found in response");
        }
    }

    @Override
    public void sendSms(String token, String recipient, String senderName, String message) {
        String escapedMessage = escapeMessage(message);

        try {
            HttpResponse<String> response = Unirest.post(smsApiUrl)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .body(constructSmsRequestBody(recipient, senderName, escapedMessage))
                    .asString();

            if (response.getStatus() == 201) {
                log.info("SMS sent successfully!");
            } else {
                log.warn("Failed to send SMS: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("Error sending SMS", e);
            throw new RuntimeException("Error sending SMS", e);
        }
    }

    private String escapeMessage(String message) {
        return message.replace("\n", "\\n").replace("\"", "\\\"");
    }

    private String constructSmsRequestBody(String recipient, String senderName, String message) {
        return "{\r\n" +
                "  \"outboundSMSMessageRequest\": {\r\n" +
                "    \"address\": \"tel:+224" + recipient + "\",\r\n" +
                "    \"senderAddress\": \"" + senderAddress + "\",\r\n" +
                "    \"senderName\": \"" + senderName + "\",\r\n" +
                "    \"outboundSMSTextMessage\": {\r\n" +
                "      \"message\": \"" + message + "\"\r\n" +
                "    }\r\n" +
                "  }\r\n" +
                "}";
    }
}


