package com.bmc.truesight.saas.remedy.integration.impl;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.truesight.saas.remedy.integration.TemplateValidator;
import com.bmc.truesight.saas.remedy.integration.beans.Configuration;
import com.bmc.truesight.saas.remedy.integration.beans.EventSource;
import com.bmc.truesight.saas.remedy.integration.beans.FieldItem;
import com.bmc.truesight.saas.remedy.integration.beans.TSIEvent;
import com.bmc.truesight.saas.remedy.integration.beans.Template;
import com.bmc.truesight.saas.remedy.integration.exception.ValidationException;
import com.bmc.truesight.saas.remedy.integration.util.Constants;
import com.bmc.truesight.saas.remedy.integration.util.StringUtil;

/**
 * Generic Template Validator for templates
 *
 * @author vitiwari
 *
 */
public class GenericTemplateValidator implements TemplateValidator {

    private static final Logger log = LoggerFactory.getLogger(GenericTemplateValidator.class);

    @Override
    public boolean validate(Template template) throws ValidationException {
        Configuration config = template.getConfig();
        TSIEvent payload = template.getEventDefinition();
        Map<String, FieldItem> fieldItemMap = template.getFieldItemMap();

        if (config.getRemedyHostName().isEmpty()
                || config.getRemedyUserName().isEmpty()
                || config.getTsiEventEndpoint().isEmpty()
                || config.getTsiApiToken().isEmpty()
                || (config.getConditionFields().size() == 0)
                || (config.getChunkSize() <= 0)
                || (config.getRetryConfig() < 0)
                || (config.getWaitMsBeforeRetry() <= 0)
                || (config.getStartDateTime() == null || (config.getStartDateTime() != null && StringUtils.isEmpty(config.getStartDateTime().toString())))
                || (config.getEndDateTime() == null || (config.getEndDateTime() != null && StringUtils.isEmpty(config.getEndDateTime().toString())))) {
            throw new ValidationException(StringUtil.format(Constants.CONFIG_VALIDATION_FAILED, new Object[]{}));
        }
        if (config.getStartDateTime().after(config.getEndDateTime())) {
            throw new ValidationException(StringUtil.format(Constants.DATERANGE_VALIDATION_FAILED, new Object[]{}));
        }
        if (config.getEndDateTime().after(new Date())) {
            throw new ValidationException(StringUtil.format(Constants.DATERANGE_VALIDATION_FAILED, new Object[]{}));
        }
        // validate payload configuration
        if (payload.getTitle() != null && payload.getTitle().startsWith("@") && !fieldItemMap.containsKey(payload.getTitle())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING,
                    new Object[]{payload.getTitle()}));
        }

        for (String fpField : payload.getFingerprintFields()) {
            if (fpField != null) {
                if (fpField.startsWith("@")) {
                    String field = fpField.substring(1);
                    if (!Constants.FINGERPRINT_EVENT_FIELDS.contains(field)) {
                        throw new ValidationException(StringUtil.format(Constants.EVENT_FIELD_MISSING, new Object[]{field, Constants.FINGERPRINT_EVENT_FIELDS}));
                    }
                } else {
                    if (!payload.getProperties().containsKey(fpField)) {
                        throw new ValidationException(StringUtil.format(Constants.EVENT_PROPERTY_FIELD_MISSING, new Object[]{fpField}));
                    }
                }
            }
        }

        Map<String, String> properties = payload.getProperties();
        if (properties.keySet().size() > Constants.MAX_PROPERTY_FIELD_SUPPORTED) {
            throw new ValidationException(StringUtil.format(Constants.PROPERTY_FIELD_COUNT_EXCEEDS, new Object[]{properties.keySet().size(), Constants.MAX_PROPERTY_FIELD_SUPPORTED}));
        }
        if (properties.containsKey("app_id")) {
            String appId = properties.get("app_id");
            if (!isValidAppId(appId)) {
                throw new ValidationException(StringUtil.format(Constants.APPLICATION_NAME_INVALID, new Object[]{appId}));
            }
        }
        for (String key : properties.keySet()) {
            if (!StringUtil.isValidJavaIdentifier(key)) {
                throw new ValidationException(StringUtil.format(Constants.PROPERTY_NAME_INVALID, new Object[]{key.trim()}));
            }
            if (properties.get(key).startsWith("@") && !fieldItemMap.containsKey(properties.get(key))) {
                throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{properties.get(key)}));
            }
        }

        if (payload.getSeverity() != null && payload.getSeverity().startsWith("@") && !fieldItemMap.containsKey(payload.getSeverity())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getSeverity()}));
        }

        if (payload.getStatus() != null && payload.getStatus().startsWith("@") && !fieldItemMap.containsKey(payload.getStatus())) {
            throw new ValidationException(
                    StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getStatus()}));
        }

        if (payload.getEventClass() != null && payload.getEventClass().startsWith("@") && !fieldItemMap.containsKey(payload.getEventClass())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getEventClass()}));
        }

        //valiadting source
        EventSource source = payload.getSource();
        if (source.getName() != null && source.getName().startsWith("@") && !fieldItemMap.containsKey(source.getName())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{source.getName()}));
        }
        if (source.getType() != null && source.getType().startsWith("@") && !fieldItemMap.containsKey(source.getType())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{source.getType()}));
        }
        if (source.getRef() != null && source.getRef().startsWith("@") && !fieldItemMap.containsKey(source.getRef())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{source.getRef()}));
        }

        EventSource sender = payload.getSender();
        if (sender.getName() != null && sender.getName().startsWith("@") && !fieldItemMap.containsKey(sender.getName())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{sender.getName()}));
        }
        if (sender.getType() != null && sender.getType().startsWith("@") && !fieldItemMap.containsKey(sender.getType())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{sender.getType()}));
        }
        if (sender.getRef() != null && sender.getRef().startsWith("@") && !fieldItemMap.containsKey(sender.getRef())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{sender.getRef()}));
        }
        return true;
    }

    private boolean isValidAppId(String inputString) {
        for (char c : inputString.toCharArray()) {
            if (Constants.SPECIAL_CHARACTOR.indexOf(c, 0) >= 0) {
                return false;
            }
        };
        return true;
    }
}
