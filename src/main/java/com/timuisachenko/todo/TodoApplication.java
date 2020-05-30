package com.timuisachenko.todo;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ResponseTransformer;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toConcurrentMap;
import static java.util.stream.Collectors.toList;
import static spark.Spark.*;

public class TodoApplication {
    private static ConcurrentMap<Long, Todo> repo;
    private static AtomicLong idIncrement;
    private static Logger LOGGER = LoggerFactory.getLogger(TodoApplication.class.getSimpleName());

    public static void main(String[] args) {
        Gson gson = new Gson();
        repo = Stream.of(
                new Todo(1l, "Привет это мой таск лист", false),
                new Todo(2l, "Мне нужно постоянно просматривать и сзаписывать задания ", false),
                new Todo(3l, "А это задание я уже выполнил", true))
                .collect(toConcurrentMap(f -> f.getId(), Function.identity()));
        idIncrement = new AtomicLong(repo.keySet().stream().max(Comparator.naturalOrder()).orElseGet(() -> 0l));

        path("/api", () -> {
            get("/todo", "application/json", (req, res) -> repo.keySet().stream().map(k -> repo.get(k)).collect(toList()), new ResponseTransformer() {
                @Override
                public String render(Object o) throws Exception {
                    return gson.toJson(o);
                }
            });
            post("/todo", (req, res) -> {
                Todo todo = gson.fromJson(req.body(), Todo.class);
                repo.putIfAbsent(idIncrement.incrementAndGet(), new Todo(idIncrement.longValue(), todo.getText(), false));
                return repo.get(idIncrement);
            });
            put("/todo",
                    (req, res) -> {
                        Todo todo = gson.fromJson(req.body(), Todo.class);
                        repo.put(todo.id, new Todo(todo.id, todo.getText(), todo.isCompleted()));
                        LOGGER.debug(todo.toString());
                        return repo.get(idIncrement);
                    }
            );
            delete("/todo/:id", (req, res) -> repo.remove(Long.parseLong(req.params(":id"))));

        });
    }

    private static class Todo {
        private Long id;
        private String text;
        private boolean completed;

        public Todo(Long id) {
            this.id = id;
        }

        public Todo(Long id, String text, boolean completed) {
            this.id = id;
            this.text = text;
            this.completed = completed;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Todo todo = (Todo) o;
            return completed == todo.completed &&
                    Objects.equals(id, todo.id) &&
                    Objects.equals(text, todo.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, text, completed);
        }
    }
}
