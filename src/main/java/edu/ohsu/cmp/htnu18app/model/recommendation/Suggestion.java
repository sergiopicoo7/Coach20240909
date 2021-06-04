package edu.ohsu.cmp.htnu18app.model.recommendation;

import java.util.List;

public class Suggestion {
    private String label;
    private List<String> actions;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }
}