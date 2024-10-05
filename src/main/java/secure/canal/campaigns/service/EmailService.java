package secure.canal.campaigns.service;

import secure.canal.campaigns.enumeration.VerificationType;

public interface EmailService {

    void sendVerificationEmail(String firstName, String email, String verificationUrl, VerificationType verificationType);

    void sendSMS(String recipient, String message);
}
