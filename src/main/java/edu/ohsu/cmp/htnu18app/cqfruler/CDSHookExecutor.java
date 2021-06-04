package edu.ohsu.cmp.htnu18app.cqfruler;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.cqfruler.model.*;
import edu.ohsu.cmp.htnu18app.entity.app.HomeBloodPressureReading;
import edu.ohsu.cmp.htnu18app.http.HttpRequest;
import edu.ohsu.cmp.htnu18app.http.HttpResponse;
import edu.ohsu.cmp.htnu18app.model.BloodPressureModel;
import edu.ohsu.cmp.htnu18app.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.htnu18app.model.recommendation.Audience;
import edu.ohsu.cmp.htnu18app.model.recommendation.Card;
import edu.ohsu.cmp.htnu18app.service.HomeBloodPressureReadingService;
import edu.ohsu.cmp.htnu18app.service.PatientService;
import edu.ohsu.cmp.htnu18app.util.MustacheUtil;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

public class CDSHookExecutor implements Runnable {
    private static final boolean TESTING = true; // true: use hard-coded 'canned' responses from CQF Ruler (fast, cheap)
                                                  // false: make CQF Ruler calls (slow, expensive)

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String sessionId;
    private String cdsHooksEndpointURL;
    private PatientService patientService;
    private HomeBloodPressureReadingService hbprService;

    public CDSHookExecutor(String sessionId, String cdsHooksEndpointURL,
                           PatientService patientService,
                           HomeBloodPressureReadingService hbprService) {
        this.sessionId = sessionId;
        this.cdsHooksEndpointURL = cdsHooksEndpointURL;
        this.patientService = patientService;
        this.hbprService = hbprService;
    }

    @Override
    public String toString() {
        return "CDSHookExecutor{" +
                "sessionId='" + sessionId + '\'' +
                ", cdsHooksEndpointURL='" + cdsHooksEndpointURL + '\'' +
                ", patientService=" + patientService +
                ", hbprService=" + hbprService +
                '}';
    }

    @Override
    public void run() {
        CacheData cache = SessionCache.getInstance().get(sessionId);

        cache.deleteAllCards();

        List<CDSHook> hooks = null;
        try {
            hooks = getCDSHooks();

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " getting CDS Hooks - " + e.getMessage(), e);
        }

        if (hooks != null) {
            for (CDSHook hook : hooks) {
                try {
                    List<Card> cards = getCardsForHook(sessionId, hook.getId(),
                            cache.getFhirCredentialsWithClient(),
                            cache.getAudience());

                    cache.setCards(hook.getId(), cards);

                    logger.info("cards generated for sessionId=" + sessionId + ", hookId=" + hook.getId());

                } catch (Exception e) {
                    logger.error("caught " + e.getClass().getName() + " executing hook '" + hook.getId() + "' - " + e.getMessage(), e);
                }
            }
        }
    }

