package com.yandex.mail.fakeserver;

import com.yandex.mail.generators.ContainersGenerator;
import com.yandex.mail.wrappers.LabelWrapper;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import kotlin.collections.CollectionsKt;

import static com.yandex.mail.storage.entities.LabelTypeKt.IMPORTANT;
import static com.yandex.mail.storage.entities.LabelTypeKt.USER;
import static kotlin.collections.CollectionsKt.listOf;

/**
 * Encapsulates 'server' labels and defines some handy manipulation methods.
 */
public final class LabelsList {

    @NonNull
    public final List<LabelWrapper> labelsList;

    private LabelsList(@NonNull List<LabelWrapper> labelsList) {
        this.labelsList = new ArrayList<>(labelsList);
    }

    private  LabelsList(@NonNull LabelWrapper... labels) {
        this(listOf(labels));
    }

    @NonNull
    public static LabelsList generateDefault(@NonNull ContainersGenerator generator) {
        LabelWrapper important = LabelWrapper.builder()
                .displayName("Important")
                .serverLid(generator.nextLid())
                .type(IMPORTANT)
                .build();
        return new LabelsList(important);
    }

    @NonNull
    public LabelWrapper getByName(@NonNull final String name) {
        return CollectionsKt.first(labelsList, label -> name.equals(label.getDisplayName()));
    }

    @NonNull
    public LabelWrapper getByType(int labelType) {
        return CollectionsKt.first(labelsList, label -> labelType == label.getType());
    }

    @NonNull
    public LabelWrapper getByServerLid(@NonNull final String serverLid) {
        return CollectionsKt.first(labelsList, label -> serverLid.equals(label.getServerLid()));
    }

    /**
     * Check whether label with given name present in list
     */
    public boolean isPresentLabelWithName(@NonNull final String name) {
        return CollectionsKt.any(labelsList, label -> name.equals(label.getDisplayName()));
    }

    /**
     * Check whether label with given serverLid present in list
     */
    public boolean isPresentLabelWithServerLid(@NonNull final String serverLid) {
        return CollectionsKt.any(labelsList, label -> serverLid.equals(label.getServerLid()));
    }

    /**
     * Removes label with given serverLid if present.
     */
    public void removeLabelByLid(@NonNull final String serverLid) {
        if (isPresentLabelWithServerLid(serverLid)) {
            labelsList.remove(getByServerLid(serverLid));
        }
    }

    @NonNull
    public static LabelWrapper.LabelWrapperBuilder createEmptyUserLabel(@NonNull ContainersGenerator generator) {
        return LabelWrapper.builder()
                .type(USER)
                .serverLid(generator.nextLid());
    }
}
