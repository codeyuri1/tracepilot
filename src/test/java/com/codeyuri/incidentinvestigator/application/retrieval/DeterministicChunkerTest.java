package com.codeyuri.incidentinvestigator.application.retrieval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeterministicChunkerTest {

    @Test
    void preservesTheConfiguredOverlapAtChunkBoundaries() {
        var chunks = new DeterministicChunker(5, 2).chunk("abcdefghij");

        assertThat(chunks).extracting(TextChunk::content).containsExactly("abcde", "defgh", "ghij");
        assertThat(chunks).extracting(TextChunk::index).containsExactly(0, 1, 2);
    }

    @Test
    void normalizesLineEndingsAndReturnsNoChunkForBlankDocuments() {
        var chunker = new DeterministicChunker(20, 3);

        assertThat(chunker.chunk("  alpha\r\n beta\t\t gamma  ")).extracting(TextChunk::content)
                .containsExactly("alpha\n beta gamma");
        assertThat(chunker.chunk(" \n\t ")).isEmpty();
    }

    @Test
    void rejectsAnOverlapThatWouldPreventProgress() {
        assertThatThrownBy(() -> new DeterministicChunker(10, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
