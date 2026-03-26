package com.assetmind.core.service;

import com.assetmind.core.domain.Asset;
import com.assetmind.core.domain.AssetClass;
import com.assetmind.core.domain.PaginatedResult;

import java.util.List;
import java.util.Optional;

public interface AssetService {
    Asset create(Asset asset);

    Optional<Asset> findById(String id);

    Optional<Asset> update(String id, Asset asset);

    List<Asset> findAll(Optional<AssetClass> assetClass);

    PaginatedResult<Asset> findAllPaginated(int page, int size, String sortBy, String sortDirection, Optional<AssetClass> assetClass);

    void delete(String id);
}
