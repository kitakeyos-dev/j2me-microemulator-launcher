package me.kitakeyos.j2me.domain.network.repository;

import me.kitakeyos.j2me.domain.network.model.SocketRule;

import java.util.List;

/**
 * Persistence port for network rules. Implementations live in Infrastructure.
 */
public interface NetworkRuleRepository {

    /**
     * Load every stored rule. Entries whose type has no registered codec, or
     * that fail to parse, are skipped rather than failing the whole load.
     *
     * @return the stored rules, never null
     */
    List<SocketRule> loadAll();

    /**
     * Replace the stored rule set with the given one.
     */
    void saveAll(List<SocketRule> rules);
}
