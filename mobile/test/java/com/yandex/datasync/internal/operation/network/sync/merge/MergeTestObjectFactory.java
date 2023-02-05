package com.yandex.datasync.internal.operation.network.sync.merge;

import androidx.annotation.NonNull;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.internal.model.request.ChangesRequest;
import com.yandex.datasync.internal.model.response.ApplyChangesResponse;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.DeltasResponse;
import com.yandex.datasync.internal.model.response.SnapshotResponse;

import java.io.IOException;

import static com.yandex.datasync.util.ResourcesUtil.getTextFromFile;

public class MergeTestObjectFactory {

    public static DeltasResponse getDeltasFromFile(@NonNull final String fileName)
            throws IOException {
        return new Moshi.Builder().build().adapter(DeltasResponse.class)
                .fromJson(getTextFromFile(fileName));
    }

    public static SnapshotResponse getSnapshotFromFile(@NonNull final String fileName)
            throws IOException {
        return new Moshi.Builder().build().adapter(SnapshotResponse.class)
                .fromJson(getTextFromFile(fileName));
    }

    public static DatabaseDto getDatabaseInfoFromFile(@NonNull final String fileName)
            throws IOException {
        return new Moshi.Builder().build().adapter(DatabaseDto.class)
                .fromJson(getTextFromFile(fileName));
    }

    public static ApplyChangesResponse getApplyChangesRequest(long revision) throws IOException {
        return getApplyChangesRequest("merge/apply_changes.json", revision);
    }

    public static ApplyChangesResponse getApplyChangesRequest(String fileName, long revision) throws IOException {
        final ApplyChangesResponse applyChangesResponse =
                new Moshi.Builder().build().adapter(ApplyChangesResponse.class)
                        .fromJson(getTextFromFile(fileName));
        applyChangesResponse.setRevision(revision);
        return applyChangesResponse;
    }

    public static ChangesRequest getChangesFromFile(@NonNull final String fileName)
            throws IOException {
        return new Moshi.Builder().build().adapter(ChangesRequest.class)
                .fromJson(getTextFromFile(fileName));
    }
}
