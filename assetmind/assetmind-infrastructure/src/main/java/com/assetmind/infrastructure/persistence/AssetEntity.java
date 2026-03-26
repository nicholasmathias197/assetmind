package com.assetmind.infrastructure.persistence;

import com.assetmind.core.domain.AssetClass;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "assets")
public class AssetEntity {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 64)
    private AssetClass assetClass;

    @Column(name = "cost_basis", nullable = false, precision = 19, scale = 2)
    private BigDecimal costBasis;

    @Column(name = "in_service_date", nullable = false)
    private LocalDate inServiceDate;

    @Column(name = "useful_life_years", nullable = false)
    private int usefulLifeYears;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AssetClass getAssetClass() {
        return assetClass;
    }

    public void setAssetClass(AssetClass assetClass) {
        this.assetClass = assetClass;
    }

    public BigDecimal getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(BigDecimal costBasis) {
        this.costBasis = costBasis;
    }

    public LocalDate getInServiceDate() {
        return inServiceDate;
    }

    public void setInServiceDate(LocalDate inServiceDate) {
        this.inServiceDate = inServiceDate;
    }

    public int getUsefulLifeYears() {
        return usefulLifeYears;
    }

    public void setUsefulLifeYears(int usefulLifeYears) {
        this.usefulLifeYears = usefulLifeYears;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}

