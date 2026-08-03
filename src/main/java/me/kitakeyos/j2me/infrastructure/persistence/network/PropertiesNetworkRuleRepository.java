package me.kitakeyos.j2me.infrastructure.persistence.network;

import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.domain.network.model.SocketRule;
import me.kitakeyos.j2me.domain.network.repository.NetworkRuleCodec;
import me.kitakeyos.j2me.domain.network.repository.NetworkRuleRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Stores network rules in {@code network_rules.properties} using the layout
 * {@code <typeId>.count} plus {@code <typeId>.<index>.<field>}.
 * <p>
 * The repository owns that layout and nothing else: which fields a rule has is
 * entirely a {@link NetworkRuleCodec} concern, so supporting a new rule type is
 * a registration, not an edit here.
 */
public class PropertiesNetworkRuleRepository implements NetworkRuleRepository {

    private static final Logger logger = Logger.getLogger(PropertiesNetworkRuleRepository.class.getName());
    private static final String NETWORK_RULES_FILE = "network_rules.properties";

    private final ApplicationConfig applicationConfig;
    /** typeId -> codec, in registration order so saved files stay stable. */
    private final Map<String, NetworkRuleCodec<?>> codecsByType = new LinkedHashMap<>();
    /** rule class -> codec, for dispatching on save. */
    private final Map<Class<?>, NetworkRuleCodec<?>> codecsByClass = new HashMap<>();

    public PropertiesNetworkRuleRepository(ApplicationConfig applicationConfig,
            List<NetworkRuleCodec<?>> codecs) {
        this.applicationConfig = applicationConfig;
        for (NetworkRuleCodec<?> codec : codecs) {
            codecsByType.put(codec.typeId(), codec);
            codecsByClass.put(codec.ruleType(), codec);
        }
    }

    @Override
    public List<SocketRule> loadAll() {
        List<SocketRule> rules = new ArrayList<>();
        File configFile = getConfigFile();
        if (!configFile.exists()) {
            return rules;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);
        } catch (IOException e) {
            logger.severe("Failed to load network rules: " + e.getMessage());
            return rules;
        }

        for (NetworkRuleCodec<?> codec : codecsByType.values()) {
            rules.addAll(readSection(props, codec));
        }
        logger.info("Loaded " + rules.size() + " network rules from " + configFile.getAbsolutePath());
        return rules;
    }

    @Override
    public void saveAll(List<SocketRule> rules) {
        Properties props = new Properties();

        for (NetworkRuleCodec<?> codec : codecsByType.values()) {
            writeSection(props, codec, rules);
        }

        File configFile = getConfigFile();
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "Network Rules Configuration");
            logger.info("Network rules saved to " + configFile.getAbsolutePath());
        } catch (IOException e) {
            logger.severe("Failed to save network rules: " + e.getMessage());
        }
    }

    private List<SocketRule> readSection(Properties props, NetworkRuleCodec<?> codec) {
        List<SocketRule> rules = new ArrayList<>();
        int count = RuleFields.asInt(props.getProperty(codec.typeId() + ".count"), 0);

        for (int i = 0; i < count; i++) {
            String prefix = codec.typeId() + "." + i + ".";
            Map<String, String> fields = new HashMap<>();
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith(prefix)) {
                    fields.put(key.substring(prefix.length()), props.getProperty(key));
                }
            }
            if (fields.isEmpty()) {
                continue;
            }
            try {
                SocketRule rule = codec.decode(fields);
                if (rule != null) {
                    rules.add(rule);
                }
            } catch (RuntimeException e) {
                // One malformed entry must not cost the user the rest of their rules.
                logger.warning("Skipping unreadable " + codec.typeId() + " rule at index " + i + ": " + e);
            }
        }
        return rules;
    }

    /**
     * Encode every rule this codec claims. Indices are assigned per section, so
     * they stay dense even when the caller's list interleaves rule types.
     */
    private <T extends SocketRule> void writeSection(Properties props, NetworkRuleCodec<T> codec,
            List<SocketRule> rules) {
        int index = 0;
        for (SocketRule rule : rules) {
            if (!codec.ruleType().isInstance(rule)) {
                continue;
            }
            String prefix = codec.typeId() + "." + index + ".";
            for (Map.Entry<String, String> field : codec.encode(codec.ruleType().cast(rule)).entrySet()) {
                if (field.getValue() != null) {
                    props.setProperty(prefix + field.getKey(), field.getValue());
                }
            }
            index++;
        }
        props.setProperty(codec.typeId() + ".count", String.valueOf(index));
    }

    private File getConfigFile() {
        return new File(applicationConfig.getDataDirectory(), NETWORK_RULES_FILE);
    }
}
