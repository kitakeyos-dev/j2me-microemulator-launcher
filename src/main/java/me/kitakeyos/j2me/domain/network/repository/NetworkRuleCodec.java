package me.kitakeyos.j2me.domain.network.repository;

import me.kitakeyos.j2me.domain.network.model.SocketRule;

import java.util.Map;

/**
 * Serialises one rule type to and from a flat field map.
 * <p>
 * The repository owns the file layout; a codec owns only its own fields. That
 * split is what makes a new rule type a matter of registering another codec
 * instead of editing the save and load routines.
 *
 * @param <T> the rule type handled by this codec
 */
public interface NetworkRuleCodec<T extends SocketRule> {

    /**
     * Stable identifier written into the store. Changing it orphans existing
     * saved rules of this type.
     */
    String typeId();

    /**
     * The concrete rule class this codec handles.
     */
    Class<T> ruleType();

    /**
     * Flatten the rule. Keys must not contain the repository's separator ('.').
     */
    Map<String, String> encode(T rule);

    /**
     * Rebuild a rule from previously encoded fields.
     *
     * @return the rule, or {@code null} if the fields are unusable
     */
    T decode(Map<String, String> fields);
}
