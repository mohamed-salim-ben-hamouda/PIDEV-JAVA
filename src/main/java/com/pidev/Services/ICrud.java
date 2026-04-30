package com.pidev.Services;

import java.util.List;

public interface ICrud<T> {
    boolean add(T t);
    boolean update(T t);
    void delete(int id);
    List<T> getAll();
    T getById(int id);
}
