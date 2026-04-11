package com.semantyca.metriq.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semantyca.core.model.FileMetadata;
import com.semantyca.core.model.cnst.FileStorageType;
import com.semantyca.core.model.user.IUser;
import com.semantyca.core.model.user.SuperUser;
import com.semantyca.core.repository.exception.DocumentHasNotFoundException;
import com.semantyca.core.repository.exception.DocumentModificationAccessException;
import com.semantyca.core.repository.rls.RLSRepository;
import com.semantyca.core.repository.table.EntityData;
import com.semantyca.metriq.repository.soundfragment.SoundFragmentFileHandler;
import com.semantyca.mixpla.model.cnst.PlaylistItemType;
import com.semantyca.mixpla.model.cnst.SourceType;
import com.semantyca.mixpla.model.soundfragment.SoundFragment;
import com.semantyca.mixpla.repository.MixplaNameResolver;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.SqlResult;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.semantyca.mixpla.repository.MixplaNameResolver.SOUND_FRAGMENT;

@ApplicationScoped
public class SoundFragmentRepository extends SoundFragmentRepositoryAbstract {
    private static final EntityData entityData = MixplaNameResolver.create().getEntityNames(SOUND_FRAGMENT);
    private final SoundFragmentFileHandler fileHandler;

    public SoundFragmentRepository(SoundFragmentFileHandler fileHandler) {
        super();
        this.fileHandler = null;
    }

    @Inject
    public SoundFragmentRepository(PgPool client,
                                   ObjectMapper mapper,
                                   RLSRepository rlsRepository,
                                   SoundFragmentFileHandler fileHandler) {
        super(client, mapper, rlsRepository);
        this.fileHandler = fileHandler;
    }

    public Uni<SoundFragment> findById(UUID uuid) {
        String sql = "SELECT * FROM " + entityData.getTableName() + " WHERE id = $1";

        return client.preparedQuery(sql)
                .execute(Tuple.of(uuid))
                .onItem().transform(RowSet::iterator)
                .onItem().transformToUni(iterator -> {
                    if (iterator.hasNext()) {
                        Row row = iterator.next();
                        return from(row, false, false, false);
                    } else {
                        return Uni.createFrom().failure(new DocumentHasNotFoundException(uuid));
                    }
                });
    }

    public Uni<List<UUID>> findExpiredFragments() {
        String sql = "SELECT id FROM " + entityData.getTableName() + " " +
                "WHERE expires_at IS NOT NULL AND expires_at < NOW() AND archived = 0";

        return client.query(sql)
                .execute()
                .onItem().transformToMulti(rows -> Multi.createFrom().iterable(rows))
                .onItem().transform(row -> row.getUUID("id"))
                .collect().asList();
    }

    public Uni<Integer> hardDelete(UUID uuid) {
        return findById(uuid)
                .onFailure(DocumentHasNotFoundException.class).recoverWithItem(() -> {
                    LOGGER.warn("SoundFragment {} not found, may already be deleted", uuid);
                    return null;
                })
                .onItem().transformToUni(doc -> {
                    if (doc == null) {
                        return Uni.createFrom().item(0);
                    }
                    return deleteStorageFiles(uuid)
                            .onItem().transformToUni(v -> deleteDatabaseRecords(uuid));
                });
    }

