package com.codelens.source.model;

/** Ciclo de vida de un snapshot de codigo. */
public enum SnapshotStatus {
    PENDING,
    FETCHING,
    READY,
    FAILED,
    PURGED
}
