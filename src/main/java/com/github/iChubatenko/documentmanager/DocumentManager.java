package com.github.iChubatenko.documentmanager;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Optional.*;

public class DocumentManager {

    private final Map<String, Document> storage = new ConcurrentHashMap<>();

    public Document save(Document document) {
        if (document.getId() == null || document.getId().isEmpty()) {
            document.setId(UUID.randomUUID().toString());
            if (document.getCreated() == null) {
                document.setCreated(Instant.now());
            }
        } else {
            Document existing = storage.get(document.getId());
            Instant created = ofNullable(existing)
                    .map(Document::getCreated)
                    .orElseGet(() -> ofNullable(document.getCreated()).orElse(Instant.now()));
            document.setCreated(created);
        }

        storage.put(document.getId(), document);
        return document;
    }

    public List<Document> search(SearchRequest request) {
        return storage.values().stream()
                .filter(doc -> ofNullable(request.getTitlePrefixes())
                        .map(prefixes -> prefixes.stream()
                                .anyMatch(prefix -> ofNullable(doc.getTitle())
                                        .map(t -> t.startsWith(prefix))
                                        .orElse(false)))
                        .orElse(true))

                .filter(doc -> ofNullable(request.getContainsContents())
                        .map(contents -> contents.stream()
                                .anyMatch(sub -> ofNullable(doc.getContent())
                                        .map(c -> c.contains(sub))
                                        .orElse(false)))
                        .orElse(true))

                .filter(doc -> ofNullable(request.getAuthorIds())
                        .map(ids -> ofNullable(doc.getAuthor())
                                .map(author -> ids.contains(author.getId()))
                                .orElse(false))
                        .orElse(true))

                .filter(doc -> ofNullable(request.getCreatedFrom())
                        .map(from -> ofNullable(doc.getCreated())
                                .map(created -> !created.isBefore(from))
                                .orElse(false))
                        .orElse(true))

                .filter(doc -> ofNullable(request.getCreatedTo())
                        .map(to -> ofNullable(doc.getCreated())
                                .map(created -> !created.isAfter(to))
                                .orElse(false))
                        .orElse(true))

                .collect(Collectors.toList());
    }

    public Optional<Document> findById(String id) {
        return ofNullable(storage.get(id));
    }

    @Data
    @Builder
    public static class SearchRequest {
        private List<String> titlePrefixes;
        private List<String> containsContents;
        private List<String> authorIds;
        private Instant createdFrom;
        private Instant createdTo;
    }

    @Data
    @Builder
    public static class Document {
        private String id;
        private String title;
        private String content;
        private Author author;
        private Instant created;
    }

    @Data
    @Builder
    public static class Author {
        private String id;
        private String name;
    }
}
