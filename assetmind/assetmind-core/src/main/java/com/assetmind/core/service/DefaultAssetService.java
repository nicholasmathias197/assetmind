package com.assetmind.core.service;

import com.assetmind.core.domain.Asset;
import com.assetmind.core.domain.AssetClass;
import com.assetmind.core.domain.PaginatedResult;
import com.assetmind.core.port.AssetRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DefaultAssetService implements AssetService {

    private final AssetRepositoryPort assetRepositoryPort;

    public DefaultAssetService(AssetRepositoryPort assetRepositoryPort) {
        this.assetRepositoryPort = assetRepositoryPort;
    }

    @Override
    public Asset create(Asset asset) {
        return assetRepositoryPort.save(asset);
    }

    @Override
    public Optional<Asset> findById(String id) {
        return assetRepositoryPort.findById(id);
    }

    @Override
    public Optional<Asset> update(String id, Asset asset) {
        if (assetRepositoryPort.findById(id).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(assetRepositoryPort.save(asset));
    }

    @Override
    public List<Asset> findAll(Optional<AssetClass> assetClass) {
        return assetClass
                .map(assetRepositoryPort::findByAssetClass)
                .orElseGet(assetRepositoryPort::findAll);
    }

    @Override
    public PaginatedResult<Asset> findAllPaginated(int page, int size, String sortBy, String sortDirection, Optional<AssetClass> assetClass) {
        return assetClass
                .map(cls -> assetRepositoryPort.findByAssetClassPaginated(cls, page, size, sortBy, sortDirection))
                .orElseGet(() -> assetRepositoryPort.findAllPaginated(page, size, sortBy, sortDirection));
    }

    @Override
    public void delete(String id) {
        assetRepositoryPort.delete(id);
    }
}

