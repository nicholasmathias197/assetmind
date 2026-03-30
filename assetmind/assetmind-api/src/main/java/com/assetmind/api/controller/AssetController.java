package com.assetmind.api.controller;

import com.assetmind.api.dto.AssetResponse;
import com.assetmind.api.dto.CreateAssetRequest;
import com.assetmind.core.domain.Asset;
import com.assetmind.core.domain.AssetClass;
import com.assetmind.core.domain.PaginatedResult;
import com.assetmind.core.service.AssetService;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

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

    @GetMapping(value = "/export", produces = EXCEL_CONTENT_TYPE)
    public ResponseEntity<byte[]> exportToExcel(@RequestParam(required = false) AssetClass assetClass) {
        List<Asset> assets = assetService.findAll(Optional.ofNullable(assetClass));
        try (XSSFWorkbook workbook = createAssetWorkbook(assets, false); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return excelResponse(outputStream.toByteArray(), "assets.xlsx");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate Excel export", e);
        }
    }

    @GetMapping(value = "/export/template", produces = EXCEL_CONTENT_TYPE)
    public ResponseEntity<byte[]> exportTemplate() {
        try (XSSFWorkbook workbook = createAssetWorkbook(List.of(), true); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return excelResponse(outputStream.toByteArray(), "assets-template.xlsx");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate Excel template", e);
        }
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportAssetsResponse importFromExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        }

        int imported = 0;
        int failed = 0;
        int totalRows = 0;
        List<String> errors = new ArrayList<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();
            for (int rowIndex = 1; rowIndex <= lastRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || rowIsEmpty(row)) {
                    continue;
                }
                totalRows++;
                try {
                    String id = getRequiredStringCell(row, 0, "id");
                    String description = getRequiredStringCell(row, 1, "description");
                    AssetClass parsedAssetClass = AssetClass.valueOf(getRequiredStringCell(row, 2, "assetClass").trim().toUpperCase());
                    BigDecimal costBasis = getRequiredDecimalCell(row, 3, "costBasis");
                    LocalDate inServiceDate = getRequiredDateCell(row, 4, "inServiceDate");
                    int usefulLifeYears = getRequiredIntegerCell(row, 5, "usefulLifeYears");

                    Asset asset = new Asset(
                            id,
                            description,
                            parsedAssetClass,
                            costBasis,
                            inServiceDate,
                            usefulLifeYears,
                            false
                    );

                    if (assetService.findById(id).isPresent()) {
                        assetService.update(id, asset);
                    } else {
                        assetService.create(asset);
                    }
                    imported++;
                } catch (Exception ex) {
                    failed++;
                    errors.add("Row " + (rowIndex + 1) + ": " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read uploaded Excel file", e);
        }

        return new ImportAssetsResponse(totalRows, imported, failed, errors);
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

    private XSSFWorkbook createAssetWorkbook(List<Asset> assets, boolean includeSampleRow) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Assets");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("id");
        header.createCell(1).setCellValue("description");
        header.createCell(2).setCellValue("assetClass");
        header.createCell(3).setCellValue("costBasis");
        header.createCell(4).setCellValue("inServiceDate");
        header.createCell(5).setCellValue("usefulLifeYears");

        for (int i = 0; i < assets.size(); i++) {
            Asset asset = assets.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(asset.id());
            row.createCell(1).setCellValue(asset.description());
            row.createCell(2).setCellValue(asset.assetClass().name());
            row.createCell(3).setCellValue(asset.costBasis().doubleValue());
            row.createCell(4).setCellValue(asset.inServiceDate().toString());
            row.createCell(5).setCellValue(asset.usefulLifeYears());
        }

        if (includeSampleRow) {
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("ASSET-001");
            sample.createCell(1).setCellValue("Example laptop");
            sample.createCell(2).setCellValue(AssetClass.COMPUTER_EQUIPMENT.name());
            sample.createCell(3).setCellValue(1200.00);
            sample.createCell(4).setCellValue("2026-01-15");
            sample.createCell(5).setCellValue(5);
        }

        for (int c = 0; c < 6; c++) {
            sheet.autoSizeColumn(c);
        }

        return workbook;
    }

    private ResponseEntity<byte[]> excelResponse(byte[] bytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE))
                .body(bytes);
    }

    private boolean rowIsEmpty(Row row) {
        for (int i = 0; i < 6; i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() != CellType.BLANK && !cell.toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getRequiredStringCell(Row row, int columnIndex, String columnName) {
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            throw new IllegalArgumentException("Missing value for " + columnName);
        }
        String value;
        if (cell.getCellType() == CellType.STRING) {
            value = cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            value = BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        } else {
            value = cell.toString();
        }
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing value for " + columnName);
        }
        return value.trim();
    }

    private BigDecimal getRequiredDecimalCell(Row row, int columnIndex, String columnName) {
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            throw new IllegalArgumentException("Missing value for " + columnName);
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            return new BigDecimal(cell.getStringCellValue().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid numeric value for " + columnName);
        }
    }

    private int getRequiredIntegerCell(Row row, int columnIndex, String columnName) {
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            throw new IllegalArgumentException("Missing value for " + columnName);
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }
            return Integer.parseInt(cell.getStringCellValue().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid integer value for " + columnName);
        }
    }

    private LocalDate getRequiredDateCell(Row row, int columnIndex, String columnName) {
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            throw new IllegalArgumentException("Missing value for " + columnName);
        }

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        try {
            return LocalDate.parse(cell.toString().trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date value for " + columnName + " (expected YYYY-MM-DD)");
        }
    }

    public record ImportAssetsResponse(int totalRows, int imported, int failed, List<String> errors) {
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

