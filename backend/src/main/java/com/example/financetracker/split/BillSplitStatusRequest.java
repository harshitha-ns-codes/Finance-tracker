package com.example.financetracker.split;

import jakarta.validation.constraints.NotNull;

public class BillSplitStatusRequest {

    @NotNull
    private SplitStatus status;

    public SplitStatus getStatus() {
        return status;
    }

    public void setStatus(SplitStatus status) {
        this.status = status;
    }
}
