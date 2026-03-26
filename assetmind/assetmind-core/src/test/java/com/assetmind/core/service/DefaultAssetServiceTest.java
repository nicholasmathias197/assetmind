package com.assetmind.core.service;

import com.assetmind.core.domain.Asset;
import com.assetmind.core.domain.AssetClass;
import com.assetmind.core.domain.PaginatedResult;
import com.assetmind.core.port.AssetRepositoryPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultAssetServiceTest {

    @Test
    void shouldCreateAndFindAsset() {
        AssetRepositoryPort inMemoryRepo = new AssetRepositoryPort() {
            private final Map<String, Asset> store = new ConcurrentHashMap<>();

            @Override
            public Asset save(Asset asset) {
                store.put(asset.id(), asset);
                return asset;
            }

            @Override
            public Optional<Asset> findById(String id) {
                Asset found = store.get(id);
                return found != null && !found.deleted() ? Optional.of(found) : Optional.empty();
            }

            @Override
            public List<Asset> findAll() {
                return store.values().stream()
                        .filter(a -> !a.deleted())
                        .toList();
            }

            @Override
            public List<Asset> findByAssetClass(AssetClass assetClass) {
                return store.values().stream()
                        .filter(a -> !a.deleted() && a.assetClass() == assetClass)
                        .toList();
            }

            @Override
            public PaginatedResult<Asset> findAllPaginated(int page, int size, String sortBy, String sortDirection) {
                List<Asset> active = store.values().stream()
                        .filter(a -> !a.deleted())
                        .toList();
                return new PaginatedResult<>(active, page, size, active.size());
            }

            @Override
            public PaginatedResult<Asset> findByAssetClassPaginated(AssetClass assetClass, int page, int size, String sortBy, String sortDirection) {
                List<Asset> active = store.values().stream()
                        .filter(a -> !a.deleted() && a.assetClass() == assetClass)
                        .toList();
                return new PaginatedResult<>(active, page, size, active.size());
            }

            @Override
            public void delete(String id) {
                Optional<Asset> found = store.values().stream()
                        .filter(a -> a.id().equals(id))
                        .findFirst();
                if (found.isPresent()) {
                    Asset asset = found.get();
                    store.put(id, new Asset(
                            asset.id(),
                            asset.description(),
                            asset.assetClass(),
                            asset.costBasis(),
                            asset.inServiceDate(),
                            asset.usefulLifeYears(),
                            true
                    ));
                }
            }
        };

        DefaultAssetService service = new DefaultAssetService(inMemoryRepo);
        Asset asset = new Asset(
                "asset-abc",
                "MacBook Pro",
                AssetClass.COMPUTER_EQUIPMENT,
                new BigDecimal("3200.00"),
                LocalDate.of(2026, 3, 1),
                5,
                false
        );

        service.create(asset);
        Optional<Asset> found = service.findById("asset-abc");

        assertTrue(found.isPresent());
        assertEquals("MacBook Pro", found.get().description());

        Optional<Asset> updated = service.update("asset-abc", new Asset(
                "asset-abc",
                "MacBook Pro M4",
                AssetClass.COMPUTER_EQUIPMENT,
                new BigDecimal("3400.00"),
                LocalDate.of(2026, 3, 1),
                5,
                false
        ));

        assertTrue(updated.isPresent());
        assertEquals("MacBook Pro M4", updated.get().description());

        List<Asset> allAssets = service.findAll(Optional.empty());
        List<Asset> filteredAssets = service.findAll(Optional.of(AssetClass.COMPUTER_EQUIPMENT));
        List<Asset> vehicleAssets = service.findAll(Optional.of(AssetClass.VEHICLE));

        assertEquals(1, allAssets.size());
        assertEquals(1, filteredAssets.size());
        assertTrue(filteredAssets.getFirst().description().contains("M4"));
        assertTrue(vehicleAssets.isEmpty());

        Optional<Asset> missingUpdate = service.update("missing", asset);
        assertFalse(missingUpdate.isPresent());

        service.delete("asset-abc");
        Optional<Asset> deletedAsset = service.findById("asset-abc");
        assertFalse(deletedAsset.isPresent());

        PaginatedResult<Asset> paginatedResult = service.findAllPaginated(0, 20, "id", "asc", Optional.empty());
        assertEquals(0, paginatedResult.content().size());
    }
}

