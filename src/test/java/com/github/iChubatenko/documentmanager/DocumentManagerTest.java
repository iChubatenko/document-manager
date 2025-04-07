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
     void setUp(){
        manager = new DocumentManager();
    }

    @Test
    void save_shouldAssignIdAndCreated_ifNewDocument(){
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
    void save_shouldPreserveCreated_ifDocumentAlreadyExists(){
        DocumentManager.Document doc1 = DocumentManager.Document.builder()
                .id("123")
                .title("Original")
                .content("Content")
                .author(DocumentManager.Author.builder().id("a1").name("Author").build())
                .created(Instant.parse("2025-01-01T10:00:00+02:00"))
                .build();

        manager.save(doc1);

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
        assertEquals(Instant.parse("2025-01-01T10:00:00+02:00"), result.getCreated());
    }

    @Test
    void findById_shouldReturnDocument_ifExists(){
        DocumentManager.Document doc = DocumentManager.Document.builder()
                .title("Find me")
                .build();

        DocumentManager.Document saved = manager.save(doc);
        Optional<DocumentManager.Document> found = manager.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Find me", found.get().getTitle());
    }

    @Test
    void findById_shouldReturnEmpty_ifNotExists(){
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
    void search_shouldReturnAll_ifNoCriteria(){
        manager.save(DocumentManager.Document.builder().title("1").build());
        manager.save(DocumentManager.Document.builder().title("2").build());

        DocumentManager.SearchRequest empty = DocumentManager.SearchRequest.builder().build();
        List<DocumentManager.Document> results = manager.search(empty);

        assertEquals(2, results.size());
    }

}