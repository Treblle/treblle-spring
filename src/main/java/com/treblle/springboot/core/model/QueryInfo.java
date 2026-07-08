package com.treblle.springboot.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * An item of the {@code data.queries} array.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class QueryInfo {

    private String sql;
    private double time;

    public QueryInfo() {
    }

    public QueryInfo(String sql, double time) {
        this.sql = sql;
        this.time = time;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }
}
