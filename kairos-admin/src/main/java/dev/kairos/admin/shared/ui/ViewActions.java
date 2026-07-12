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

    /**
     * Like {@link #execute}, but runs {@code refresh} on both success and
     * failure. Use this for an <em>optimistic</em> control (e.g. a grid toggle
     * switch the client already flipped): a failed action must still re-render
     * the row from server state, otherwise the control keeps showing the
     * un-applied value.
     *
     * @param action         the API call to run
     * @param successMessage notification shown when the action succeeds
     * @param failureMessage notification shown, and logged, when it fails
     * @param refresh        re-render side effect, run whether the action
     *                       succeeded or failed
     * @param logger         the calling view's logger, so failures log under its category
     */
    public static void executeAndRefresh(Runnable action, String successMessage, String failureMessage,
                                         Runnable refresh, Logger logger) {
        try {
            execute(action, successMessage, failureMessage, () -> { }, logger);
        } finally {
            refresh.run();
        }
    }
}
