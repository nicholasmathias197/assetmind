package com.assetmind.core.service;

import com.assetmind.core.domain.DepreciationMethod;
import com.assetmind.core.domain.DepreciationRequest;
import com.assetmind.core.domain.ScheduleLine;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultDepreciationEngine implements DepreciationEngine {

    @Override
    public List<ScheduleLine> calculateSchedule(DepreciationRequest request) {
        BigDecimal adjustedBasis = applyTaxElections(request);
        return switch (request.method()) {
            case STRAIGHT_LINE, ADS_STRAIGHT_LINE -> straightLineSchedule(adjustedBasis, request.usefulLifeYears(), request.salvageValue());
            case MACRS_200DB_HY -> macrsHalfYearSchedule(adjustedBasis, request.usefulLifeYears());
        };
    }

    private BigDecimal applyTaxElections(DepreciationRequest request) {
        BigDecimal basis = request.costBasis();
        if (request.section179Enabled() && request.section179Amount() != null) {
            basis = basis.subtract(request.section179Amount().min(basis));
        }

        if (request.bonusDepreciationRate() != null && request.bonusDepreciationRate().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal bonus = basis.multiply(request.bonusDepreciationRate()).setScale(2, RoundingMode.HALF_UP);
            basis = basis.subtract(bonus);
        }

        return basis.max(BigDecimal.ZERO);
    }

    private List<ScheduleLine> straightLineSchedule(BigDecimal basis, int usefulLifeYears, BigDecimal salvageValue) {
        int life = Math.max(1, usefulLifeYears);
        BigDecimal depreciableAmount = basis.subtract(salvageValue).max(BigDecimal.ZERO);
        BigDecimal annualExpense = depreciableAmount.divide(BigDecimal.valueOf(life), 2, RoundingMode.HALF_UP);

        List<ScheduleLine> schedule = new ArrayList<>();
        BigDecimal openingValue = basis;

        for (int year = 1; year <= life; year++) {
            BigDecimal expense = year == life
                    ? openingValue.subtract(salvageValue).max(BigDecimal.ZERO)
                    : annualExpense.min(openingValue);
            BigDecimal ending = openingValue.subtract(expense).max(salvageValue);
            schedule.add(new ScheduleLine(year, openingValue, expense, ending, "Straight-line depreciation"));
            openingValue = ending;
        }

        return schedule;
    }

    private List<ScheduleLine> macrsHalfYearSchedule(BigDecimal basis, int usefulLifeYears) {
        int life = Math.max(1, usefulLifeYears);
        List<ScheduleLine> schedule = new ArrayList<>();

        // Simplified 200% declining balance with half-year convention switch-to-SL behavior.
        BigDecimal openingValue = basis;
        BigDecimal rate = BigDecimal.valueOf(2.0d / life);

        for (int year = 1; year <= life + 1; year++) {
            BigDecimal yearRate = (year == 1 || year == life + 1)
                    ? rate.divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP)
                    : rate;

            BigDecimal dbExpense = openingValue.multiply(yearRate).setScale(2, RoundingMode.HALF_UP);
            int yearsRemaining = (life + 1) - year + 1;
            BigDecimal slExpense = openingValue.divide(BigDecimal.valueOf(Math.max(1, yearsRemaining)), 2, RoundingMode.HALF_UP);
            BigDecimal expense = dbExpense.max(slExpense).min(openingValue);

            BigDecimal ending = openingValue.subtract(expense).max(BigDecimal.ZERO);
            schedule.add(new ScheduleLine(year, openingValue, expense, ending, "MACRS 200DB half-year approximation"));
            openingValue = ending;

            if (openingValue.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }

        return schedule;
    }
}

