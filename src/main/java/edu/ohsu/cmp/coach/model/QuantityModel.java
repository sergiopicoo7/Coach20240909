package edu.ohsu.cmp.coach.model;

import org.hl7.fhir.r4.model.Quantity;

import java.math.BigDecimal;

public class QuantityModel {
    private BigDecimal value;
    private String unit;

    public QuantityModel(Quantity q) {
        this.value = q.getValue();
        this.unit = q.getUnit();
    }

    public QuantityModel(Integer value, String unit) {
        this.value = new BigDecimal(value);
        this.unit = unit;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
