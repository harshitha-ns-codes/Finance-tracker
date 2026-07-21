package com.example.financetracker.transaction;

import jakarta.validation.constraints.NotNull;

public class RegretUpdateRequest {

    @NotNull
    private RegretStatus status;

    public RegretStatus getStatus() {
        return status;
    }

    public void setStatus(RegretStatus status) {
        this.status = status;
    }
}
