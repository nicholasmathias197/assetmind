package com.assetmind.core.port;

import com.assetmind.core.domain.Asset;
import com.assetmind.core.domain.AssetClass;
import com.assetmind.core.domain.PaginatedResult;

import java.util.List;
import java.util.Optional;

public interface AssetRepositoryPort {
    Asset save(Asset asset);

    Optional<Asset> findById(String id);

    List<Asset> findAll();

    List<Asset> findByAssetClass(AssetClass assetClass);

    PaginatedResult<Asset> findAllPaginated(int page, int size, String sortBy, String sortDirection);

    PaginatedResult<Asset> findByAssetClassPaginated(AssetClass assetClass, int page, int size, String sortBy, String sortDirection);

    void delete(String id);
}

