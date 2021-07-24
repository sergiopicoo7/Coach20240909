package edu.ohsu.cmp.htnu18app.entity.app;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
@Table(schema = "htnu18app")
public class Goal {
    public static final String DEFAULT_BP_GOAL_ID = "default-bp-goal";
    public static final Integer DEFAULT_BP_GOAL_SYSTOLIC = 140;
    public static final Integer DEFAULT_BP_GOAL_DIASTOLIC = 90;
    public static final String BP_GOAL_REFERENCE_SYSTEM = "https://coach.ohsu.edu";
    public static final String BP_GOAL_REFERENCE_CODE = "blood-pressure";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patId;
    private String extGoalId;
    private String referenceSystem;
    private String referenceCode;
    private String goalText;
    private Integer systolicTarget;
    private Integer diastolicTarget;
    private Date targetDate;
    private Date createdDate;

    @OneToMany(mappedBy = "goalId", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<GoalHistory> history;

    protected Goal() {
    }

    public Goal(String extGoalId, String referenceSystem, String referenceCode, String goalText, Date targetDate) {
        this.extGoalId = extGoalId;
        this.referenceSystem = referenceSystem;
        this.referenceCode = referenceCode;
        this.goalText = goalText;
        this.targetDate = targetDate;
    }

    public Goal(String extGoalId, String referenceSystem, String referenceCode, Integer systolicTarget, Integer diastolicTarget) {
        this.extGoalId = extGoalId;
        this.referenceSystem = referenceSystem;
        this.referenceCode = referenceCode;
        this.goalText = "Target BP: " + systolicTarget + "/" + diastolicTarget;
        this.systolicTarget = systolicTarget;
        this.diastolicTarget = diastolicTarget;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPatId() {
        return patId;
    }

    public void setPatId(Long patId) {
        this.patId = patId;
    }

    public String getExtGoalId() {
        return extGoalId;
    }

    public void setExtGoalId(String goalId) {
        this.extGoalId = goalId;
    }

    public String getReferenceSystem() {
        return referenceSystem;
    }

    public void setReferenceSystem(String referenceSystem) {
        this.referenceSystem = referenceSystem;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public String getGoalText() {
        return goalText;
    }

    public void setGoalText(String goalText) {
        this.goalText = goalText;
    }

    public boolean isBloodPressureGoal() {
        return systolicTarget != null && diastolicTarget != null;
    }

    public Integer getSystolicTarget() {
        return systolicTarget;
    }

    public void setSystolicTarget(Integer systolicTarget) {
        this.systolicTarget = systolicTarget;
    }

    public Integer getDiastolicTarget() {
        return diastolicTarget;
    }

    public void setDiastolicTarget(Integer diastolicTarget) {
        this.diastolicTarget = diastolicTarget;
    }

    public Date getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(Date targetDate) {
        this.targetDate = targetDate;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Set<GoalHistory> getHistory() {
        return history;
    }

    public void setHistory(Set<GoalHistory> history) {
        this.history = history;
    }

    public LifecycleStatus getLifecycleStatus() {
        GoalHistory mostRecent = getMostRecentHistory();
        return mostRecent != null ?
                mostRecent.getLifecycleStatus() :
                null;
    }

    public AchievementStatus getAchievementStatus() {
        GoalHistory mostRecent = getMostRecentHistory();
        return mostRecent != null ?
                mostRecent.getAchievementStatus() :
                null;
    }

    public Date getStatusDate() {
        GoalHistory mostRecent = getMostRecentHistory();
        return mostRecent != null ?
                mostRecent.getCreatedDate() :
                null;
    }

    private GoalHistory getMostRecentHistory() {
        GoalHistory mostRecent = null;
        for (GoalHistory gh : history) {
            if (mostRecent == null) mostRecent = gh;
            else {
                if (gh.getCreatedDate().compareTo(mostRecent.getCreatedDate()) > 0) {
                    mostRecent = gh;
                }
            }
        }
        return mostRecent;
    }
}
