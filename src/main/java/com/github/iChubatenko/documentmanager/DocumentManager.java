package com.github.iChubatenko.documentmanager;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DocumentManager {

    private final Map<String, Document> storage = new ConcurrentHashMap<>();

    public Document save(Document document) {
        if (document.getId() == null || document.getId().isEmpty()) {
            String id = UUID.randomUUID().toString();
            document.setId(id);
        }

        Document existing = storage.get(document.getId());
        if (existing != null) {
            document.setCreated(existing.getCreated());
        } else if (document.getCreated() == null) {
            document.setCreated(Instant.now());
        }

        storage.put(document.getId(), document);
        return document;
    }

    public List<Document> search(SearchRequest request) {
        return storage.values().stream()
                .filter(doc -> Optional.ofNullable(request.getTitlePrefixes())
                        .map(prefixes -> prefixes.stream()
                                .anyMatch(prefix -> Optional.ofNullable(doc.getTitle())
                                        .map(t -> t.startsWith(prefix))
                                        .orElse(false)))
                        .orElse(true))

                .filter(doc -> Optional.ofNullable(request.getContainsContents())
                        .map(contents -> contents.stream()
                                .anyMatch(sub -> Optional.ofNullable(doc.getContent())
                                        .map(c -> c.contains(sub))
                                        .orElse(false)))
                        .orElse(true))

                .filter(doc -> Optional.ofNullable(request.getAuthorIds())
                        .map(ids -> Optional.ofNullable(doc.getAuthor())
                                .map(author -> ids.contains(author.getId()))
                                .orElse(false))
                        .orElse(true))

                .filter(doc -> Optional.ofNullable(request.getCreatedFrom())
                        .map(from -> Optional.ofNullable(doc.getCreated())
                                .map(created -> !created.isBefore(from))
                                .orElse(false))
                        .orElse(true))

                .filter(doc -> Optional.ofNullable(request.getCreatedTo())
                        .map(to -> Optional.ofNullable(doc.getCreated())
                                .map(created -> !created.isAfter(to))
                                .orElse(false))
                        .orElse(true))

                .collect(Collectors.toList());
    }

    public Optional<Document> findById(String id) {
        return Optional.ofNullable(storage.get(id));
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
