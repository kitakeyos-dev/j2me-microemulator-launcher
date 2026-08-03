package me.kitakeyos.j2me.infrastructure.persistence.network;

import me.kitakeyos.j2me.domain.network.model.RedirectionRule;
import me.kitakeyos.j2me.domain.network.repository.NetworkRuleCodec;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Field mapping for {@link RedirectionRule}. The type id matches the prefix
 * the previous hand-rolled writer used, so rule files written by older builds
 * still load.
 */
public class RedirectionRuleCodec implements NetworkRuleCodec<RedirectionRule> {

    @Override
    public String typeId() {
        return "redirect";
    }

    @Override
    public Class<RedirectionRule> ruleType() {
        return RedirectionRule.class;
    }

    @Override
    public Map<String, String> encode(RedirectionRule rule) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("originalHost", rule.getOriginalHost());
        fields.put("originalPort", String.valueOf(rule.getOriginalPort()));
        fields.put("targetHost", rule.getTargetHost());
        fields.put("targetPort", String.valueOf(rule.getTargetPort()));
        fields.put("instanceId", String.valueOf(rule.getInstanceId()));
        fields.put("enabled", String.valueOf(rule.isEnabled()));
        return fields;
    }

    @Override
    public RedirectionRule decode(Map<String, String> fields) {
        String originalHost = fields.get("originalHost");
        String targetHost = fields.get("targetHost");
        if (originalHost == null || targetHost == null) {
            return null;
        }

        RedirectionRule rule = new RedirectionRule(
                originalHost,
                RuleFields.asInt(fields.get("originalPort"), 0),
                targetHost,
                RuleFields.asInt(fields.get("targetPort"), 0),
                RuleFields.asInt(fields.get("instanceId"), RedirectionRule.ALL_INSTANCES));
        rule.setEnabled(RuleFields.asBoolean(fields.get("enabled"), true));
        return rule;
    }
}
