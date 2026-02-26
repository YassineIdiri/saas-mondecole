package com.example.mondecole_pocket.dto;

import jakarta.validation.constraints.NotNull;

public record ReorderRequest(
        @NotNull
        Long id,

        @NotNull
        Integer orderIndex
) {}