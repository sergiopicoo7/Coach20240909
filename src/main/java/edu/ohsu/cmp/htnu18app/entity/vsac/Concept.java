package edu.ohsu.cmp.htnu18app.entity.vsac;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
@Table(schema = "vsac")
public class Concept {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String codeSystem;
    private String codeSystemName;
    private String codeSystemVersion;
    private String displayName;
    private Date created;
    private Date updated;

    // see: https://attacomsian.com/blog/spring-data-jpa-many-to-many-mapping
    @ManyToMany(mappedBy = "concepts", fetch = FetchType.LAZY)
    private Set<ValueSet> valueSets;

    @Override
    public String toString() {
        return "Concept{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", codeSystem='" + codeSystem + '\'' +
                ", codeSystemName='" + codeSystemName + '\'' +
                ", codeSystemVersion='" + codeSystemVersion + '\'' +
                ", displayName='" + displayName + '\'' +
                ", created=" + created +
                ", updated=" + updated +
//                ", valueSets=" + valueSets +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCodeSystem() {
        return codeSystem;
    }

    public void setCodeSystem(String codeSystem) {
        this.codeSystem = codeSystem;
    }

    public String getCodeSystemName() {
        return codeSystemName;
    }

    public void setCodeSystemName(String codeSystemName) {
        this.codeSystemName = codeSystemName;
    }

    public String getCodeSystemVersion() {
        return codeSystemVersion;
    }

    public void setCodeSystemVersion(String codeSystemVersion) {
        this.codeSystemVersion = codeSystemVersion;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public Set<ValueSet> getValueSets() {
        return valueSets;
    }

    public void setValueSets(Set<ValueSet> valueSets) {
        this.valueSets = valueSets;
    }
}
