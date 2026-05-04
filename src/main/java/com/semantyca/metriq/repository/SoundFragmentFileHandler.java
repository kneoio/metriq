package com.semantyca.metriq.repository;

import com.semantyca.core.model.FileMetadata;
import com.semantyca.core.repository.IFileStorage;
import com.semantyca.core.repository.exception.attachment.FileRetrievalFailureException;
import com.semantyca.core.repository.exception.attachment.MissingFileRecordException;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

@ApplicationScoped
public class SoundFragmentFileHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoundFragmentFileHandler.class);

    private final PgPool client;
    private final IFileStorage fileStorage;

    @Inject
    public SoundFragmentFileHandler(PgPool client, @Named("hetzner") IFileStorage fileStorage) {
        this.client = client;
        this.fileStorage = fileStorage;
    }

    public Uni<FileMetadata> getFirstFile(UUID id) {
        String sql = "SELECT f.file_key FROM _files f WHERE f.parent_id = $1";
        return retrieveFileFromStorage(id, sql, Tuple.of(id));
    }

    private Uni<FileMetadata> retrieveFileFromStorage(UUID id, String sql, Tuple parameters) {
        return client.preparedQuery(sql)
                .execute(parameters)
                .onFailure().invoke(failure -> LOGGER.error("Database query failed for ID: {}", id, failure))
                .onItem().transformToUni(rows -> {
                    if (rows.rowCount() == 0) {
                        LOGGER.warn("No file record found for ID: {}", id);
                        return Uni.createFrom().failure(new MissingFileRecordException("File not found: " + id));
                    }

                    String fileKey = rows.iterator().next().getString("file_key");
                    boolean hasNonAscii = fileKey != null && fileKey.chars().anyMatch(c -> c > 127);
                    if (hasNonAscii) {
                        StringBuilder codePoints = new StringBuilder();
                        for (char c : fileKey.toCharArray()) {
                            if (c > 127) codePoints.append(String.format("[U+%04X '%c']", (int) c, c));
                        }
                        String nonAsciiInfo = codePoints.toString();
                        String utf8Bytes = Arrays.toString(fileKey.getBytes(StandardCharsets.UTF_8));
                        LOGGER.warn("file_key contains non-ASCII characters - ID: {}, key: '{}', non-ASCII: {}, UTF-8 bytes: {}",
                                id, fileKey, nonAsciiInfo, utf8Bytes);
                    } else {
                        LOGGER.info("Retrieving file - ID: {}, key: '{}'", id, fileKey);
                    }

                    return fileStorage.getFileStream(fileKey)
                            .onItem().invoke(file -> LOGGER.info("File retrieval successful - ID: {}, key: '{}', contentLength: {}", id, fileKey, file.getContentLength()))
                            .onFailure().recoverWithUni(ex -> {
                                LOGGER.error("File retrieval failed - ID: {}, key: '{}', errorType: {}, errorMsg: {}",
                                        id, fileKey, ex.getClass().getName(), ex.getMessage(), ex);
                                String errorMsg = String.format("File retrieval failed - ID: %s, Key: %s, Error: %s",
                                        id, fileKey, ex.getClass().getSimpleName());
                                FileRetrievalFailureException fnf = new FileRetrievalFailureException(errorMsg);
                                fnf.initCause(ex);
                                return Uni.createFrom().<FileMetadata>failure(fnf);
                            });
                });
    }

    public Uni<Void> deleteFile(String key) {
        return fileStorage.deleteFile(key)
                .onItem().transformToUni(v -> {
                    String deleteSql = "DELETE FROM _files WHERE file_key = $1";
                    return client.preparedQuery(deleteSql)
                            .execute(Tuple.of(key))
                            .onItem().transform(result -> {
                                LOGGER.debug("Successfully deleted file metadata with key: {}", key);
                                return null;
                            });
                })
                .onFailure().transform(ex -> {
                    LOGGER.error("Failed to delete file with key: {}", key, ex);
                    return new RuntimeException("Failed to delete file", ex);
                }).replaceWithVoid();
    }
}
