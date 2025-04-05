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
                .filter(doc -> {
                    if (request.getTitlePrefixes() != null && !request.getTitlePrefixes().isEmpty()) {
                        boolean matches = request.getTitlePrefixes().stream()
                                .anyMatch(prefix -> doc.getTitle() != null && doc.getTitle().startsWith(prefix));
                        if (!matches) return false;
                    }

                    if (request.getContainsContents() != null && !request.getContainsContents().isEmpty()) {
                        boolean matches = request.getContainsContents().stream()
                                .anyMatch(substring -> doc.getContent() != null && doc.getContent().contains(substring));
                        if (!matches) return false;
                    }

                    if (request.getAuthorIds() != null && !request.getAuthorIds().isEmpty()) {
                        if (doc.getAuthor() == null || !request.getAuthorIds().contains(doc.getAuthor().getId())) {
                            return false;
                        }
                    }

                    if (request.getCreatedFrom() != null && doc.getCreated() != null &&
                            doc.getCreated().isBefore(request.getCreatedFrom())) {
                        return false;
                    }

                    if (request.getCreatedTo() != null && doc.getCreated() != null &&
                            doc.getCreated().isAfter(request.getCreatedTo())) {
                        return false;
                    }
                    return true;
                })
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
