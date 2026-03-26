package com.assetmind.infrastructure.persistence;

import com.assetmind.core.domain.Asset;
import com.assetmind.core.domain.AssetClass;
import com.assetmind.core.domain.PaginatedResult;
import com.assetmind.core.port.AssetRepositoryPort;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Optional;

@Repository
public class AssetRepositoryAdapter implements AssetRepositoryPort {

    private final SpringDataAssetJpaRepository jpaRepository;

    public AssetRepositoryAdapter(SpringDataAssetJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Asset save(Asset asset) {
        AssetEntity entity = toEntity(asset);
        AssetEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Asset> findById(String id) {
        return jpaRepository.findByIdActive(id).map(this::toDomain);
    }

    @Override
    public List<Asset> findAll() {
        return jpaRepository.findAllActive(PageRequest.of(0, Integer.MAX_VALUE)).getContent().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Asset> findByAssetClass(AssetClass assetClass) {
        return jpaRepository.findByAssetClass(assetClass).stream().map(this::toDomain).toList();
    }

    @Override
    public PaginatedResult<Asset> findAllPaginated(int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<AssetEntity> pageResult = jpaRepository.findAllActive(pageable);
        return new PaginatedResult<>(
                pageResult.getContent().stream().map(this::toDomain).toList(),
                page,
                size,
                pageResult.getTotalElements()
        );
    }

    @Override
    public PaginatedResult<Asset> findByAssetClassPaginated(AssetClass assetClass, int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<AssetEntity> pageResult = jpaRepository.findByAssetClassActive(assetClass, pageable);
        return new PaginatedResult<>(
                pageResult.getContent().stream().map(this::toDomain).toList(),
                page,
                size,
                pageResult.getTotalElements()
        );
    }

    @Override
    public void delete(String id) {
        jpaRepository.findByIdActive(id).ifPresent(entity -> {
            entity.setDeleted(true);
            jpaRepository.save(entity);
        });
    }

    private AssetEntity toEntity(Asset asset) {
        AssetEntity entity = new AssetEntity();
        entity.setId(asset.id());
        entity.setDescription(asset.description());
        entity.setAssetClass(asset.assetClass());
        entity.setCostBasis(asset.costBasis());
        entity.setInServiceDate(asset.inServiceDate());
        entity.setUsefulLifeYears(asset.usefulLifeYears());
        entity.setDeleted(asset.deleted());
        return entity;
    }

    private Asset toDomain(AssetEntity entity) {
        return new Asset(
                entity.getId(),
                entity.getDescription(),
                entity.getAssetClass(),
                entity.getCostBasis(),
                entity.getInServiceDate(),
                entity.getUsefulLifeYears(),
                entity.isDeleted()
        );
    }
}

