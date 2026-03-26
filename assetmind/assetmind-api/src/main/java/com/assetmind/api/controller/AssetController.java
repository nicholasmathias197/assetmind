package com.assetmind.api.controller;

import com.assetmind.api.dto.AssetResponse;
import com.assetmind.api.dto.CreateAssetRequest;
import com.assetmind.core.domain.Asset;
import com.assetmind.core.domain.AssetClass;
import com.assetmind.core.domain.PaginatedResult;
import com.assetmind.core.service.AssetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssetResponse create(@Valid @RequestBody CreateAssetRequest request) {
        Asset created = assetService.create(new Asset(
                request.id(),
                request.description(),
                request.assetClass(),
                request.costBasis(),
                request.inServiceDate(),
                request.usefulLifeYears(),
                false
        ));
        return toResponse(created);
    }

    @GetMapping("/{id}")
    public AssetResponse findById(@PathVariable String id) {
        Asset asset = assetService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
        return toResponse(asset);
    }

    @PutMapping("/{id}")
    public AssetResponse update(@PathVariable String id, @Valid @RequestBody CreateAssetRequest request) {
        Asset updated = assetService.update(id, new Asset(
                        id,
                        request.description(),
                        request.assetClass(),
                        request.costBasis(),
                        request.inServiceDate(),
                        request.usefulLifeYears(),
                        false
                ))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
        return toResponse(updated);
    }

    @GetMapping
    public List<AssetResponse> list(@RequestParam(required = false) AssetClass assetClass) {
        return assetService.findAll(Optional.ofNullable(assetClass)).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/page")
    public PaginatedAssetResponse listPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) AssetClass assetClass) {
        PaginatedResult<Asset> result = assetService.findAllPaginated(page, size, sortBy, sortDirection, Optional.ofNullable(assetClass));
        return new PaginatedAssetResponse(
                result.content().stream().map(this::toResponse).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        assetService.delete(id);
    }

    private AssetResponse toResponse(Asset asset) {
        return new AssetResponse(
                asset.id(),
                asset.description(),
                asset.assetClass(),
                asset.costBasis(),
                asset.inServiceDate(),
                asset.usefulLifeYears()
        );
    }

    public static class PaginatedAssetResponse {
        public final List<AssetResponse> content;
        public final int page;
        public final int size;
        public final long totalElements;
        public final int totalPages;

        public PaginatedAssetResponse(List<AssetResponse> content, int page, int size, long totalElements, int totalPages) {
            this.content = content;
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }
    }
}

