package com.pidev.Services;

import java.util.List;

public interface ICrud<T> {
    void add(T t);
    void update(T t);
    void delete(int id);
    List<T> getAll();
    T getById(int id);
}
