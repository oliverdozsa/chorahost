package pub.galactic.chorahost.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Emails the invitation code once the invitation is committed. A mail failure
 * is logged but never fails the request: the invitation exists and the code is
 * also returned to the inviting admin.
 */
@ApplicationScoped
public class InvitationMailer {

    private static final Logger LOG = Logger.getLogger(InvitationMailer.class);

    @Inject
    Mailer mailer;

    void onInvitationCreated(@Observes(during = TransactionPhase.AFTER_SUCCESS) InvitationCreated event) {
        String subject = event.inviterName() + " invited you to join " + event.teamName() + " on ChoraForge";
        String body = """
                Hi,

                %s invited you to join the team "%s" on ChoraForge.

                Sign in with this email address (%s) and use this invitation code to join:

                %s

                The invitation expires on %s.
                """.formatted(event.inviterName(), event.teamName(), event.email(), event.code(),
                event.expiresAt());
        try {
            mailer.send(Mail.withText(event.email(), subject, body));
        } catch (RuntimeException e) {
            LOG.errorf(e, "Failed to send invitation email to %s", event.email());
        }
    }
}
