package dev.kairos.admin.feature.dashboard;

import com.vaadin.flow.component.page.WebStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Reads and writes the user's dashboard layout (card order and which cards are
 * hidden) to browser {@code localStorage}. Values are stored as comma-separated
 * {@link CardId} names under two keys. Reads are asynchronous because browser
 * storage is only reachable via a client round-trip.
 */
public final class DashboardPrefs {

    private static final Logger logger = LoggerFactory.getLogger(DashboardPrefs.class);

    private static final String KEY_ORDER = "kairos.dashboard.order";
    private static final String KEY_HIDDEN = "kairos.dashboard.hidden";
    private static final String SEPARATOR = ",";

    private final List<CardId> order;
    private final Set<CardId> hidden;

    private DashboardPrefs(List<CardId> order, Set<CardId> hidden) {
        this.order = order;
        this.hidden = hidden;
    }

    /** The default layout: every card, in {@link CardId} declaration order, none hidden. */
    public static DashboardPrefs defaults() {
        return new DashboardPrefs(new ArrayList<>(Arrays.asList(CardId.values())), new LinkedHashSet<>());
    }

    /**
     * Asynchronously loads the stored order, then the stored hidden set, and
     * hands the resulting prefs to {@code onLoaded}. Unknown or missing values
     * fall back to defaults, and any card new since the value was stored is
     * appended so it is never lost.
     */
    public static void load(Consumer<DashboardPrefs> onLoaded) {
        WebStorage.getItem(KEY_ORDER, orderRaw ->
                WebStorage.getItem(KEY_HIDDEN, hiddenRaw -> {
                    DashboardPrefs prefs = from(orderRaw, hiddenRaw);
                    logger.debug("Dashboard prefs loaded from browser storage: order={} cards, hidden={} cards",
                            prefs.order.size(), prefs.hidden.size());
                    onLoaded.accept(prefs);
                }));
    }

    private static DashboardPrefs from(String orderRaw, String hiddenRaw) {
        List<CardId> order = parseOrder(orderRaw);
        Set<CardId> hidden = new LinkedHashSet<>(parse(hiddenRaw));
        return new DashboardPrefs(order, hidden);
    }

    private static List<CardId> parseOrder(String raw) {
        List<CardId> stored = parse(raw);
        List<CardId> order = new ArrayList<>(stored);
        for (CardId id : CardId.values()) {
            if (!order.contains(id)) {
                order.add(id);
            }
        }
        return order;
    }

    private static List<CardId> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        List<CardId> ids = new ArrayList<>();
        for (String token : raw.split(SEPARATOR)) {
            toCardId(token.trim()).ifPresent(ids::add);
        }
        return ids;
    }

    private static Optional<CardId> toCardId(String token) {
        try {
            return Optional.of(CardId.valueOf(token));
        } catch (IllegalArgumentException ex) {
            logger.warn("Unrecognized card id '{}' in browser storage — skipping (stale or renamed value)", token);
            return Optional.empty();
        }
    }

    /**
     * Returns the current card order, reflecting any drag-to-reorder changes
     * made since the prefs were loaded. The list always contains every
     * {@link CardId}, including cards that are hidden.
     *
     * @return mutable ordered list of all card identifiers
     */
    public List<CardId> order() {
        return order;
    }

    /**
     * Returns {@code true} when {@code id} should be rendered on the dashboard
     * (i.e. the user has not hidden it).
     *
     * @param id the card to test
     * @return {@code true} if visible, {@code false} if hidden
     */
    public boolean isVisible(CardId id) {
        return !hidden.contains(id);
    }

    /**
     * Shows or hides a card. Does not persist the change; call {@link #save()}
     * afterwards to write to browser storage.
     *
     * @param id      the card whose visibility to change
     * @param visible {@code true} to show the card, {@code false} to hide it
     */
    public void setVisible(CardId id, boolean visible) {
        if (visible) {
            hidden.remove(id);
        } else {
            hidden.add(id);
        }
    }

    /** Moves {@code moved} to the position of {@code target}, shifting the rest. */
    public void reorder(CardId moved, CardId target) {
        if (moved == target || !order.contains(moved) || !order.contains(target)) {
            return;
        }
        order.remove(moved);
        order.add(order.indexOf(target), moved);
    }

    /** Restores the default order and clears all hidden cards (does not persist). */
    public void resetToDefaults() {
        order.clear();
        order.addAll(Arrays.asList(CardId.values()));
        hidden.clear();
    }

    /** Persists the current order and hidden set to browser storage. */
    public void save() {
        WebStorage.setItem(KEY_ORDER, join(order));
        WebStorage.setItem(KEY_HIDDEN, join(hidden));
        logger.debug("Dashboard prefs saved to browser storage: order={} cards, hidden={} cards",
                order.size(), hidden.size());
    }

    private static String join(Iterable<CardId> ids) {
        StringBuilder sb = new StringBuilder();
        for (CardId id : ids) {
            if (sb.length() > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(id.name());
        }
        return sb.toString();
    }
}
