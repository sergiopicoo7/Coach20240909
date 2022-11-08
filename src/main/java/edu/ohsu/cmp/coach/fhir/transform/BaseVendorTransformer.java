package edu.ohsu.cmp.coach.fhir.transform;

import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.*;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class BaseVendorTransformer implements VendorTransformer {
    protected static final String NO_ENCOUNTERS_KEY = null; // intentionally instantiated with null value

    public static final String OBSERVATION_CATEGORY_SYSTEM = "http://terminology.hl7.org/CodeSystem/observation-category";
    public static final String OBSERVATION_CATEGORY_CODE = "vital-signs";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected UserWorkspace workspace;

    public BaseVendorTransformer(UserWorkspace workspace) {
        this.workspace = workspace;
    }


    protected abstract BloodPressureModel buildBloodPressureModel(Encounter encounter, Observation bpObservation, Observation protocolObservation, FhirConfigManager fcm) throws DataException;
    protected abstract BloodPressureModel buildBloodPressureModel(Observation o, FhirConfigManager fcm) throws DataException;
    protected abstract BloodPressureModel buildBloodPressureModel(Observation systolicObservation, Observation diastolicObservation, FhirConfigManager fcm) throws DataException;

    @Override
    public List<BloodPressureModel> transformIncomingBloodPressureReadings(Bundle bundle) throws DataException {
        if (bundle == null) return null;

        Map<String, List<Observation>> encounterObservationsMap = buildEncounterObservationsMap(bundle);
        FhirConfigManager fcm = workspace.getFhirConfigManager();
        List<Coding> bpCodings = fcm.getAllBpCodings();

        List<BloodPressureModel> list = new ArrayList<>();

        for (Encounter encounter : getAllEncounters(bundle)) {
            logger.debug("processing Encounter: " + encounter.getId());

            List<Observation> encounterObservations = getObservationsFromMap(encounter, encounterObservationsMap);

            if (encounterObservations != null) {
                logger.debug("building Observations for Encounter " + encounter.getId());

                List<Observation> bpObservationList = new ArrayList<>();    // potentially many per encounter
                Observation protocolObservation = null;

                Iterator<Observation> iter = encounterObservations.iterator();
                while (iter.hasNext()) {
                    Observation o = iter.next();
                    if (o.hasCode() && FhirUtil.hasCoding(o.getCode(), bpCodings)) {
                        logger.debug("bpObservation = " + o.getId() + " (encounter=" + encounter.getId() +
                                ") (effectiveDateTime=" + o.getEffectiveDateTimeType().getValueAsString() + ")");
                        bpObservationList.add(o);
                        iter.remove();

                    } else if (protocolObservation == null && FhirUtil.hasCoding(o.getCode(), fcm.getProtocolCoding())) {
                        logger.debug("protocolObservation = " + o.getId() + " (encounter=" + encounter.getId() +
                                ") (effectiveDateTime=" + o.getEffectiveDateTimeType().getValueAsString() + ")");
                        protocolObservation = o;
                        iter.remove();
                    }
                }

                for (Observation bpObservation : bpObservationList) {
                    list.add(buildBloodPressureModel(encounter, bpObservation, protocolObservation, fcm));
                }

                bpObservationList.clear();

            } else {
                logger.debug("no Observations found for Encounter " + encounter.getId());
            }
        }

        // there may be BP observations in the system that aren't tied to any encounters.  we still want to capture these
        // of course, we can't associate any other observations with them (e.g. protocol), but whatever.  better than nothing

        // these observations without Encounters that also have identical timestamps are presumed to be related.
        // these need to be combined into a single BloodPresureModel object for any pair of (systolic, diastolic) that
        // have the same timestamp

        List<Coding> systolicCodings = fcm.getSystolicCodings();
        List<Coding> diastolicCodings = fcm.getDiastolicCodings();
        List<Coding> bpPanelCodings = fcm.getBpPanelCodings();

        Map<String, List<Observation>> dateObservationsMap = new LinkedHashMap<>();

        for (Map.Entry<String, List<Observation>> entry : encounterObservationsMap.entrySet()) {
            if (entry.getValue() != null) {
                for (Observation o : entry.getValue()) {
                    if (o.hasCode()) {
                        if (FhirUtil.hasCoding(o.getCode(), bpPanelCodings)) {
                            logger.debug("bpObservation = " + o.getId() + " (no encounter) (effectiveDateTime=" +
                                    o.getEffectiveDateTimeType().getValueAsString() + ")");
                            list.add(buildBloodPressureModel(o, fcm));

                        } else if (FhirUtil.hasCoding(o.getCode(), systolicCodings) || FhirUtil.hasCoding(o.getCode(), diastolicCodings)) {
                            String dateStr = o.getEffectiveDateTimeType().getValueAsString();
                            if ( ! dateObservationsMap.containsKey(dateStr) ) {
                                dateObservationsMap.put(dateStr, new ArrayList<>());
                            }
                            dateObservationsMap.get(dateStr).add(o);

                        } else {
                            logger.debug("did not process Observation " + o.getId());
                        }
                    }
                }
            }
        }

        // now process dateObservationsMap, which should only include individual systolic and diastolic readings

        for (Map.Entry<String, List<Observation>> entry : dateObservationsMap.entrySet()) {
            List<Observation> list2 = entry.getValue();

            if (list2.size() == 2) { // probably both systolic and diastolic, but check for sure
                Observation o1 = list2.get(0);
                Observation o2 = list2.get(1);

                Observation systolicObservation;
                Observation diastolicObservation;

                if (o1.hasCode() && FhirUtil.hasCoding(o1.getCode(), systolicCodings) &&
                        o2.hasCode() && FhirUtil.hasCoding(o2.getCode(), diastolicCodings)) {
                    systolicObservation = o1;
                    diastolicObservation = o2;

                } else if (o1.hasCode() && FhirUtil.hasCoding(o1.getCode(), diastolicCodings) &&
                        o2.hasCode() && FhirUtil.hasCoding(o2.getCode(), systolicCodings)) {
                    systolicObservation = o2;
                    diastolicObservation = o1;

                } else {
                    logger.warn("unexpected Observation pair building BloodPressureModel for ids=[" +
                            o1.getId() + ", " + o2.getId() + "] - skipping -");
                    continue;
                }

                logger.debug("systolicObservation = " + systolicObservation.getId() + " (effectiveDateTime=" +
                        systolicObservation.getEffectiveDateTimeType().getValueAsString() + ")");
                logger.debug("diastolicObservation = " + diastolicObservation.getId() + " (effectiveDateTime=" +
                        diastolicObservation.getEffectiveDateTimeType().getValueAsString() + ")");

                list.add(buildBloodPressureModel(systolicObservation, diastolicObservation, fcm));

            } else {                        // more readings than expected, handle somehow?
                logger.warn("expected 2 observations but encountered " + list2.size() +
                        " for readingDate=" + entry.getKey() + " - skipping -");
            }
        }

        return list;
    }

    protected Map<String, List<Observation>> buildEncounterObservationsMap(Bundle bundle) {
        Map<String, List<Observation>> map = new HashMap<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Observation) {
                    Observation observation = (Observation) entry.getResource();
                    if (observation.hasEncounter()) {
                        List<String> keys = buildKeys(observation.getEncounter());

                        // we want to associate THE SAME list with each key, NOT separate instances of identical lists

                        List<Observation> list = null;
                        for (String key : keys) {
                            if (map.containsKey(key)) {
                                list = map.get(key);
                                break;
                            }
                        }
                        if (list == null) {
                            list = new ArrayList<>();
                            for (String key : keys) {
                                map.put(key, list);
                            }
                        }

                        map.get(keys.get(0)).add(observation);

                    } else {
                        List<Observation> list = map.get(NO_ENCOUNTERS_KEY);
                        if (list == null) {
                            list = new ArrayList<>();
                            map.put(NO_ENCOUNTERS_KEY, list);
                        }
                        list.add(observation);
                    }
                }
            }
        }
        return map;
    }

    protected List<Observation> getObservationsFromMap(Encounter encounter, Map<String, List<Observation>> map) {
        List<Observation> list = null;
        for (String key : buildKeys(encounter.getId(), encounter.getIdentifier())) {
            if (map.containsKey(key)) {     // the same exact list may be represented multiple times for different keys.  we only care about the first
                if (list == null) {
                    list = map.remove(key);
                } else {
                    map.remove(key);
                }
            }
        }
        return list;
    }

    protected List<String> buildKeys(Reference reference) {
        return FhirUtil.buildKeys(reference);
    }

    protected List<String> buildKeys(String id, Identifier identifier) {
        return FhirUtil.buildKeys(id, identifier);
    }

    protected List<String> buildKeys(String id, List<Identifier> identifiers) {
        return FhirUtil.buildKeys(id, identifiers);
    }

    protected String genTemporaryId() {
        return UUID.randomUUID().toString();
    }

