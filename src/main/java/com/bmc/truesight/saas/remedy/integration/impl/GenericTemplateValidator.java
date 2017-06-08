package com.bmc.truesight.saas.remedy.integration.impl;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.bmc.truesight.saas.remedy.integration.TemplateValidator;
import com.bmc.truesight.saas.remedy.integration.beans.Configuration;
import com.bmc.truesight.saas.remedy.integration.beans.Event;
import com.bmc.truesight.saas.remedy.integration.beans.EventSource;
import com.bmc.truesight.saas.remedy.integration.beans.FieldItem;
import com.bmc.truesight.saas.remedy.integration.beans.Template;
import com.bmc.truesight.saas.remedy.integration.exception.ValidationException;
import com.bmc.truesight.saas.remedy.integration.util.Message;
import com.bmc.truesight.saas.remedy.integration.util.StringUtil;

/**
 * Generic Template Validator for templates
 *
 * @author vitiwari
 *
 */
public class GenericTemplateValidator implements TemplateValidator {

    @Override
    public boolean validate(Template template) throws ValidationException {
        Configuration config = template.getConfig();
        Event payload = template.getEventDefinition();
        Map<String, FieldItem> fieldItemMap = template.getFieldItemMap();

        if (config.getRemedyHostName().isEmpty()
                || config.getRemedyUserName().isEmpty()
                || config.getRemedyPassword().isEmpty()
                || config.getTsiEventEndpoint().isEmpty()
                || config.getTsiApiToken().isEmpty()
                || (config.getConditionFields().size() == 0)
                || (config.getChunkSize() <= 0)
                || (config.getRetryConfig() < 0)
                || (config.getWaitMsBeforeRetry() <= 0)
                || (StringUtils.isEmpty(config.getStartDateTime().toString()))
                || (StringUtils.isEmpty(config.getEndDateTime().toString()))) {
            throw new ValidationException(StringUtil.format(Message.CONFIG_VALIDATION_FAILED, new Object[]{}));
        }

        // validate payload configuration
        if (payload.getTitle() != null && payload.getTitle().startsWith("@") && !fieldItemMap.containsKey(payload.getTitle())) {
            throw new ValidationException(StringUtil.format(Message.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING,
                    new Object[]{payload.getTitle()}));
        }

        // validate payload configuration
        for (String fpField : payload.getFingerprintFields()) {
            if (fpField != null && fpField.startsWith("@") && !fieldItemMap.containsKey(fpField)) {
                throw new ValidationException(
                        StringUtil.format(Message.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{fpField}));
            }
        }

        // validate payload configuration
        Map<String, String> properties = payload.getProperties();
        for (String key : properties.keySet()) {

            if (properties.get(key).startsWith("@") && !fieldItemMap.containsKey(properties.get(key))) {
                throw new ValidationException(StringUtil.format(Message.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{properties.get(key)}));
            }
        }

        if (payload.getSeverity() != null && payload.getSeverity().startsWith("@") && !fieldItemMap.containsKey(payload.getSeverity())) {
            throw new ValidationException(StringUtil.format(Message.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getSeverity()}));
        }

        if (payload.getStatus() != null && payload.getStatus().startsWith("@") && !fieldItemMap.containsKey(payload.getStatus())) {
            throw new ValidationException(
                    StringUtil.format(Message.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getStatus()}));
        }

        if (payload.getCreatedAt() != null && payload.getCreatedAt().startsWith("@") && !fieldItemMap.containsKey(payload.getCreatedAt())) {
            throw new ValidationException(StringUtil.format(Message.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getCreatedAt()}));
        }

        if (payload.getEventClass() != null && payload.getEventClass().startsWith("@") && !fieldItemMap.containsKey(payload.getEventClass())) {
            throw new ValidationException(StringUtil.format(Message.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getEventClass()}));
        }

        //valiadting source
        EventSource source = payload.getSource();
        if (source.getName() != null && source.getName().startsWith("@") && !fieldItemMap.containsKey(source.getName())) {
            throw new ValidationException(StringUtil.format(Message.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{source.getName()}));
        }
        if (source.getType() != null && source.getType().startsWith("@") && !fieldItemMap.containsKey(source.getType())) {
            throw new ValidationException(StringUtil.format(Message.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{source.getType()}));
        }
        if (source.getRef() != null && source.getRef().startsWith("@") && !fieldItemMap.containsKey(source.getRef())) {
            throw new ValidationException(StringUtil.format(Message.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{source.getRef()}));
        }

        EventSource sender = payload.getSender();
        if (sender.getName() != null && sender.getName().startsWith("@") && !fieldItemMap.containsKey(sender.getName())) {
            throw new ValidationException(StringUtil.format(Message.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{sender.getName()}));
        }
        if (sender.getType() != null && sender.getType().startsWith("@") && !fieldItemMap.containsKey(sender.getType())) {
            throw new ValidationException(StringUtil.format(Message.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{sender.getType()}));
        }
        if (sender.getRef() != null && sender.getRef().startsWith("@") && !fieldItemMap.containsKey(sender.getRef())) {
            throw new ValidationException(StringUtil.format(Message.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{sender.getRef()}));
        }
        return true;
    }

}