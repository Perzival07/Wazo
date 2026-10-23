package com.example.demo;

import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


@Service 
public class TodoService {
    
    private final Map<Long, Todo> store = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong();

    public List<Todo> findAll(){
        return store.values().stream().sorted(Comparator.comparingLong(Todo::id)).toList();
    }

    public Optional<Todo> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Todo create(Todo input) {
        long id = counter.incrementAndGet();
        Todo saved = new Todo(id, input.title(), input.done());
        store.put(id,saved);
        return saved;
    }

    public Optional<Todo> update (long id, Todo input){
        if (!store.containsKey(id)) return Optional.empty();
        Todo updated = new Todo(id, input.title(), input.done());
        store.put(id, updated);
        return Optional.of(updated);
    }
    public boolean delete(long id){
        return store.remove(id)!= null;
    }



}
