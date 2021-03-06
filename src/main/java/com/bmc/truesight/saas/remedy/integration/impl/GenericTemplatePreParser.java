package com.bmc.truesight.saas.remedy.integration.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.truesight.saas.remedy.integration.ARServerForm;
import com.bmc.truesight.saas.remedy.integration.TemplatePreParser;
import com.bmc.truesight.saas.remedy.integration.beans.Configuration;
import com.bmc.truesight.saas.remedy.integration.beans.FieldItem;
import com.bmc.truesight.saas.remedy.integration.beans.TSIEvent;
import com.bmc.truesight.saas.remedy.integration.beans.Template;
import com.bmc.truesight.saas.remedy.integration.exception.ParsingException;
import com.bmc.truesight.saas.remedy.integration.util.Constants;
import com.bmc.truesight.saas.remedy.integration.util.StringUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 * This class helps in preParsing the default master configurations and return
 * as a {@link Template} object.
 *
 * @author vitiwari
 *
 */
public class GenericTemplatePreParser implements TemplatePreParser {

    private static final Logger log = LoggerFactory.getLogger(GenericTemplatePreParser.class);

    private static final String INCIDENT_CONFIG_FILE = "incidentDefaultTemplate.json";
    private static final String CHANGE_CONFIG_FILE = "changeDefaultTemplate.json";

    @Override
    public Template loadDefaults(ARServerForm form) throws ParsingException {
        String fileName = "";
        switch (form) {
            case INCIDENT_FORM:
                fileName = INCIDENT_CONFIG_FILE;
                break;
            case CHANGE_FORM:
                fileName = CHANGE_CONFIG_FILE;
        }
        Template template = new Template();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = null;
        try {
            String configJson = getFile(fileName);
            rootNode = mapper.readTree(configJson);
        } catch (IOException e) {
            throw new ParsingException(StringUtil.format(Constants.CONFIG_FILE_NOT_VALID, new Object[]{e.getMessage()}));
        }

        Configuration config = new Configuration();
        try {
            JsonNode configuration = rootNode.get(Constants.CONFIG_NODE_NAME);
            if (configuration != null) {
                JsonNode hostNode = configuration.get(Constants.CONFIG_HOSTNAME_NODE_NAME);
                if (hostNode != null) {
                    config.setRemedyHostName(hostNode.asText());
                }

                JsonNode portNode = configuration.get(Constants.CONFIG_PORT_NODE_NAME);
                if (portNode != null) {
                    try {
                        Integer port = Integer.parseInt(portNode.asText());
                        config.setRemedyPort(port);
                    } catch (NumberFormatException ex) {
                        log.debug("default port is not a valid port, skipping the port setting");
                    }
                }

                JsonNode userNode = configuration.get(Constants.CONFIG_USERNAME_NODE_NAME);
                if (userNode != null) {
                    config.setRemedyUserName(userNode.asText());
                }

                JsonNode passNode = configuration.get(Constants.CONFIG_PASSWORD_NODE_NAME);
                if (passNode != null) {
                    config.setRemedyPassword(passNode.asText());
                }

                JsonNode tsiEndNode = configuration.get(Constants.CONFIG_TSIENDPOINT_NODE_NAME);
                if (tsiEndNode != null) {
                    config.setTsiEventEndpoint(tsiEndNode.asText());
                }

                JsonNode tsiKeyNode = configuration.get(Constants.CONFIG_TSITOKEN_NODE_NAME);
                if (tsiKeyNode != null) {
                    config.setTsiApiToken(tsiKeyNode.asText());
                }

                JsonNode chunkNode = configuration.get(Constants.CONFIG_CHUNKSIZE_NODE_NAME);
                if (chunkNode != null) {
                    Integer chunk = Integer.parseInt(chunkNode.asText());
                    config.setChunkSize(chunk);
                }
                
                JsonNode retryChunkNode = configuration.get(Constants.CONFIG_RETRYCHUNKSIZE_NODE_NAME);
                if (retryChunkNode != null) {
                    Integer chunk = Integer.parseInt(retryChunkNode.asText());
                    config.setRetryChunkSize(chunk);
                }

                JsonNode threadsNode = configuration.get(Constants.CONFIG_THREADS_NODE_NAME);
                if (threadsNode != null) {
                    Integer thread = Integer.parseInt(threadsNode.asText());
                    config.setThreadCount(thread);
                }

                ObjectReader obReader = mapper.reader(new TypeReference<List<Integer>>() {
                });
                JsonNode condFields = configuration.get(Constants.CONFIG_CONDFIELDS_NODE_NAME);
                if (condFields != null) {
                    List<Integer> condList = obReader.readValue(condFields);
                    config.setConditionFields(condList);
                }

                JsonNode statusFields = configuration.get(Constants.CONFIG_CONDSTATUSFIELDS_NODE_NAME);
                if (statusFields != null) {
                    List<Integer> condList = obReader.readValue(statusFields);
                    config.setQueryStatusList(condList);
                }

                JsonNode retryNode = configuration.get(Constants.CONFIG_RETRY_NODE_NAME);
                if (retryNode != null) {
                    config.setRetryConfig(Integer.valueOf(retryNode.asInt()));
                }

                JsonNode waitMsNode = configuration.get(Constants.CONFIG_WAITSMS_NODE_NAME);
                if (waitMsNode != null) {
                    config.setWaitMsBeforeRetry(Integer.valueOf(waitMsNode.asInt()));
                }
            }
            template.setConfig(config);
        } catch (IOException e) {
            throw new ParsingException(StringUtil.format(Constants.CONFIG_PROPERTY_NOT_FOUND, new Object[]{e.getMessage()}));
        }

        // Read the payload details and map to pojo
        try {
            JsonNode payloadNode = rootNode.get(Constants.EVENTDEF_NODE_NAME);
            String payloadString = mapper.writeValueAsString(payloadNode);
            TSIEvent event = mapper.readValue(payloadString, TSIEvent.class);
            template.setEventDefinition(event);
        } catch (IOException e) {
            throw new ParsingException(StringUtil.format(Constants.PAYLOAD_PROPERTY_NOT_FOUND, new Object[]{}));
        }

        // Mapping of fieldDefinitionMap
        try {
            JsonNode fieldDefinitionNode = rootNode.get(Constants.FIELDDEFINITIONMAP_NODE_NAME);
            String fieldDefinitionString = mapper.writeValueAsString(fieldDefinitionNode);
            TypeReference<HashMap<String, FieldItem>> typeRef = new TypeReference<HashMap<String, FieldItem>>() {
            };
            Map<String, FieldItem> fieldDefinitionMap = mapper.readValue(fieldDefinitionString, typeRef);
            template.setFieldDefinitionMap(fieldDefinitionMap);
        } catch (IOException e) {
            throw new ParsingException(StringUtil.format(Constants.PAYLOAD_PROPERTY_NOT_FOUND, new Object[]{}));
        }

        return template;
    }

    private String getFile(String fileName) throws IOException {
        //Get file from resources folder
        InputStream inputStream = ClassLoader.getSystemResourceAsStream(fileName);
        InputStreamReader streamReader = new InputStreamReader(inputStream, "UTF-8");
        BufferedReader in = new BufferedReader(streamReader);
        StringBuffer text = new StringBuffer("");
        for (String line; (line = in.readLine()) != null;) {
            text = text.append(line);
        }
        return text.toString();
    }

}