//    adapted from CDSHooksExecutor.buildHomeBloodPressureObservation()
//    used when creating new Home Health (HH) Blood Pressure Observations

//    protected Observation buildHomeHealthBloodPressureObservation(BloodPressureModel model, String patientId, FhirConfigManager fcm) throws DataException {
//        return buildHomeHealthBloodPressureObservation(model, null, patientId, fcm);
//    }

    protected Observation buildProtocolObservation(AbstractVitalsModel model, String patientId, FhirConfigManager fcm) throws DataException {
        return buildProtocolObservation(model, null, patientId, fcm);
    }

    protected Observation buildProtocolObservation(AbstractVitalsModel model, Encounter encounter, String patientId, FhirConfigManager fcm) throws DataException {
        Observation o = new Observation();

        o.setId(genTemporaryId());

        o.setSubject(new Reference().setReference(patientId));

        if (encounter != null) {
            o.setEncounter(new Reference().setReference(FhirUtil.toRelativeReference(encounter)));
        } else if (model.getSourceEncounter() != null) {
            o.setEncounter(new Reference().setReference(FhirUtil.toRelativeReference(model.getSourceEncounter())));
        }

        o.setStatus(Observation.ObservationStatus.FINAL);
        o.getCode().addCoding(fcm.getProtocolCoding());

        FhirUtil.addHomeSettingExtension(o);

        o.setEffective(new DateTimeType(model.getReadingDate()));

        String answerValue = model.getFollowedProtocol() ?
                fcm.getProtocolAnswerYes() :
                fcm.getProtocolAnswerNo();

        o.setValue(new CodeableConcept());
        o.getValueCodeableConcept()
                .setText(answerValue)
                .addCoding(fcm.getProtocolAnswerCoding());

        return o;
    }

    protected Goal buildGoal(GoalModel model, String patientId, FhirConfigManager fcm) {

        // this is only used when building local goals, for which sourceGoal == null

        Goal g = new Goal();

        g.setId(model.getExtGoalId());
        g.setSubject(new Reference().setReference(patientId));
        g.setLifecycleStatus(model.getLifecycleStatus().getFhirValue());
        g.getAchievementStatus().addCoding().setCode(model.getAchievementStatus().getFhirValue())
                .setSystem("http://terminology.hl7.org/CodeSystem/goal-achievement");
        g.getCategoryFirstRep().addCoding().setCode(model.getReferenceCode()).setSystem(model.getReferenceSystem());
        g.getDescription().setText(model.getGoalText());
        g.setStatusDate(model.getStatusDate());
        g.getTarget().add(new Goal.GoalTargetComponent()
                .setDue(new DateType().setValue(model.getTargetDate())));

        if (model.isBPGoal()) {
            Goal.GoalTargetComponent systolic = new Goal.GoalTargetComponent();
            systolic.getMeasure().addCoding(fcm.getBpSystolicCoding());
            systolic.setDetail(new Quantity());
            systolic.getDetailQuantity().setCode(fcm.getBpValueCode());
            systolic.getDetailQuantity().setSystem(fcm.getBpValueSystem());
            systolic.getDetailQuantity().setUnit(fcm.getBpValueUnit());
            systolic.getDetailQuantity().setValue(model.getSystolicTarget());
            g.getTarget().add(systolic);

            Goal.GoalTargetComponent diastolic = new Goal.GoalTargetComponent();
            diastolic.getMeasure().addCoding(fcm.getBpDiastolicCoding());
            diastolic.setDetail(new Quantity());
            diastolic.getDetailQuantity().setCode(fcm.getBpValueCode());
            diastolic.getDetailQuantity().setSystem(fcm.getBpValueSystem());
            diastolic.getDetailQuantity().setUnit(fcm.getBpValueUnit());
            diastolic.getDetailQuantity().setValue(model.getDiastolicTarget());
            g.getTarget().add(diastolic);
        }

        return g;
    }

    // most Encounters will be in the workspace cache, but newly created ones will not be in there yet,
    // although they *will* be in the bundle passed in as a parameter.  so consolidate those into one list
    protected List<Encounter> getAllEncounters(Bundle bundle) {
        List<Encounter> list = new ArrayList<>();

        list.addAll(workspace.getEncounters());

        if (bundle != null && bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource()) {
                    if (entry.getResource() instanceof Encounter) {
                        list.add((Encounter) entry.getResource());
                    }
                }
            }
        }

        return list;
    }
}