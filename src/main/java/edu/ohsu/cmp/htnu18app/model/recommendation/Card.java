package edu.ohsu.cmp.htnu18app.model.recommendation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSCard;
import edu.ohsu.cmp.htnu18app.cqfruler.model.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Card {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String summary;
    private String indicator;
    private String detail;
    private Source source;
    private String rationale;
    private List<Suggestion> suggestions;
    private String selectionBehavior;
    private List<Link> links;

    public Card(CDSCard cdsCard, List<String> filterGoalIds) throws IOException {
        this.summary = cdsCard.getSummary();
        this.indicator = cdsCard.getIndicator();
        this.detail = cdsCard.getDetail();
        this.source = cdsCard.getSource();

        this.rationale = cdsCard.getRationale();

        this.selectionBehavior = cdsCard.getSelectionBehavior();

        Gson gson = new GsonBuilder().create();
        try {
            this.suggestions = gson.fromJson(cdsCard.getSuggestions(), new TypeToken<ArrayList<Suggestion>>(){}.getType());

            // filter goals that the user has already responded to
            if (this.suggestions != null) {
                Iterator<Suggestion> iter = this.suggestions.iterator();
                while (iter.hasNext()) {
                    Suggestion s = iter.next();
                    if (s.getType().equals(Suggestion.TYPE_GOAL) && filterGoalIds.contains(s.getId())) {
                        iter.remove();
                    }
                }
            }

        } catch (JsonSyntaxException e) {
            logger.error("caught " + e.getClass().getName() + " processing suggestions - " + e.getMessage(), e);
            logger.error("JSON=" + cdsCard.getSuggestions());
            throw e;
        }

        try {
            this.links = gson.fromJson(cdsCard.getLinks(), new TypeToken<ArrayList<Link>>(){}.getType());

        } catch (JsonSyntaxException e) {
            logger.error("caught " + e.getClass().getName() + " processing links - " + e.getMessage(), e);
            logger.error("JSON=" + cdsCard.getLinks());
            throw e;
        }
    }

    public String getSummary() {
        return summary;
    }

    public String getIndicator() {
        return indicator;
    }

    public String getDetail() {
        return detail;
    }

    public Source getSource() {
        return source;
    }

    public String getRationale() {
        return rationale;
    }

    public List<Suggestion> getSuggestions() {
        return suggestions;
    }

    public String getSelectionBehavior() {
        return selectionBehavior;
    }

    public List<Link> getLinks() {
        return links;
    }
}