////////////////////////////////////////////////////////////////////////
// private methods
//
    private List<CDSHook> getCDSHooks() throws IOException {
        logger.info("getting " + cdsHooksEndpointURL);

        String json;
        if (TESTING) {
            json = "{  \"services\": [    {      \"hook\": \"patient-view\",      \"name\": \"Hypertension\",      \"title\": \"OHSU Hypertension\",      \"description\": \"This PlanDefinition identifies hypertension\",      \"id\": \"plandefinition-Hypertension\",      \"prefetch\": {        \"item1\": \"Patient?_id\\u003d{{context.patientId}}\",        \"item2\": \"Observation?subject\\u003dPatient/{{context.patientId}}\\u0026code\\u003dhttp://loinc.org|55284-4\",        \"item3\": \"Encounter?patient\\u003dPatient/{{context.patientId}}\",        \"item4\": \"Condition?patient\\u003dPatient/{{context.patientId}}\\u0026code\\u003dhttp://snomed.info/sct|111438007,http://snomed.info/sct|123799005,http://snomed.info/sct|123800009,http://snomed.info/sct|14973001,http://snomed.info/sct|169465000,http://snomed.info/sct|194783001,http://snomed.info/sct|194785008,http://snomed.info/sct|194788005,http://snomed.info/sct|194791005,http://snomed.info/sct|199008003,http://snomed.info/sct|26078007,http://snomed.info/sct|28119000,http://snomed.info/sct|31992008,http://snomed.info/sct|39018007,http://snomed.info/sct|39727004,http://snomed.info/sct|427889009,http://snomed.info/sct|428575007,http://snomed.info/sct|48552006,http://snomed.info/sct|57684003,http://snomed.info/sct|73410007,http://snomed.info/sct|74451002,http://snomed.info/sct|89242004\",        \"item5\": \"Procedure?patient\\u003dPatient/{{context.patientId}}\\u0026code\\u003dhttp://snomed.info/sct|164783007,http://snomed.info/sct|413153004,http://snomed.info/sct|448489007,http://snomed.info/sct|448678005,http://www.ama-assn.org/go/cpt|93784,http://www.ama-assn.org/go/cpt|93786,http://www.ama-assn.org/go/cpt|93788,http://www.ama-assn.org/go/cpt|93790\"      }    }  ]}\n";

        } else {
            HttpResponse response = new HttpRequest().get(cdsHooksEndpointURL);
            json = response.getResponseBody();
        }

        Gson gson = new GsonBuilder().create();
        CDSServices services = gson.fromJson(json, new TypeToken<CDSServices>(){}.getType());

        return services.getHooks();
    }

    private List<Card> getCardsForHook(String sessionId, String hookId,
                                       FHIRCredentialsWithClient fcc, Audience audience) throws IOException {

        List<Card> cards = new ArrayList<Card>();

        try {
            Patient p = patientService.getPatient(sessionId);

            Bundle bpBundle = patientService.getBloodPressureObservations(sessionId);

            // inject home blood pressure readings into Bundle for evaluation by CQF Ruler
            List<HomeBloodPressureReading> hbprList = hbprService.getHomeBloodPressureReadings(sessionId);
            for (HomeBloodPressureReading item : hbprList) {
                String uuid = UUID.randomUUID().toString();

                Encounter e = buildEncounter(uuid, p.getId(), item.getReadingDate());
                bpBundle.addEntry().setFullUrl("http://hl7.org/fhir/Encounter/" + e.getId()).setResource(e);

                Observation o = buildBloodPressureObservation(uuid, p.getId(), e.getId(), item);

                // todo: should the URL be different?
                bpBundle.addEntry().setFullUrl("http://hl7.org/fhir/Observation/" + o.getId()).setResource(o);
            }

            HookRequest request = new HookRequest(fcc.getCredentials(), p, bpBundle);

            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile("cqfruler/hookRequest.mustache");
            StringWriter writer = new StringWriter();
            mustache.execute(writer, request).flush();

            logger.info("hookRequest = " + writer.toString());

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Content-Type", "application/json; charset=UTF-8");

            String json;
            if (TESTING) {
                //                json = "{ \"cards\": [ { \"summary\": \"Hypertension Diagnosis\", \"indicator\": \"info\", \"detail\": \"ConsiderHTNStage1 Patient\", \"source\": { \"label\": \"Info for those with normal blood pressure\", \"url\": \"https://en.wikipedia.org/wiki/Blood_pressure\" } }, { \"summary\": \"Recommend diagnosis of Stage 2 hypertension\", \"indicator\": \"warning\", \"detail\": \"{{#patient}}Patient rationale{{/patient}}{{#careTeam}}care team rationale{{/careTeam}}|{{#patient}}https://source.com/patient{{/patient}}{{#careTeam}}https://source.com/careTeam{{/careTeam}}|[ { \\\"label\\\": \\\"Enter Blood Pressure\\\", \\\"actions\\\": [ \\\"ServiceRequest for High Blood Pressure Monitoring\\\", \\\"{{#patient}}<a href='/bp-readings'>Click here to go to the Home Blood Pressure entry page</a>{{/patient}}\\\" ] }, { \\\"label\\\": \\\"Diet\\\", \\\"actions\\\": [ \\\"Put the fork down!\\\", \\\"{{#patient}}<input type='checkbox' class='goal' data-goalid='dashDiet' /><label>Try the DASH Diet</label>{{/patient}}\\\" ] } ]|at-most-one|<ol>{{#patient}}<li>https://links.com/patient</li>{{/patient}}<li>https://links.com/careTeamAndPatient</li></ol>\", \"source\": {} } ] }";
                json = "{ \"cards\": [ { \"summary\": \"Hypertension Diagnosis\", \"indicator\": \"info\", \"detail\": \"ConsiderHTNStage1 Patient\", \"source\": { \"label\": \"Info for those with normal blood pressure\", \"url\": \"https://en.wikipedia.org/wiki/Blood_pressure\" } }, { \"summary\": \"Recommend diagnosis of Stage 2 hypertension\", \"indicator\": \"warning\", \"detail\": \"{{#patient}}Patient rationale{{/patient}}{{#careTeam}}care team rationale{{/careTeam}}|{{#patient}}https://source.com/patient{{/patient}}{{#careTeam}}https://source.com/careTeam{{/careTeam}}|[ { \\\"label\\\": \\\"Enter Blood Pressure\\\", \\\"actions\\\": [ \\\"ServiceRequest for High Blood Pressure Monitoring\\\", \\\"{{#patient}}<a href='/bp-readings'>Click here to go to the Home Blood Pressure entry page</a>{{/patient}}\\\" ] }, { \\\"label\\\": \\\"Diet\\\", \\\"actions\\\": [ \\\"Put the fork down!\\\", \\\"{{#patient}}<input type='checkbox' class='goal' data-goalid='dashDiet' /><label>Try the DASH Diet</label>{{/patient}}\\\" ] } ]|at-most-one|[{{#patient}}{ \\\"label\\\": \\\"Patient Link\\\", \\\"url\\\": \\\"https://links.com/patient\\\" },{{/patient}} { \\\"label\\\": \\\"Care Team and Patient Link\\\", \\\"url\\\": \\\"https://links.com/careTeamAndPatient\\\" }]\", \"source\": {} } ] }";

            } else {
                HttpResponse httpResponse = new HttpRequest().post(cdsHooksEndpointURL + "/" + hookId, null, headers, writer.toString());
                json = httpResponse.getResponseBody();
            }

            Gson gson = new GsonBuilder().create();
            try {
                json = MustacheUtil.compileMustache(audience, json);
                CDSHookResponse response = gson.fromJson(json, new TypeToken<CDSHookResponse>() {}.getType());

                for (CDSCard cdsCard : response.getCards()) {
                    cards.add(new Card(cdsCard));
                }

            } catch (Exception e) {
                logger.error("caught " + e.getClass().getName() + " processing response for hookId=" + hookId + " - " + e.getMessage(), e);
                logger.error("\n\nJSON =\n" + json + "\n\n");
                throw e;
            }

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " processing hookId=" + hookId + " - " + e.getMessage(), e);

            if (e instanceof IOException) {
                throw (IOException) e;
            }
        }

        return cards;
    }

    private Encounter buildEncounter(String uuid, String patientId, Date date) {
        Encounter e = new Encounter();

        e.setId("encounter-" + uuid);
        e.setStatus(Encounter.EncounterStatus.FINISHED);
        e.getClass_().setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode").setCode("AMB").setDisplay("ambulatory");

        e.setSubject(new Reference().setReference(patientId));

        Calendar start = Calendar.getInstance();
        start.setTime(date);
        start.add(Calendar.MINUTE, -1);

        Calendar end = Calendar.getInstance();
        end.setTime(date);
        end.add(Calendar.MINUTE, 1);

        e.getPeriod().setStart(start.getTime()).setEnd(end.getTime());

        return e;
    }

    private Observation buildBloodPressureObservation(String uuid, String patientId, String encounterId, HomeBloodPressureReading item) {
        // adapted from https://www.programcreek.com/java-api-examples/?api=org.hl7.fhir.dstu3.model.Observation

        Observation o = new Observation();

        o.setId("observation-bp-" + uuid);
        o.setSubject(new Reference().setReference(patientId));
        o.setEncounter(new Reference().setReference(encounterId));
        o.setStatus(Observation.ObservationStatus.FINAL);
        o.getCode().addCoding().setCode(BloodPressureModel.CODE).setSystem(BloodPressureModel.SYSTEM);
        o.setEffective(new DateTimeType(item.getReadingDate()));

        Observation.ObservationComponentComponent systolic = new Observation.ObservationComponentComponent();
        systolic.getCode().addCoding().setCode(BloodPressureModel.SYSTOLIC_CODE).setSystem(BloodPressureModel.SYSTEM);
        systolic.setValue(new Quantity());
        systolic.getValueQuantity().setCode(BloodPressureModel.VALUE_CODE);
        systolic.getValueQuantity().setSystem(BloodPressureModel.VALUE_SYSTEM);
        systolic.getValueQuantity().setUnit(BloodPressureModel.VALUE_UNIT);
        systolic.getValueQuantity().setValue(item.getSystolic());
        o.getComponent().add(systolic);

        Observation.ObservationComponentComponent diastolic = new Observation.ObservationComponentComponent();
        diastolic.getCode().addCoding().setCode(BloodPressureModel.DIASTOLIC_CODE).setSystem(BloodPressureModel.SYSTEM);
        diastolic.setValue(new Quantity());
        diastolic.getValueQuantity().setCode(BloodPressureModel.VALUE_CODE);
        diastolic.getValueQuantity().setSystem(BloodPressureModel.VALUE_SYSTEM);
        diastolic.getValueQuantity().setUnit(BloodPressureModel.VALUE_UNIT);
        diastolic.getValueQuantity().setValue(item.getDiastolic());
        o.getComponent().add(diastolic);

        return o;
    }
}