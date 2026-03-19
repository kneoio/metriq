package com.semantyca.metriq.dto;


import com.semantyca.mixpla.model.cnst.StreamStatus;

import java.time.LocalDateTime;

public record StatusChangeRecordDTO(LocalDateTime timestamp, StreamStatus oldStatus,
                                    StreamStatus newStatus) {
}
