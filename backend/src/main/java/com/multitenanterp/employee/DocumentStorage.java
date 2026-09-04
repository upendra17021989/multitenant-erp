package com.multitenanterp.employee;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentStorage {
    void store(String key, InputStream content) throws IOException;
    Resource load(String key);
    void delete(String key) throws IOException;
}
