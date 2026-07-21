package com.example.financetracker.simulate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class TradeoffRequest {

    @NotNull
    @Valid
    private TradeoffOptionInput option1;

    @NotNull
    @Valid
    private TradeoffOptionInput option2;

    public TradeoffOptionInput getOption1() {
        return option1;
    }

    public void setOption1(TradeoffOptionInput option1) {
        this.option1 = option1;
    }

    public TradeoffOptionInput getOption2() {
        return option2;
    }

    public void setOption2(TradeoffOptionInput option2) {
        this.option2 = option2;
    }

    public static class TradeoffOptionInput {
        @NotBlank
        private String name;

        @NotNull
        @DecimalMin("0.01")
        private BigDecimal amount;

        /** PURCHASE | SAVING */
        @NotBlank
        private String type;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
