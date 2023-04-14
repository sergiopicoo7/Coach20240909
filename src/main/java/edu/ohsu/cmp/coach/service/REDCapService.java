package edu.ohsu.cmp.coach.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import edu.ohsu.cmp.coach.exception.REDCapException;
import edu.ohsu.cmp.coach.http.HttpRequest;
import edu.ohsu.cmp.coach.http.HttpResponse;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class REDCapService extends AbstractService {
    private static final Pattern REDCAP_TOKEN_PATTERN = Pattern.compile("^[a-fA-F0-9]{32}$");

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String YES = "1";
    public static final String NO = "0";

    public static final String FORM_INCOMPLETE = "0";
    public static final String FORM_UNVERIFIED = "1";
    public static final String FORM_COMPLETE = "2";

    @Value("${redcap.api.url}")
    private String redcapApiUrl;

    @Value("${redcap.api.token}")
    private String redcapApiToken;

    @Value("${redcap.subject-info-form.name}")
    private String subjectInfoForm;

    @Value("${redcap.consent-form.name}")
    private String consentForm;

    @Value("${redcap.consent-form.consent-field-yn}")
    private String consentField;

    public boolean isRedcapEnabled() {
        return REDCAP_TOKEN_PATTERN.matcher(redcapApiToken).matches();
    }

    public boolean hasSubjectInfoRecord(String redcapId) throws EncoderException, REDCapException, IOException {
        String val = getValue(subjectInfoForm, redcapId, subjectInfoForm + "_complete");
        return StringUtils.equals(val, FORM_COMPLETE);
    }

    public boolean createSubjectInfoRecord(String redcapId) throws EncoderException, IOException, REDCapException {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("record_id", redcapId);
        map.put("redcap_event_name", subjectInfoForm);
        map.put(subjectInfoForm + "_complete", FORM_COMPLETE);

        List<Map<String, String>> list = new ArrayList<>();
        list.add(map);

        Gson gson = new Gson();
        String data = gson.toJson(list, new TypeToken<List<Map<String, String>>>(){}.getType());

        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("token", redcapApiToken);
        bodyParams.put("content", "record");
        bodyParams.put("format", "json");
        bodyParams.put("type", "flat");
        bodyParams.put("data", data);

        HttpResponse response = new HttpRequest().post(
                redcapApiUrl,
                null,
                requestHeaders,
                bodyParams
        );

        int code = response.getResponseCode();
        if (code < 200 || code > 299) {
            logger.error("REDCap ERROR: received code " + code + " - body=" + response.getResponseBody());
            throw new REDCapException("received code " + code + " from REDCap");

        } else {
            logger.debug("REDCap: received code " + code + " - body=" + response.getResponseBody());
            Map<String, String> record = gson.fromJson(response.getResponseBody(), new TypeToken<Map<String, String>>(){}.getType());

            return record.get("count").equals("1");
        }
    }

    public String getConsentSurveyLink(String redcapId) throws REDCapException, EncoderException, IOException {
        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("token", redcapApiToken);
        bodyParams.put("content", "surveyLink");
        bodyParams.put("record", redcapId);
        bodyParams.put("instrument", consentForm);

        HttpResponse response = new HttpRequest().post(
                redcapApiUrl,
                null,
                requestHeaders,
                bodyParams
        );

        int code = response.getResponseCode();
        if (code < 200 || code > 299) {
            logger.error("REDCap ERROR: received code " + code + " - body=" + response.getResponseBody());
            throw new REDCapException("received code " + code + " from REDCap");

        } else {
            logger.debug("REDCap: received code " + code + " - body=" + response.getResponseBody());
            return response.getResponseBody();
        }
    }

    public boolean hasConsentRecord(String redcapId) throws EncoderException, REDCapException, IOException {
        String val = getValue(consentForm, redcapId, consentForm + "_complete");
        return StringUtils.equals(val, FORM_COMPLETE);
    }

    public boolean isConsentGranted(String redcapId) throws EncoderException, REDCapException, IOException {
        String val = getValue(consentForm, redcapId, consentField);
        return StringUtils.equals(val, YES);
    }

    private String getValue(String form, String redcapId, String field) throws EncoderException, IOException, REDCapException {
        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("token", redcapApiToken);
        bodyParams.put("content", "record");
        bodyParams.put("format", "json");
        bodyParams.put("type", "flat");
        bodyParams.put("rawOrLabel", "raw");
        bodyParams.put("forms", form);
        bodyParams.put("records", redcapId);

        HttpResponse response = new HttpRequest().post(
                redcapApiUrl,
                null,
                requestHeaders,
                bodyParams
        );

        int code = response.getResponseCode();
        if (code < 200 || code > 299) {
            logger.error("REDCap ERROR: received code " + code + " - body=" + response.getResponseBody());
            throw new REDCapException("received code " + code + " from REDCap");

        } else {
            logger.debug("REDCap: received code " + code + " - body=" + response.getResponseBody());
            Gson gson = new Gson();
            List<Map<String, String>> records = gson.fromJson(response.getResponseBody(), new TypeToken<List<Map<String, String>>>(){}.getType());

            if (records.isEmpty()) {
                return null;
            } else if (records.size() == 1) {
                return records.get(0).get(field);
            } else {
                throw new REDCapException("found too many records for (" + form + ", " + redcapId + ") - expected 1, found " + records.size());
            }
        }
    }
}
