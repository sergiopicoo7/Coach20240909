package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.app.GoalHistory;
import edu.ohsu.cmp.coach.repository.app.GoalHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class GoalHistoryService extends BaseService {

    @Autowired
    private GoalHistoryRepository repository;

//    public List<GoalHistory> getGoalHistoryList(String sessionId, String extGoalId) {
//        CacheData cache = SessionCache.getInstance().get(sessionId);
//        return repository.findAllByPatIdAndExtGoalId(cache.getInternalPatientId(), extGoalId);
//    }

    public GoalHistory create(GoalHistory goalHistory) {
        goalHistory.setCreatedDate(new Date());
        return repository.save(goalHistory);
    }
}