package dev.kairos.admin.shared.ui;

import org.slf4j.Logger;
import org.springframework.web.client.RestClientResponseException;

/**
 * Runs a view action against the kairos-api and reports the outcome uniformly:
 * a success notification and refresh on success, an error notification and log
 * on failure. Centralizes the try/catch that every CRUD view otherwise repeats.
 */
public final class ViewActions {

    private ViewActions() {
    }

    /**
     * Executes {@code action}; on success shows {@code successMessage} and runs
     * {@code onSuccess} (e.g. a grid refresh), on failure shows
     * {@code failureMessage} and logs the cause via the caller's {@code logger}.
     *
     * @param action         the API call to run
     * @param successMessage notification shown when the action succeeds
     * @param failureMessage notification shown, and logged, when it fails
     * @param onSuccess      side effect to run after a successful action
     * @param logger         the calling view's logger, so failures log under its category
     */
    public static void execute(Runnable action, String successMessage, String failureMessage,
                               Runnable onSuccess, Logger logger) {
        try {
            action.run();
            Notifications.success(successMessage);
            onSuccess.run();
        } catch (RestClientResponseException ex) {
            logger.error("{} — API responded {}: {}", failureMessage, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            Notifications.error(failureMessage);
        } catch (RuntimeException ex) {
            logger.error("{} — request failed", failureMessage, ex);
            Notifications.error(failureMessage);
        }
    }
}
