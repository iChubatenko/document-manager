package com.github.iChubatenko.documentmanager;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


class DocumentManagerTest {

    private DocumentManager manager;

    @BeforeEach
    void setUp() {
        manager = new DocumentManager();
    }

    @Test
    void save_shouldAssignIdAndCreated_ifNewDocument() {
        DocumentManager.Document document = DocumentManager.Document.builder()
                .title("Doc Title")
                .content("Some content")
                .author(DocumentManager.Author.builder().id("author1").name("John").build())
                .build();

        DocumentManager.Document saved = manager.save(document);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreated());
        assertEquals("Doc Title", saved.getTitle());
        assertEquals("Some content", saved.getContent());
    }

    @Test
    void save_shouldPreserveCreated_ifDocumentAlreadyExists() {
        DocumentManager.Document doc1 = DocumentManager.Document.builder()
                .id("123")
                .title("Original")
                .content("Content")
                .author(DocumentManager.Author.builder().id("a1").name("Author").build())
                //wil be ignored
                .created(Instant.parse("2025-01-01T10:00:00Z"))
                .build();

        DocumentManager.Document saved1 = manager.save(doc1);

        DocumentManager.Document updated = DocumentManager.Document.builder()
                .id("123")
                .title("Updated")
                .content("Updated content")
                .author(doc1.getAuthor())
                .build();

        DocumentManager.Document result = manager.save(updated);

        assertEquals("Updated", result.getTitle());
        assertEquals("Updated content", result.getContent());
        assertEquals("123", result.getId());
        assertEquals(saved1.getCreated(), result.getCreated());
    }

    @Test
    void findById_shouldReturnDocument_ifExists() {
        DocumentManager.Document doc = DocumentManager.Document.builder()
                .title("Find me")
                .build();

        DocumentManager.Document saved = manager.save(doc);
        Optional<DocumentManager.Document> found = manager.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Find me", found.get().getTitle());
    }

    @Test
    void findById_shouldReturnEmpty_ifNotExists() {
        assertFalse(manager.findById("nonexistent").isPresent());
    }

    @Test
    void search_shouldReturnMatchingTitlePrefix() {
        manager.save(DocumentManager.Document.builder()
                .title("Java Tasks")
                .content("Some content")
                .author(DocumentManager.Author.builder().id("a1").build())
                .build());

        manager.save(DocumentManager.Document.builder()
                .title("Python Tasks")
                .build());

        DocumentManager.SearchRequest request = DocumentManager.SearchRequest.builder()
                .titlePrefixes(List.of("Java"))
                .build();

        List<DocumentManager.Document> results = manager.search(request);
        assertEquals(1, results.size());
        assertEquals("Java Tasks", results.get(0).getTitle());
    }

    @Test
    void search_shouldReturnMatchingContent() {
        manager.save(DocumentManager.Document.builder()
                .title("Doc 1")
                .content("Important document")
                .build());

        DocumentManager.SearchRequest request = DocumentManager.SearchRequest.builder()
                .containsContents(List.of("document"))
                .build();

        List<DocumentManager.Document> results = manager.search(request);
        assertEquals(1, results.size());
    }

    @Test
    void search_shouldReturnMatchingAuthorId() {
        manager.save(DocumentManager.Document.builder()
                .title("Author match")
                .author(DocumentManager.Author.builder().id("a123").name("Taras").build())
                .build());

        DocumentManager.SearchRequest request = DocumentManager.SearchRequest.builder()
                .authorIds(List.of("a123"))
                .build();

        List<DocumentManager.Document> results = manager.search(request);
        assertEquals(1, results.size());
    }

    @Test
    void search_shouldReturnMatchingCreatedRange() {
        Instant now = Instant.now();

        manager.save(DocumentManager.Document.builder()
                .title("Old")
                .created(now.minusSeconds(86400)) // 1 day ago
                .build());

        manager.save(DocumentManager.Document.builder()
                .title("Recent")
                .created(now)
                .build());

        DocumentManager.SearchRequest request = DocumentManager.SearchRequest.builder()
                .createdFrom(now.minusSeconds(3600)) // 1 hour ago
                .createdTo(now.plusSeconds(60))
                .build();

        List<DocumentManager.Document> results = manager.search(request);
        assertEquals(1, results.size());
        assertEquals("Recent", results.get(0).getTitle());
    }

    @Test
    void search_shouldReturnAll_ifNoCriteria() {
        manager.save(DocumentManager.Document.builder().title("1").build());
        manager.save(DocumentManager.Document.builder().title("2").build());

        DocumentManager.SearchRequest empty = DocumentManager.SearchRequest.builder().build();
        List<DocumentManager.Document> results = manager.search(empty);

        assertEquals(2, results.size());
    }

    @Test
    void search_shouldReturnDocumentMatchingMultipleCriteria() {
        DocumentManager.Author author1 = DocumentManager.Author.builder()
                .id("author1")
                .name("Petro Petrenko")
                .build();

        DocumentManager.Author author2 = DocumentManager.Author.builder()
                .id("author2")
                .name("Someone Else")
                .build();

        manager.save(DocumentManager.Document.builder()
                .id("1")
                .title("Java Streams")
                .content("This document explains Java Streams API in detail")
                .author(author1)
                .created(Instant.parse("2025-04-01T10:00:00Z"))
                .build());

        manager.save(DocumentManager.Document.builder()
                .id("2")
                .title("Java Streams Basics")
                .content("Stream and more")
                .author(author2)
                .created(Instant.parse("2025-03-01T10:00:00Z"))
                .build());

        manager.save(DocumentManager.Document.builder()
                .id("3")
                .title("Java Stream Examples")
                .content("Stream processing examples in Java")
                .author(author1)
                .created(Instant.parse("2025-02-15T10:00:00Z"))
                .build());

        manager.save(DocumentManager.Document.builder()
                .id("4")
                .title("Advanced Streams in Java")
                .content("Java Stream internals")
                .author(author1)
                .created(Instant.parse("2025-06-01T10:00:00Z"))
                .build());

        manager.save(DocumentManager.Document.builder()
                .id("5")
                .title("Java Stream Tricks")
                .content("Stream tips and tricks")
                .author(author1)
                .created(Instant.parse("2025-01-10T10:00:00Z"))
                .build());


        DocumentManager.SearchRequest request = DocumentManager.SearchRequest.builder()
                .titlePrefixes(List.of("Java"))
                .containsContents(List.of("Stream"))
                .authorIds(List.of("author1"))
                .createdFrom(Instant.parse("2024-12-01T00:00:00Z"))
                .createdTo(Instant.parse("2025-12-31T23:59:59Z"))
                .build();

        List<DocumentManager.Document> result = manager.search(request);

        assertEquals(3, result.size());

        List<String> expectedIds = List.of("1", "3", "5");
        List<String> actualIds = result.stream().map(DocumentManager.Document::getId).toList();

        assertTrue(actualIds.containsAll(expectedIds));
    }
}