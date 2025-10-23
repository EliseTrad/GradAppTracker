package com.gradapptracker.backend.programdocument.entity;

import com.gradapptracker.backend.document.entity.Document;
import com.gradapptracker.backend.program.entity.Program;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * JPA entity for the program_documents join table.
 */
@Entity
@Table(name = "program_documents", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "program_id", "document_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "program", "document" })
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProgramDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "program_doc_id")
    private Integer programDocId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "usage_notes", columnDefinition = "text")
    private String usageNotes;

}

