package com.assetmind.infrastructure.persistence;

import com.assetmind.core.domain.AssetClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SpringDataAssetJpaRepository extends JpaRepository<AssetEntity, String> {
	List<AssetEntity> findByAssetClass(AssetClass assetClass);

	@Query("SELECT a FROM AssetEntity a WHERE a.deleted = false")
	Page<AssetEntity> findAllActive(Pageable pageable);

	@Query("SELECT a FROM AssetEntity a WHERE a.assetClass = ?1 AND a.deleted = false")
	Page<AssetEntity> findByAssetClassActive(AssetClass assetClass, Pageable pageable);

	@Query("SELECT a FROM AssetEntity a WHERE a.id = ?1 AND a.deleted = false")
	Optional<AssetEntity> findByIdActive(String id);
}

