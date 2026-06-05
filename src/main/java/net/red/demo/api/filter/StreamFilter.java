package net.red.demo.api.filter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;

@Valid
@Data
public class StreamFilter {
    @NotEmpty
    private String customerIP;
    @NotNull
    @Positive
    private Integer customerId;
}