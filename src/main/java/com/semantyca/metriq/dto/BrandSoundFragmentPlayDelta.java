package com.semantyca.metriq.dto;

import java.util.UUID;

public record BrandSoundFragmentPlayDelta(UUID brandId, UUID soundFragmentId, int count) {
}
