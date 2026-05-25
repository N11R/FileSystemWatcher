package filewatcher;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * EmailService handles Gmail SMTP authentication and email delivery
 * for the File System Watcher application.
 *
 * <p>Credentials are loaded from a {@code config.properties} file
 * located in the application's working directory. They are never
 * hardcoded in source code.</p>
 *
 * <p><b>SRS Coverage:</b></p>
 * <ul>
 *   <li>FR-5.3 — Gmail SMTP authenticated sending</li>
 *   <li>FR-5.4 — CSV file attached to email</li>
 *   <li>FR-5.5 — Subject and body identify the report</li>
 *   <li>FR-5.6 — Returns true on successful delivery</li>
 *   <li>FR-5.7 — Handles auth and network errors gracefully</li>
 *   <li>FR-5.8 — Credentials loaded from config file, not hardcoded</li>
 * </ul>
 *
 * <p><b>config.properties format:</b></p>
 * <pre>
 * email=yourname@gmail.com
 * password=abcd efgh ijkl mnop
 * </pre>
 *
 * @author Mariam Hussein
 * @version 1.0
 */
public class EmailService {

    // FIELD - Gmail SMTP server address (never changes for Gmail)
    private final String smtpHost;

    // FIELD - Gmail SMTP port number (587 is standard for TLS)
    private final int smtpPort;

    // FIELD - sender's Gmail address, loaded from config.properties
    private final String senderEmail;

    // FIELD - Gmail App Password, loaded from config.properties
    private final String appPassword;

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    /**
     * Constructs an EmailService by loading credentials from
     * {@code config.properties}. If the file is missing or unreadable,
     * senderEmail and appPassword will be empty strings and sending
     * will fail gracefully.
     */
    public EmailService() {
        // create a Properties object to read the config file
        Properties config = new Properties();

        try {
            // open the config file from the working directory
            FileInputStream fis = new FileInputStream("config.properties");
            // load key-value pairs into config
            config.load(fis);
            fis.close();
        } catch (IOException e) {
            System.err.println("Could not load config.properties: "
                    + e.getMessage());
        }

        // SMTP host and port are fixed for Gmail — not sensitive
        this.smtpHost    = "smtp.gmail.com";
        this.smtpPort    = 587;

        // load credentials from file; default to empty string if missing
        this.senderEmail = config.getProperty("email", "");
        this.appPassword = config.getProperty("password", "");
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE METHOD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Creates and returns an authenticated Gmail SMTP {@link Session}.
     * Called internally by {@link #sendEmail}.
     *
     * @return an authenticated {@link Session} ready for sending email
     */
    private Session authenticate() {
        // OBJECT - holds SMTP connection settings
        Properties props = new Properties();

        // tell JavaMail to require login credentials
        props.put("mail.smtp.auth", "true");

        // enable TLS encryption on the connection
        props.put("mail.smtp.starttls.enable", "true");

        // set the Gmail server address
        props.put("mail.smtp.host", smtpHost);

        // set the port number as a String (JavaMail expects String)
        props.put("mail.smtp.port", String.valueOf(smtpPort));

        // create and return an authenticated session
        return Session.getInstance(props, new Authenticator() {

            // METHOD OVERRIDE - supplies credentials when JavaMail asks
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, appPassword);
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC METHOD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Sends an email with the given CSV file attached to the recipient.
     *
     * <p><b>SRS:</b> FR-5.3 through FR-5.8.</p>
     *
     * @param recipient  the destination email address
     * @param attachment the CSV file to attach
     * @return {@code true} if the email was sent successfully,
     *         {@code false} if an error occurred
     */
    public boolean sendEmail(final String recipient, final File attachment) {
        try {
            // STEP 1 - get the authenticated Gmail session
            Session session = authenticate();

            // STEP 2 - create a new email message
            Message message = new MimeMessage(session);

            // STEP 3 - set the FROM address
            message.setFrom(new InternetAddress(senderEmail));

            // STEP 4 - set the TO address
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(recipient)
            );

            // STEP 5 - set the subject line (FR-5.5)
            message.setSubject(
                    "File System Watcher Report - "
                            + java.time.LocalDate.now()
            );

            // STEP 6 - create the plain text body part
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(
                    "Please find the attached File System Watcher report.\n\n"
                            + "Generated on: " + java.time.LocalDateTime.now()
            );

            // STEP 7 - create the attachment part and attach the CSV file
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.attachFile(attachment);

            // STEP 8 - combine text body and attachment into one multipart
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(attachmentPart);

            // STEP 9 - set the full multipart content on the message
            message.setContent(multipart);

            // STEP 10 - send the email through Gmail SMTP
            Transport.send(message);

            // STEP 11 - return true to confirm success (FR-5.6)
            return true;

        } catch (MessagingException e) {
            // handles authentication failure or network error (FR-5.7)
            System.err.println("Failed to send email: " + e.getMessage());
            return false;

        } catch (IOException e) {
            // handles file attachment error (FR-5.7)
            System.err.println("Failed to attach file: " + e.getMessage());
            return false;
        }
    }
}