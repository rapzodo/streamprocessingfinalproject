package com.gridu.persistence;

import org.apache.spark.sql.Dataset;

import java.util.List;

public interface BaseDao<T> {
    static final long TTL = 3;

    void persist(Dataset<T> t);

    List<T> getAllRecords();

}