    protected Uni<SoundFragment> from(Row row, boolean includeGenres, boolean includeFiles, boolean includeLabels) {
        SoundFragment doc = new SoundFragment();
        setDefaultFields(doc, row);
        doc.setSource(SourceType.valueOf(row.getString("source")));
        doc.setStatus(row.getInteger("status"));
        doc.setType(PlaylistItemType.valueOf(row.getString("type")));
        doc.setTitle(row.getString("title"));
        doc.setArtist(row.getString("artist"));
        doc.setAlbum(row.getString("album"));

        if (row.getValue("length") != null) {
            Long lengthMillis = row.getLong("length");
            doc.setLength(Duration.ofMillis(lengthMillis));
        }
        doc.setArchived(row.getInteger("archived"));
        doc.setSlugName(row.getString("slug_name"));
        doc.setDescription(row.getString("description"));
        doc.setExpiresAt(row.getLocalDateTime("expires_at"));

        Uni<SoundFragment> uni = Uni.createFrom().item(doc);

        if (includeGenres) {
            uni = uni.chain(d -> loadGenres(d.getId()).onItem().transform(genres -> {
                d.setGenres(genres);
                return d;
            }));
        } else {
            doc.setGenres(List.of());
        }

        if (includeLabels) {
            uni = uni.chain(d -> loadLabels(d.getId()).onItem().transform(labels -> {
                d.setLabels(labels);
                return d;
            }));
        } else {
            doc.setLabels(List.of());
        }

        if (includeFiles) {
            String fileQuery = "SELECT id, reg_date, last_mod_date, parent_table, parent_id, archived, archived_date, storage_type, mime_type, slug_name, file_original_name, file_key FROM _files WHERE parent_table = '" + entityData.getTableName() + "' AND parent_id = $1 AND archived = 0 ORDER BY reg_date ASC";
            uni = uni.chain(soundFragment -> client.preparedQuery(fileQuery)
                    .execute(Tuple.of(soundFragment.getId()))
                    .onItem().transform(rowSet -> {
                        List<FileMetadata> files = new ArrayList<>();
                        for (Row fileRow : rowSet) {
                            FileMetadata fileMetadata = new FileMetadata();
                            fileMetadata.setId(fileRow.getLong("id"));
                            fileMetadata.setRegDate(fileRow.getLocalDateTime("reg_date").atZone(ZoneId.systemDefault()));
                            fileMetadata.setLastModifiedDate(fileRow.getLocalDateTime("last_mod_date").atZone(ZoneId.systemDefault()));
                            fileMetadata.setParentTable(fileRow.getString("parent_table"));
                            fileMetadata.setParentId(fileRow.getUUID("parent_id"));
                            fileMetadata.setArchived(fileRow.getInteger("archived"));
                            if (fileRow.getLocalDateTime("archived_date") != null)
                                fileMetadata.setArchivedDate(fileRow.getLocalDateTime("archived_date"));
                            fileMetadata.setFileStorageType(FileStorageType.valueOf(fileRow.getString("storage_type")));
                            fileMetadata.setMimeType(fileRow.getString("mime_type"));
                            fileMetadata.setSlugName(fileRow.getString("slug_name"));
                            fileMetadata.setFileOriginalName(fileRow.getString("file_original_name"));
                            fileMetadata.setFileKey(fileRow.getString("file_key"));
                            files.add(fileMetadata);
                        }
                        soundFragment.setFileMetadataList(files);
                        if (files.isEmpty()) markAsCorrupted(soundFragment.getId()).subscribe().with(r -> {}, e -> {});
                        return soundFragment;
                    }));
        } else {
            doc.setFileMetadataList(List.of());
        }

        return uni;
    }


    private Uni<List<UUID>> loadLabels(UUID soundFragmentId) {
        String sql = "SELECT label_id FROM kneobroadcaster__sound_fragment_labels WHERE id = $1";
        return client.preparedQuery(sql)
                .execute(Tuple.of(soundFragmentId))
                .onItem().transformToMulti(rows -> Multi.createFrom().iterable(rows))
                .onItem().transform(row -> row.getUUID("label_id"))
                .collect().asList();
    }


    private Uni<List<UUID>> loadGenres(UUID soundFragmentId) {
        String sql = "SELECT g.id FROM __genres g " +
                "JOIN kneobroadcaster__sound_fragment_genres sfg ON g.id = sfg.genre_id " +
                "WHERE sfg.sound_fragment_id = $1 ORDER BY g.identifier";

        return client.preparedQuery(sql)
                .execute(Tuple.of(soundFragmentId))
                .onItem().transformToMulti(rows -> Multi.createFrom().iterable(rows))
                .onItem().transform(row -> row.getUUID("id"))
                .collect().asList();
    }

