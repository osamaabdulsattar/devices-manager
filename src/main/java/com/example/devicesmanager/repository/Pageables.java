package com.example.devicesmanager.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Pageables {

    private Pageables() {}

    /**
     * Appends the primary key as a final sort order so pages are deterministic even when the requested sort
     * has ties (e.g. two devices with the same name); otherwise rows could repeat or be skipped across pages.
     */
    public static Pageable withIdTieBreaker(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().and(Sort.by("id")));
    }
}
