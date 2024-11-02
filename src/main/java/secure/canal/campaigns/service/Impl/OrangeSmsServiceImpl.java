package secure.canal.campaigns.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import secure.canal.campaigns.payload.BalanceResponse;
import secure.canal.campaigns.payload.TokenResponse;
import secure.canal.campaigns.service.OrangeSmsService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;


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
    public BalanceResponse getSmsBalance(String token) {
        try {
            HttpResponse<String> response = Unirest.get(smsBalanceUrl)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .asString();

            if (response.getStatus() == 200) {
                log.info("Retrieved SMS balance successfully");
                return extractBalanceResponse(response.getBody());
            } else {
                log.warn("Failed to retrieve SMS balance: " + response.getBody());
                return null;  // Handle error case appropriately
            }
        } catch (Exception e) {
            log.error("Error retrieving SMS balance", e);
            throw new RuntimeException("Error retrieving SMS balance", e);
        }
    }

    private BalanceResponse extractBalanceResponse(String responseBody) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        if (jsonNode.isArray() && jsonNode.size() > 0) {
            JsonNode firstContract = jsonNode.get(0);
            BalanceResponse balanceResponse = new BalanceResponse();
            balanceResponse.setAvailableUnits(firstContract.get("availableUnits").asLong());
            balanceResponse.setStatus(firstContract.get("status").asText());
            balanceResponse.setExpirationDate(parseDate(firstContract.get("expirationDate").asText()));
            return balanceResponse;
        } else {
            throw new IllegalArgumentException("Invalid response format or empty array");
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
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
        // Escaping only the double quotes
        return message.replace("\"", "\\\"");
    }




    private String constructSmsRequestBody(String recipient, String senderName, String message) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // Create the body of the request
            Map<String, Object> outboundSmsMessageRequest = new HashMap<>();
            outboundSmsMessageRequest.put("address", "tel:+224" + recipient);
            outboundSmsMessageRequest.put("senderAddress", senderAddress);
            outboundSmsMessageRequest.put("senderName", senderName);

            // Create the message part
            Map<String, String> outboundSmsTextMessage = new HashMap<>();
            outboundSmsTextMessage.put("message", message);

            // Combine into a final structure
            outboundSmsMessageRequest.put("outboundSMSTextMessage", outboundSmsTextMessage);

            // Wrap everything into the outer request object
            Map<String, Object> finalRequestBody = new HashMap<>();
            finalRequestBody.put("outboundSMSMessageRequest", outboundSmsMessageRequest);

            // Convert to JSON
            return objectMapper.writeValueAsString(finalRequestBody);

        } catch (Exception e) {
            throw new RuntimeException("Error constructing SMS request body", e);
        }
    }
}


