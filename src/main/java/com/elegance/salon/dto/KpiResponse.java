package com.elegance.salon.dto;

public class KpiResponse {
    private String id;
    private String title;
    private String value;
    private String change;
    private Boolean isPositive;
    private String period;
    private String iconName;

    public KpiResponse() {
    }

    public KpiResponse(String id, String title, String value, String change, Boolean isPositive, String period, String iconName) {
        this.id = id;
        this.title = title;
        this.value = value;
        this.change = change;
        this.isPositive = isPositive;
        this.period = period;
        this.iconName = iconName;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getChange() { return change; }
    public void setChange(String change) { this.change = change; }

    public Boolean getIsPositive() { return isPositive; }
    public void setIsPositive(Boolean isPositive) { this.isPositive = isPositive; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }
}