    public Uni<Integer> markAsCorrupted(UUID uuid) {
        IUser user = SuperUser.build();
        return rlsRepository.findById(entityData.getRlsName(), user.getId(), uuid)
                .onItem().transformToUni(permissions -> {
                    if (!permissions[0]) {
                        return Uni.createFrom().failure(new DocumentModificationAccessException(
                                "User does not have edit permission", user.getUserName(), uuid));
                    }

                    String sql = String.format("UPDATE %s SET archived = -1, last_mod_date = $1, last_mod_user = $2 WHERE id = $3",
                            entityData.getTableName());
                    return client.preparedQuery(sql)
                            .execute(Tuple.of(ZonedDateTime.now(ZoneOffset.UTC).toLocalDateTime(), user.getId(), uuid))
                            .onItem().transform(SqlResult::rowCount);
                });
    }

    private Uni<Void> deleteStorageFiles(UUID uuid) {
        String getKeysSql = "SELECT file_key FROM _files WHERE parent_id = $1";
        return client.preparedQuery(getKeysSql).execute(Tuple.of(uuid))
                .onItem().transformToUni(rows -> {
                    List<String> keysToDelete = new ArrayList<>();
                    rows.forEach(row -> {
                        String key = row.getString("file_key");
                        if (key != null && !key.isBlank()) {
                            keysToDelete.add(key);
                        }
                    });

                    List<Uni<Void>> deleteFileUnis = keysToDelete.stream()
                            .map(key -> {
                                        assert fileHandler != null;
                                        return fileHandler.deleteFile(key)
                                                .onFailure().recoverWithUni(e -> {
                                                    LOGGER.error("Failed to delete file {} from storage for SoundFragment {}. DB record deletion will proceed.", key, uuid, e);
                                                    return Uni.createFrom().voidItem();
                                                });
                                    }
                            ).collect(Collectors.toList());

                    if (deleteFileUnis.isEmpty()) {
                        return Uni.createFrom().voidItem();
                    }
                    return Uni.combine().all().unis(deleteFileUnis).discardItems();
                });
    }

    private Uni<Integer> deleteDatabaseRecords(UUID uuid) {
        return client.withTransaction(tx -> {
            String getContributionIdsSql = "SELECT id FROM kneobroadcaster__contributions WHERE sound_fragment_id = $1";
            String deleteAgreementsSql = "DELETE FROM kneobroadcaster__upload_agreements WHERE contribution_id = ANY($1)";
            String deleteContributionsSql = "DELETE FROM kneobroadcaster__contributions WHERE sound_fragment_id = $1";
            String deleteGenresSql = "DELETE FROM kneobroadcaster__sound_fragment_genres WHERE sound_fragment_id = $1";
            String deleteRlsSql = String.format("DELETE FROM %s WHERE entity_id = $1", entityData.getRlsName());
            String deleteFilesSql = "DELETE FROM _files WHERE parent_id = $1";
            String deleteDocSql = String.format("DELETE FROM %s WHERE id = $1", entityData.getTableName());

            return tx.preparedQuery(getContributionIdsSql).execute(Tuple.of(uuid))
                    .onItem().transformToUni(rows -> {
                        List<UUID> contributionIds = new ArrayList<>();
                        rows.forEach(row -> contributionIds.add(row.getUUID("id")));

                        if (contributionIds.isEmpty()) {
                            return Uni.createFrom().voidItem();
                        }

                        return tx.preparedQuery(deleteAgreementsSql)
                                .execute(Tuple.of(contributionIds.toArray(new UUID[0])));
                    })
                    .onItem().transformToUni(ignored -> {
                        Uni<RowSet<Row>> contributionsDelete = tx.preparedQuery(deleteContributionsSql).execute(Tuple.of(uuid));
                        Uni<RowSet<Row>> genresDelete = tx.preparedQuery(deleteGenresSql).execute(Tuple.of(uuid));
                        Uni<RowSet<Row>> rlsDelete = tx.preparedQuery(deleteRlsSql).execute(Tuple.of(uuid));
                        Uni<RowSet<Row>> filesDelete = tx.preparedQuery(deleteFilesSql).execute(Tuple.of(uuid));

                        return Uni.combine().all().unis(contributionsDelete, genresDelete, rlsDelete, filesDelete)
                                .discardItems()
                                .onItem().transformToUni(ignored2 -> tx.preparedQuery(deleteDocSql).execute(Tuple.of(uuid)))
                                .onItem().transform(RowSet::rowCount);
                    });
        });
    }

}