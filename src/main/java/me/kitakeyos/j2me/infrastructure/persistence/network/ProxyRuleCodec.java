package me.kitakeyos.j2me.infrastructure.persistence.network;

import me.kitakeyos.j2me.domain.network.model.ProxyRule;
import me.kitakeyos.j2me.domain.network.repository.NetworkRuleCodec;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Field mapping for {@link ProxyRule}. The type id matches the prefix the
 * previous hand-rolled writer used, so existing rule files still load.
 */
public class ProxyRuleCodec implements NetworkRuleCodec<ProxyRule> {

    @Override
    public String typeId() {
        return "proxy";
    }

    @Override
    public Class<ProxyRule> ruleType() {
        return ProxyRule.class;
    }

    @Override
    public Map<String, String> encode(ProxyRule rule) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("type", rule.getProxyType().name());
        fields.put("host", rule.getProxyHost());
        fields.put("port", String.valueOf(rule.getProxyPort()));
        fields.put("instanceId", String.valueOf(rule.getInstanceId()));
        fields.put("enabled", String.valueOf(rule.isEnabled()));
        if (rule.getUsername() != null) {
            fields.put("username", rule.getUsername());
        }
        if (rule.getPassword() != null) {
            fields.put("password", rule.getPassword());
        }
        return fields;
    }

    @Override
    public ProxyRule decode(Map<String, String> fields) {
        String typeName = fields.get("type");
        String host = fields.get("host");
        if (typeName == null || host == null) {
            return null;
        }

        ProxyRule.ProxyType type;
        try {
            type = ProxyRule.ProxyType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return null;
        }

        int port = RuleFields.asInt(fields.get("port"), 0);
        int instanceId = RuleFields.asInt(fields.get("instanceId"), ProxyRule.ALL_INSTANCES);
        String username = fields.get("username");

        ProxyRule rule = (username != null && !username.isEmpty())
                ? new ProxyRule(type, host, port, instanceId, username, fields.get("password"))
                : new ProxyRule(type, host, port, instanceId);
        rule.setEnabled(RuleFields.asBoolean(fields.get("enabled"), true));
        return rule;
    }
}
