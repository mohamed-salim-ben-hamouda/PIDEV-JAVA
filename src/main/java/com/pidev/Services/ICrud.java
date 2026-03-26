package com.pidev.Services;
import java.util.List;
public interface ICrud<T> {
    void add(T t);
    void delete(int id);
    void update(T t);
    List<T> display();
}
