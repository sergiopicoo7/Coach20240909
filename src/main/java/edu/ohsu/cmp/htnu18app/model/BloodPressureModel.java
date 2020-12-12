package edu.ohsu.cmp.htnu18app.model;

import edu.ohsu.cmp.htnu18app.exception.DataException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;

public class BloodPressureModel {
    public static final String SYSTEM = "http://loinc.org";
    public static final String CODE = "55284-4";
    public static final String SYSTOLIC_CODE = "8480-6";
    public static final String DIASTOLIC_CODE = "8462-4";

    private QuantityModel systolic;
    private QuantityModel diastolic;
    private Long timestamp;

    private enum ValueType {
        SYSTOLIC,
        DIASTOLIC,
        OTHER,
        UNKNOWN
    }

    public BloodPressureModel(Observation o) throws DataException {
        for (Observation.ObservationComponentComponent occ : o.getComponent()) {
            ValueType valueType = ValueType.UNKNOWN;

            CodeableConcept cc = occ.getCode();
            for (Coding c : cc.getCoding()) {
                if (c.getSystem().equals(SYSTEM) && c.getCode().equals(SYSTOLIC_CODE)) {
                    valueType = ValueType.SYSTOLIC;
                    break;

                } else if (c.getSystem().equals(SYSTEM) && c.getCode().equals(DIASTOLIC_CODE)) {
                    valueType = ValueType.DIASTOLIC;
                    break;
                }
            }

            if (valueType != ValueType.UNKNOWN) {
                Quantity q = occ.getValueQuantity();
                switch (valueType) {
                    case SYSTOLIC: systolic = new QuantityModel(q); break;
                    case DIASTOLIC: diastolic = new QuantityModel(q); break;
                }
            }
        }

        if (o.getEffectiveDateTimeType() != null) {
            this.timestamp = o.getEffectiveDateTimeType().getValue().getTime();

        } else if (o.getEffectiveInstantType() != null) {
            this.timestamp = o.getEffectiveInstantType().getValue().getTime();

        } else if (o.getEffectivePeriod() != null) {
            this.timestamp = o.getEffectivePeriod().getEnd().getTime();

        } else {
            throw new DataException("missing timestamp");
        }
    }

    public QuantityModel getSystolic() {
        return systolic;
    }

    public QuantityModel getDiastolic() {
        return diastolic;
    }

    public Long getTimestamp() {
        return timestamp;
    }
}
