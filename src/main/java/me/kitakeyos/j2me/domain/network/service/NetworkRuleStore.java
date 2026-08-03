package me.kitakeyos.j2me.domain.network.service;

import me.kitakeyos.j2me.domain.network.model.ProxyRule;
import me.kitakeyos.j2me.domain.network.model.RedirectionRule;
import me.kitakeyos.j2me.domain.network.model.SocketRule;
import me.kitakeyos.j2me.domain.network.repository.NetworkRuleRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Owns the configured connection rules and keeps them persisted.
 * <p>
 * Holds one heterogeneous list rather than a list per rule type, so adding a
 * rule type does not add a field, a getter pair and two persistence branches.
 * The typed accessors that remain exist for the network monitor UI, which
 * edits redirection and proxy rules through dedicated tables.
 */
public class NetworkRuleStore {

    private static final Logger logger = Logger.getLogger(NetworkRuleStore.class.getName());

    private final List<SocketRule> rules = new CopyOnWriteArrayList<>();
    private final NetworkRuleRepository repository;
    private final NetworkEvents events;

    public NetworkRuleStore(NetworkRuleRepository repository, NetworkEvents events) {
        this.repository = repository;
        this.events = events;
    }

    /**
     * Load the persisted rules, replacing anything currently held.
     */
    public void load() {
        List<SocketRule> loaded = repository.loadAll();
        rules.clear();
        rules.addAll(loaded);
        logger.info("Loaded " + loaded.size() + " network rules");
        events.fireRulesChanged();
    }

    /**
     * Persist the current rule set.
     */
    public void save() {
        repository.saveAll(new ArrayList<>(rules));
    }

    public void addRule(SocketRule rule) {
        rules.add(rule);
        onModified();
    }

    public void removeRule(SocketRule rule) {
        rules.remove(rule);
        onModified();
    }

    /**
     * @return every rule, in registration order
     */
    public List<SocketRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    /**
     * @return every rule of the given type, in registration order
     */
    public <T extends SocketRule> List<T> getRules(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (SocketRule rule : rules) {
            if (type.isInstance(rule)) {
                result.add(type.cast(rule));
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Drop every rule of the given type without persisting — used to reset
     * state before a load.
     */
    public void clearRules(Class<? extends SocketRule> type) {
        rules.removeIf(type::isInstance);
        events.fireRulesChanged();
    }

    // === Typed convenience accessors used by the network monitor UI ===

    public void addRedirectionRule(RedirectionRule rule) {
        addRule(rule);
    }

    public void removeRedirectionRule(RedirectionRule rule) {
        removeRule(rule);
    }

    public List<RedirectionRule> getRedirectionRules() {
        return getRules(RedirectionRule.class);
    }

    public void clearRedirectionRules() {
        clearRules(RedirectionRule.class);
    }

    public void addProxyRule(ProxyRule rule) {
        addRule(rule);
    }

    public void removeProxyRule(ProxyRule rule) {
        removeRule(rule);
    }

    public List<ProxyRule> getProxyRules() {
        return getRules(ProxyRule.class);
    }

    public void clearProxyRules() {
        clearRules(ProxyRule.class);
    }

    private void onModified() {
        events.fireRulesChanged();
        save();
    }
}
