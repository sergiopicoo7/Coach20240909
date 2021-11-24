package edu.ohsu.cmp.coach.entity.app;

import javax.persistence.*;

@Entity
@Table(schema = "coach", name = "adverse_event")
public class MyAdverseEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private String conceptCode;
    private String conceptSystem;
    private String conceptSystemOID;

    protected MyAdverseEvent() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConceptCode() {
        return conceptCode;
    }

    public void setConceptCode(String conceptCode) {
        this.conceptCode = conceptCode;
    }

    public String getConceptSystem() {
        return conceptSystem;
    }

    public void setConceptSystem(String conceptSystem) {
        this.conceptSystem = conceptSystem;
    }

    public String getConceptSystemOID() {
        return conceptSystemOID;
    }

    public void setConceptSystemOID(String conceptSystemOID) {
        this.conceptSystemOID = conceptSystemOID;
    }
}
