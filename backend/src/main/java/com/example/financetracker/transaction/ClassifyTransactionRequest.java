package com.example.financetracker.transaction;

import jakarta.validation.constraints.NotNull;

public class ClassifyTransactionRequest {

    @NotNull
    private NeedType needType;

    public NeedType getNeedType() {
        return needType;
    }

    public void setNeedType(NeedType needType) {
        this.needType = needType;
    }
}
