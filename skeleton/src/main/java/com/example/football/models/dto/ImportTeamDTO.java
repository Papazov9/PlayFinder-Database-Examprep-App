package com.example.football.models.dto;

import com.google.gson.annotations.Expose;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

public class ImportTeamDTO {

    @Expose
    @Size(min = 3)
    private String name;

    @Expose
    @Size(min = 3)
    private String stadiumName;

    @Expose
    @Min(1000)
    private int fanBase;

    @Expose
    @Size(min = 10)
    private String history;

    @Expose
    private String townName;

    public String getName() {
        return name;
    }

    public String getStadiumName() {
        return stadiumName;
    }

    public int getFanBase() {
        return fanBase;
    }

    public String getHistory() {
        return history;
    }

    public String getTownName() {
        return townName;
    }
}
