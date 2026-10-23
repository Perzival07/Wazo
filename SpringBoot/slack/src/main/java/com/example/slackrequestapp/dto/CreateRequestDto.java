package com.example.slackrequestapp.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRequestDto(@NotBlank String title, String description) {}