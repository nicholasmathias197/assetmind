package com.assetmind.infrastructure.persistence;

import com.assetmind.core.domain.Asset;
import com.assetmind.core.domain.AssetClass;
import com.assetmind.core.domain.PaginatedResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AssetRepositoryAdapterTest {

    private SpringDataAssetJpaRepository jpaRepository;
    private AssetRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        jpaRepository = mock(SpringDataAssetJpaRepository.class);
        adapter = new AssetRepositoryAdapter(jpaRepository);
    }

    private AssetEntity sampleEntity() {
        AssetEntity e = new AssetEntity();
        e.setId("A-1");
        e.setDescription("Laptop");
        e.setAssetClass(AssetClass.COMPUTER_EQUIPMENT);
        e.setCostBasis(new BigDecimal("1000"));
        e.setInServiceDate(LocalDate.of(2024, 1, 1));
        e.setUsefulLifeYears(5);
        e.setDeleted(false);
        return e;
    }

    @Test
    void saveDelegatesToJpaAndReturnsDomain() {
        AssetEntity entity = sampleEntity();
        when(jpaRepository.save(any(AssetEntity.class))).thenReturn(entity);

        Asset result = adapter.save(new Asset("A-1", "Laptop", AssetClass.COMPUTER_EQUIPMENT,
                new BigDecimal("1000"), LocalDate.of(2024, 1, 1), 5, false));

        assertEquals("A-1", result.id());
        assertEquals("Laptop", result.description());
        assertEquals(AssetClass.COMPUTER_EQUIPMENT, result.assetClass());
        verify(jpaRepository).save(any(AssetEntity.class));
    }

    @Test
    void findByIdReturnsAssetWhenActive() {
        AssetEntity entity = sampleEntity();
        when(jpaRepository.findByIdActive("A-1")).thenReturn(Optional.of(entity));

        Optional<Asset> result = adapter.findById("A-1");

        assertTrue(result.isPresent());
        assertEquals("A-1", result.get().id());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(jpaRepository.findByIdActive("X")).thenReturn(Optional.empty());

        Optional<Asset> result = adapter.findById("X");

        assertFalse(result.isPresent());
    }

    @Test
    void findAllReturnsActiveAssets() {
        Page<AssetEntity> page = new PageImpl<>(List.of(sampleEntity()));
        when(jpaRepository.findAllActive(any(Pageable.class))).thenReturn(page);

        List<Asset> assets = adapter.findAll();

        assertEquals(1, assets.size());
        assertEquals("A-1", assets.get(0).id());
    }

    @Test
    void findByAssetClassDelegatesToRepository() {
        when(jpaRepository.findByAssetClass(AssetClass.COMPUTER_EQUIPMENT))
                .thenReturn(List.of(sampleEntity()));

        List<Asset> assets = adapter.findByAssetClass(AssetClass.COMPUTER_EQUIPMENT);

        assertEquals(1, assets.size());
    }

    @Test
    void findAllPaginatedReturnsPaginatedResult() {
        AssetEntity entity = sampleEntity();
        Page<AssetEntity> page = new PageImpl<>(List.of(entity), Pageable.ofSize(10), 1);
        when(jpaRepository.findAllActive(any(Pageable.class))).thenReturn(page);

        PaginatedResult<Asset> result = adapter.findAllPaginated(0, 10, "id", "asc");

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(1, result.totalElements());
    }

    @Test
    void findAllPaginatedWithDescSort() {
        Page<AssetEntity> page = new PageImpl<>(List.of(), Pageable.ofSize(10), 0);
        when(jpaRepository.findAllActive(any(Pageable.class))).thenReturn(page);

        PaginatedResult<Asset> result = adapter.findAllPaginated(0, 10, "id", "desc");

        assertNotNull(result);
        assertEquals(0, result.content().size());
    }

    @Test
    void findByAssetClassPaginatedReturnsResults() {
        Page<AssetEntity> page = new PageImpl<>(List.of(sampleEntity()), Pageable.ofSize(10), 1);
        when(jpaRepository.findByAssetClassActive(eq(AssetClass.COMPUTER_EQUIPMENT), any(Pageable.class)))
                .thenReturn(page);

        PaginatedResult<Asset> result = adapter.findByAssetClassPaginated(
                AssetClass.COMPUTER_EQUIPMENT, 0, 10, "id", "asc");

        assertEquals(1, result.content().size());
    }

    @Test
    void deleteSoftDeletesEntity() {
        AssetEntity entity = sampleEntity();
        when(jpaRepository.findByIdActive("A-1")).thenReturn(Optional.of(entity));

        adapter.delete("A-1");

        assertTrue(entity.isDeleted());
        verify(jpaRepository).save(entity);
    }

    @Test
    void deleteDoesNothingWhenNotFound() {
        when(jpaRepository.findByIdActive("X")).thenReturn(Optional.empty());

        adapter.delete("X");

        verify(jpaRepository, never()).save(any());
    }
}
