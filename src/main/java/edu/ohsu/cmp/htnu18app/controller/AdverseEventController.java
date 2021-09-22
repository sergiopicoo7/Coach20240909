package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.entity.app.Outcome;
import edu.ohsu.cmp.htnu18app.service.AdverseEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/adverse-event")
public class AdverseEventController {

    @Autowired
    private AdverseEventService adverseEventService;

    @PostMapping("register-action")
    public ResponseEntity<String> registerAction(HttpSession session,
                                                 @RequestParam("adverseEventId") String adverseEventId,
                                                 @RequestParam("actionTaken") Boolean actionTaken) {

        // get the cache just to make sure it's defined and the user is properly authenticated
        CacheData cache = SessionCache.getInstance().get(session.getId());

        HttpStatus status = HttpStatus.OK;
        String message;
        if (actionTaken) {
            // for the purposes of this app, if action is taken, the adverse event is considered resolved
            boolean success = adverseEventService.setOutcome(adverseEventId, Outcome.RESOLVED);

            if (success) {
                cache.deleteSuggestion(adverseEventId);
                message = "update successful";

            } else {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                message = "error - check server logs";
            }

        } else {
            message = "no action taken";
        }

        return new ResponseEntity<>(message, status);
    }
}