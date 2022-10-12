package edu.ohsu.cmp.coach.workspace;

import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.SessionMissingException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.fhir.transform.VendorTransformer;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.model.recommendation.Audience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkspaceService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private FhirConfigManager fcm;

    @Value("${fhir.vendor-transformer-class}")
    private String vendorTransformerClass;

    private final Map<String, UserWorkspace> map;

    public WorkspaceService() {
        map = new ConcurrentHashMap<>();
    }

    public boolean exists(String sessionId) {
        return map.containsKey(sessionId);
    }

    public void init(String sessionId, Audience audience, FHIRCredentialsWithClient fcc) throws ConfigurationException {
        try {
            UserWorkspace workspace = new UserWorkspace(ctx, sessionId, audience, fcc, fcm);
            workspace.setVendorTransformer(buildVendorTransformer(workspace));
            map.put(sessionId, workspace);

        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    public UserWorkspace get(String sessionId) throws SessionMissingException {
        if (map.containsKey(sessionId)) {
            return map.get(sessionId);

        } else {
            throw new SessionMissingException(sessionId);
        }
    }

    public boolean shutdown(String sessionId) {
        if (map.containsKey(sessionId)) {
            UserWorkspace workspace = map.remove(sessionId);
            workspace.shutdown();
            return true;
        }
        return false;
    }

    private VendorTransformer buildVendorTransformer(UserWorkspace workspace) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return (VendorTransformer) Class.forName(vendorTransformerClass)
                    .getDeclaredConstructor(UserWorkspace.class)
                    .newInstance(workspace);
    }
}
