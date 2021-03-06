package com.bmc.truesight.saas.remedy.integration.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.thirdparty.org.apache.commons.codec.binary.Base64;
import com.bmc.truesight.saas.remedy.integration.BulkEventHttpClient;
import com.bmc.truesight.saas.remedy.integration.beans.Configuration;
import com.bmc.truesight.saas.remedy.integration.beans.Error;
import com.bmc.truesight.saas.remedy.integration.beans.Result;
import com.bmc.truesight.saas.remedy.integration.beans.Success;
import com.bmc.truesight.saas.remedy.integration.beans.TSIEvent;
import com.bmc.truesight.saas.remedy.integration.beans.TSIEventResponse;
import com.bmc.truesight.saas.remedy.integration.exception.BulkEventsIngestionFailedException;
import com.bmc.truesight.saas.remedy.integration.exception.TsiAuthenticationFailedException;
import com.bmc.truesight.saas.remedy.integration.util.Constants;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CallableBulkEventHttpClient implements Callable<Result>, BulkEventHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(CallableBulkEventHttpClient.class);

    public CallableBulkEventHttpClient(List<TSIEvent> eventList, Configuration configuration) {
        this.eventList = eventList;
        this.configuration = configuration;
    }

    private List<TSIEvent> eventList;
    private Configuration configuration;

    @Override
    public Result call() throws TsiAuthenticationFailedException {
        Result result = null;
        try {
            result = pushBulkEventsToTSI(eventList);
        } catch (BulkEventsIngestionFailedException e) {
            result = new Result();
            List<Error> errorList = new ArrayList<>();
            for (int i = 0; i < eventList.size(); i++) {
                Error error = new Error(i, e.getMessage());
                errorList.add(error);
            }
            result.setErrors(errorList);
            result.setSent(0);
            result.setSuccess(Success.FALSE);
        }
        return result;
    }

    private TSIEventResponse getResponseFromInputStream(InputStream instream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
        ObjectMapper mapper = new ObjectMapper();
        TSIEventResponse resp = null;
        try {
            resp = mapper.readValue(reader, TSIEventResponse.class);
        } catch (JsonParseException e1) {
            LOG.error("Response Json parsing failed,{}", e1.getMessage());
        } catch (JsonMappingException e1) {
            LOG.error("Response Json mapping failed,{}", e1.getMessage());
        } catch (IOException e1) {
            LOG.error(e1.getMessage());
        } finally {
            try {
                instream.close();
            } catch (IOException e) {
                LOG.debug("Exception in closing the input stream, {}", e.getMessage());
            }
        }
        return resp;
    }

    public static String encodeBase64(final String encodeToken) {
        byte[] encoded = Base64.encodeBase64(encodeToken.getBytes());
        return new String(encoded);
    }

    @Override
    public Result pushBulkEventsToTSI(List<TSIEvent> bulkEvents) throws BulkEventsIngestionFailedException, TsiAuthenticationFailedException {
        if (bulkEvents.size() <= 0) {
            throw new BulkEventsIngestionFailedException("Cannot send empty events list to TSI");
        }
        Result result = null;
        HttpClient httpClient = null;
        boolean isSuccessful = false;
        int retryCount = 0;

        ObjectMapper mapper = new ObjectMapper();

        while (!isSuccessful && retryCount <= this.configuration.getRetryConfig()) {
            String jsonInString = null;
            httpClient = HttpClientBuilder.create().build();
            HttpPost httpPost = new HttpPost(this.configuration.getTsiEventEndpoint());
            httpPost.addHeader("Authorization", "Basic " + encodeBase64("" + ":" + this.configuration.getTsiApiToken()));
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("accept", "application/json");
            httpPost.addHeader("User-Agent", "RemedyScript");
            try {
                jsonInString = mapper.writeValueAsString(bulkEvents);
                Charset charsetD = Charset.forName("UTF-8");
                StringEntity postingString = new StringEntity(jsonInString, charsetD);
                LOG.debug("Starting ingestion of {} events  to TSI with payload size as {} bytes", bulkEvents.size(), jsonInString.getBytes("UTF-8").length);
                httpPost.setEntity(postingString);

            } catch (Exception e) {
                LOG.debug("Can not Send events, There is an issue in creating http request data [{}]", e.getMessage());
                throw new BulkEventsIngestionFailedException(e.getMessage());
            }
            HttpResponse response;
            try {
                response = httpClient.execute(httpPost);
            } catch (Exception e) {
                LOG.debug("Sending Event resulted into an exception [{}]", e.getMessage());
                if (retryCount < this.configuration.getRetryConfig()) {
                    retryCount++;
                    try {
                        LOG.debug("[Retry  {} ], Waiting for {} sec before trying again ......", retryCount, (this.configuration.getWaitMsBeforeRetry() / 1000));
                        Thread.sleep(this.configuration.getWaitMsBeforeRetry());
                    } catch (InterruptedException e1) {
                        LOG.debug("Thread interrupted ......{}", e1.getMessage());
                    }
                    continue;
                } else {
                    throw new BulkEventsIngestionFailedException(e.getMessage());
                }
            }

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == Constants.UNAUTHORIZED_STATUS) {
                throw new TsiAuthenticationFailedException("TSI authentication failed, please verify the API Token or API Endpoint");
            } else if (statusCode != Constants.EVENT_INGESTION_STATE_SUCCESS && statusCode != Constants.EVENT_INGESTION_STATE_ACCEPTED) {
                if (retryCount < this.configuration.getRetryConfig()) {
                    retryCount++;
                    LOG.debug("Sending Event did not result in success, response status Code : {} , {}", new Object[]{response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase()});
                    try {
                        LOG.debug("[Retry  {} ], Waiting for {} sec before trying again ......", retryCount, (this.configuration.getWaitMsBeforeRetry() / 1000));
                        Thread.sleep(this.configuration.getWaitMsBeforeRetry());
                    } catch (InterruptedException e1) {
                        LOG.debug("Thread interrupted ......{}", e1.getMessage());
                    }
                    continue;
                } else {
                    throw new BulkEventsIngestionFailedException("Sending Event to TSI did not result in success, response status Code :" + response.getStatusLine().getStatusCode() + "," + response.getStatusLine().getReasonPhrase());
                }
            } else {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream instream = null;

                    try {
                        instream = entity.getContent();
                    } catch (UnsupportedOperationException e) {
                        LOG.error("Getting Response input stream failed {}", e.getMessage());
                    } catch (IOException e) {
                        LOG.error("Getting Response input stream failed {}", e.getMessage());
                    }
                    TSIEventResponse eventResponse = getResponseFromInputStream(instream);
                    if (eventResponse == null) {
                        LOG.debug("Event Response is null, returning result as Null");
                    }
                    result = eventResponse.getResult();
                    if (result.getAccepted() != null) {
                        LOG.debug("Response from event ingestion API Sent:{},successful:{},error:{}", result.getSent(), result.getAccepted() != null ? result.getAccepted().size() : 0, result.getErrors() != null ? result.getErrors().size() : 0);
                    }
                    isSuccessful = true;
                }
            }
        }
        return result;

    }

}
