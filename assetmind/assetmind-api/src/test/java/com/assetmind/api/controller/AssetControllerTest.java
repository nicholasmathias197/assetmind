package com.assetmind.api.controller;

import com.assetmind.core.domain.Asset;
import com.assetmind.core.domain.AssetClass;
import com.assetmind.core.domain.PaginatedResult;
import com.assetmind.core.service.AssetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.assetmind.api.dto.AssetResponse;
import com.assetmind.api.dto.CreateAssetRequest;

class AssetControllerTest {

    private AssetService assetService;
    private AssetController controller;

    @BeforeEach
    void setUp() {
        assetService = mock(AssetService.class);
        controller = new AssetController(assetService);
    }

    private Asset sampleAsset() {
        return new Asset("A-1", "Dell Laptop", AssetClass.COMPUTER_EQUIPMENT,
                new BigDecimal("1200"), LocalDate.of(2024, 1, 1), 5, false);
    }

    @Test
    void createReturnsCreatedAsset() {
        when(assetService.create(any(Asset.class))).thenReturn(sampleAsset());

        CreateAssetRequest request = new CreateAssetRequest("A-1", "Dell Laptop",
                AssetClass.COMPUTER_EQUIPMENT, new BigDecimal("1200"), LocalDate.of(2024, 1, 1), 5);

        AssetResponse response = controller.create(request);

        assertEquals("A-1", response.id());
        assertEquals("Dell Laptop", response.description());
        assertEquals(AssetClass.COMPUTER_EQUIPMENT, response.assetClass());
    }

    @Test
    void findByIdReturnsAsset() {
        when(assetService.findById("A-1")).thenReturn(Optional.of(sampleAsset()));

        AssetResponse response = controller.findById("A-1");

        assertEquals("A-1", response.id());
    }

    @Test
    void findByIdThrows404WhenNotFound() {
        when(assetService.findById("X")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> controller.findById("X"));
    }

    @Test
    void updateReturnsUpdatedAsset() {
        when(assetService.update(eq("A-1"), any(Asset.class))).thenReturn(Optional.of(sampleAsset()));

        CreateAssetRequest request = new CreateAssetRequest("A-1", "Dell Laptop",
                AssetClass.COMPUTER_EQUIPMENT, new BigDecimal("1200"), LocalDate.of(2024, 1, 1), 5);

        AssetResponse response = controller.update("A-1", request);

        assertEquals("A-1", response.id());
    }

    @Test
    void updateThrows404WhenNotFound() {
        when(assetService.update(eq("X"), any(Asset.class))).thenReturn(Optional.empty());

        CreateAssetRequest request = new CreateAssetRequest("X", "Missing",
                AssetClass.OTHER, new BigDecimal("100"), LocalDate.of(2024, 1, 1), 3);

        assertThrows(ResponseStatusException.class, () -> controller.update("X", request));
    }

    @Test
    void listReturnsAllAssets() {
        when(assetService.findAll(Optional.empty())).thenReturn(List.of(sampleAsset()));

        List<AssetResponse> result = controller.list(null);

        assertEquals(1, result.size());
        assertEquals("A-1", result.get(0).id());
    }

    @Test
    void listFiltersByAssetClass() {
        when(assetService.findAll(Optional.of(AssetClass.COMPUTER_EQUIPMENT)))
                .thenReturn(List.of(sampleAsset()));

        List<AssetResponse> result = controller.list(AssetClass.COMPUTER_EQUIPMENT);

        assertEquals(1, result.size());
    }

    @Test
    void listPaginatedReturnsPagedResult() {
        PaginatedResult<Asset> paginated = new PaginatedResult<>(
                List.of(sampleAsset()), 0, 10, 1);
        when(assetService.findAllPaginated(0, 10, "id", "asc", Optional.empty()))
                .thenReturn(paginated);

        AssetController.PaginatedAssetResponse response = controller.listPaginated(0, 10, "id", "asc", null);

        assertEquals(1, response.content.size());
        assertEquals(0, response.page);
        assertEquals(10, response.size);
        assertEquals(1, response.totalElements);
    }

    @Test
    void deleteCallsService() {
        controller.delete("A-1");

        verify(assetService).delete("A-1");
    }

    @Test
    void exportToExcelReturnsBytes() {
        when(assetService.findAll(Optional.empty())).thenReturn(List.of(sampleAsset()));

        var response = controller.exportToExcel(null);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                response.getHeaders().getContentType().toString());
    }

    @Test
    void exportTemplateReturnsBytes() {
        var response = controller.exportTemplate();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }

    @Test
    void importAssetsResponseRecord() {
        var response = new AssetController.ImportAssetsResponse(10, 8, 2, List.of("Row 3: error"));
        assertEquals(10, response.totalRows());
        assertEquals(8, response.imported());
        assertEquals(2, response.failed());
        assertEquals(1, response.errors().size());
    }

    @Test
    void paginatedAssetResponseFields() {
        var response = new AssetController.PaginatedAssetResponse(List.of(), 1, 20, 100, 5);
        assertEquals(1, response.page);
        assertEquals(20, response.size);
        assertEquals(100, response.totalElements);
        assertEquals(5, response.totalPages);
    }
}
