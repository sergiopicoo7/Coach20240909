package edu.ohsu.cmp.coach.workspace;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.*;
import edu.ohsu.cmp.coach.model.cqfruler.CDSHook;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.model.recommendation.Audience;
import edu.ohsu.cmp.coach.model.recommendation.Card;
import edu.ohsu.cmp.coach.model.recommendation.Suggestion;
import edu.ohsu.cmp.coach.service.*;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class UserWorkspace {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int POOL_SIZE = 2;

    private static final String CACHE_PATIENT = "Patient";
    private static final String CACHE_ENCOUNTER = "Encounter";
    private static final String CACHE_BP = "BP";
    private static final String CACHE_ADVERSE_EVENT = "AdverseEvent";
    private static final String CACHE_GOAL = "Goal";
    private static final String CACHE_MEDICATION = "Medication";

    private final ApplicationContext ctx;
    private final String sessionId;
    private final Audience audience;
    private final FHIRCredentialsWithClient fhirCredentialsWithClient;
    private final FhirConfigManager fcm;
    private final Long internalPatientId;

    private Cache cache;
    private Cache cardCache;

    private ExecutorService executorService;


    public UserWorkspace(ApplicationContext ctx, String sessionId, Audience audience,
                         FHIRCredentialsWithClient fhirCredentialsWithClient, FhirConfigManager fcm,
                         Long internalPatientId) {
        this.ctx = ctx;
        this.sessionId = sessionId;
        this.audience = audience;
        this.fhirCredentialsWithClient = fhirCredentialsWithClient;
        this.fcm = fcm;
        this.internalPatientId = internalPatientId;

        cache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS)
                .build();

        cardCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS)
                .build();

        executorService = Executors.newFixedThreadPool(POOL_SIZE);
    }

    public Audience getAudience() {
        return audience;
    }

    public FHIRCredentialsWithClient getFhirCredentialsWithClient() {
        return fhirCredentialsWithClient;
    }

    public Long getInternalPatientId() {
        return internalPatientId;
    }

    public void populate(boolean runInNewThread) {
        if (runInNewThread) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    doPopulate();
                }
            };
            executorService.submit(runnable);
        } else {
            doPopulate();
        }
    }

    private void doPopulate() {
        logger.info("BEGIN populating workspace for session=" + sessionId);
        getPatient();
        getGoals();
        getEncounters();
        getBloodPressures();
        getAdverseEvents();
        getMedications();
        getAllCards();
        logger.info("DONE populating workspace for session=" + sessionId);
    }

    public void runRecommendations() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                deleteAllCards();
                getAllCards();
            }
        };
        executorService.submit(runnable);
    }

    public void clearCaches() {
        cache.invalidateAll();
        cardCache.invalidateAll();
    }

    public void shutdown() {
        clearCaches();
        cache.cleanUp();
        cardCache.cleanUp();
    }

///////////////////////////////////////////////////////////////////////////////////////

    public Map<String, Encounter> getEncounterMap() {
        return (Map<String, Encounter>) cache.get(CACHE_ENCOUNTER, new Function<String, Map<String, Encounter>>() {
            @Override
            public Map<String, Encounter> apply(String s) {
                EHRService svc = ctx.getBean(EHRService.class);
                Map<String, Encounter> map = new LinkedHashMap<>();
                for (Encounter encounter : svc.buildEncounterList(sessionId)) {
                    for (String key : FhirUtil.buildKeys(encounter.getId(), encounter.getIdentifier())) {
                        map.put(key, encounter);
                    }
                }
                return map;
            }
        });
    }

    public List<Encounter> getEncounters() {
        return List.copyOf(getEncounterMap().values());
    }

    public Encounter getEncounter(Reference encounterReference) {
        Map<String, Encounter> map = getEncounterMap();
        for (String key : FhirUtil.buildKeys(encounterReference)) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    public PatientModel getPatient() {
        return (PatientModel) cache.get(CACHE_PATIENT, new Function<String, PatientModel>() {
            @Override
            public PatientModel apply(String s) {
                PatientService svc = ctx.getBean(PatientService.class);
                return svc.buildPatient(sessionId);
            }
        });
    }

    public List<BloodPressureModel> getBloodPressures() {
        return (List<BloodPressureModel>) cache.get(CACHE_BP, new Function<String, List<BloodPressureModel>>() {
            @Override
            public List<BloodPressureModel> apply(String s) {
                BloodPressureService svc = ctx.getBean(BloodPressureService.class);
                try {
                    return svc.buildBloodPressureList(sessionId);
                } catch (DataException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public List<AdverseEventModel> getAdverseEvents() {
        return (List<AdverseEventModel>) cache.get(CACHE_ADVERSE_EVENT, new Function<String, List<AdverseEventModel>>() {
            @Override
            public List<AdverseEventModel> apply(String s) {
                AdverseEventService svc = ctx.getBean(AdverseEventService.class);
                try {
                    return svc.buildAdverseEvents(sessionId);
                } catch (DataException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public List<GoalModel> getGoals() {
        return (List<GoalModel>) cache.get(CACHE_GOAL, new Function<String, List<GoalModel>>() {
            @Override
            public List<GoalModel> apply(String s) {
                GoalService svc = ctx.getBean(GoalService.class);
                return svc.buildCurrentGoals(sessionId);
            }
        });
    }

    public List<MedicationModel> getMedications() {
        return (List<MedicationModel>) cache.get(CACHE_MEDICATION, new Function<String, List<MedicationModel>>() {
            @Override
            public List<MedicationModel> apply(String s) {
                MedicationService svc = ctx.getBean(MedicationService.class);
                try {
                    return svc.buildMedications(sessionId);
                } catch (DataException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public Map<String, List<Card>> getAllCards() {
        Map<String, List<Card>> map = new LinkedHashMap<>();
        RecommendationService svc = ctx.getBean(RecommendationService.class);
        try {
            for (CDSHook hook : svc.getCDSHooks()) {
                try {
                    map.put(hook.getId(), getCards(hook.getId()));
                } catch (Exception e) {
                    logger.error("caught " + e.getClass().getName() + " getting cards for hook=" + hook.getId() + " - " + e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return map;
    }

    public List<Card> getCards(String recommendationId) {
        return (List<Card>) cardCache.get(recommendationId, new Function<String, List<Card>>() {
            @Override
            public List<Card> apply(String s) {
                RecommendationService svc = ctx.getBean(RecommendationService.class);
                try {
                    return svc.getCards(sessionId, s);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void deleteCards(String recommendationId) {
        cardCache.invalidate(recommendationId);
    }

    public void deleteAllCards() {
        cardCache.invalidateAll();
    }

    /**
     * used to clear a particular Suggestion from the cache, by ID.  very useful for updating the cache in-place
     * after the user performs an action that should make that suggestion disappear
     * @param id
     * @return
     */
    public boolean deleteSuggestion(String id) {
        boolean rval = false;
        for (Map.Entry<String, List<Card>> entry : ((Map<String, List<Card>>) cardCache.asMap()).entrySet()) {
            for (Card c : entry.getValue()) {
                if (c.getSuggestions() != null) {
                    Iterator<Suggestion> iter = c.getSuggestions().iterator();
                    while (iter.hasNext()) {
                        Suggestion s = iter.next();
                        if (s.getId() != null && s.getId().equals(id)) {    // ignore suggestions without IDs
                            iter.remove();
                            rval = true;
                        }
                    }
                }
            }
        }
        return rval;
    }
}
