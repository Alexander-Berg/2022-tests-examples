package ru.yandex.disk.feed;

import com.yandex.datasync.editor.CollectionEditor;
import com.yandex.datasync.internal.model.ValueDto;
import com.yandex.datasync.internal.model.response.FieldDto;
import com.yandex.datasync.internal.model.response.RecordDto;
import com.yandex.datasync.wrappedModels.Collection;
import kotlin.collections.CollectionsKt;
import rx.Observable;

import javax.annotation.NonnullByDefault;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@NonnullByDefault
public class MockCollectionBuilder {
    private final Map<String, RecordDto> records = new HashMap<>();
    private String id;
    private CollectionEditor mockCollectionEditor = mock(CollectionEditor.class);

    public MockCollectionBuilder() {
        when(mockCollectionEditor.removeRecord(anyString())).thenReturn(mockCollectionEditor);
    }

    public CollectionEditor getMockEditor() {
        return mockCollectionEditor;
    }

    public BaseFeedBlockRecordBuilder addRecord(final String key) {
        final RecordDto record = addRecordDto(key);
        return new BaseFeedBlockRecordBuilder(record).setRemoteCollectionId(id);
    }

    @SuppressWarnings("ConstantConditions")
    public Collection build() {
        return new Collection(null, null, null, id, null, new HashMap<>(records)) {
            @Override
            public CollectionEditor edit() {
                return mockCollectionEditor;
            }
        };
    }

    public ContentRecordBuilder addContentBlock(final String key) {
        final RecordDto record = addRecordDto(key);
        final ContentRecordBuilder builder = new ContentRecordBuilder(record);
        builder.setRemoteCollectionId(id);
        builder.setType(FeedBlock.Type.CONTENT);
        return builder;
    }

    private RecordDto addRecordDto(final String key) {
        final RecordDto record = mock(RecordDto.class);
        records.put(key, record);
        return record;
    }

    public ContentRecordBuilder addEasyContentBlock(final String suffix) {
        return addContentBlock("block_" + suffix)
                .setFolderId("folder_" + suffix)
                .setRemoteCollectionId(id)
                .setMediaType("mediaType_" + suffix);
    }

    public Observable<BetterCollection> buildObservable() {
        return Observable.just(new BetterCollection(build(), 1));
    }

    public MockCollectionBuilder setNextCollectionReference(
            @Nullable final String nextCollectionId) {
        addRecord("next_index")
                .setField("collection_id", nextCollectionId);
        return this;
    }

    public MockCollectionBuilder setId(final String id) {
        this.id = id;
        return this;
    }

    @NonnullByDefault
    public static class BaseFeedBlockRecordBuilder<B extends BaseFeedBlockRecordBuilder> {

        private final ArrayList<FieldDto> fields = new ArrayList<>();

        @SuppressWarnings("unchecked")
        private final B self = (B) this;

        public BaseFeedBlockRecordBuilder(final RecordDto record) {
            when(record.getFields()).thenReturn(fields);
        }

        public BaseFeedBlockRecordBuilder setField(final String name, final String value) {
            final ValueDto valueDto = mock(ValueDto.class);
            when(valueDto.getStringValue()).thenReturn(value);
            setField(name, valueDto);
            return this;
        }

        public BaseFeedBlockRecordBuilder setField(final String name, final List<String> values) {
            final List<ValueDto> valuesList = CollectionsKt.map(values, val -> {
                final ValueDto elementDto = mock(ValueDto.class);
                when(elementDto.getStringValue()).thenReturn(val);
                return elementDto;
            });
            final ValueDto valueDto = mock(ValueDto.class);
            when(valueDto.getListValues()).thenReturn(valuesList);
            setField(name, valueDto);
            return this;
        }

        private void setField(final String name, final ValueDto value) {
            final FieldDto fieldDto = mock(FieldDto.class);
            when(fieldDto.getFieldId()).thenReturn(name);
            when(fieldDto.getValue()).thenReturn(value);
            fields.add(fieldDto);
        }

        public B setType(final String type) {
            setField("type", type);
            return self;
        }

        public B setFolderId(final String folderId) {
            setField(ContentBlockMapper.FOLDER_ID, folderId);
            return self;
        }

        public B setRemoteCollectionId(final String collectionId) {
            setField("remote_collection_id", collectionId);
            return self;
        }
    }

    public static class ContentRecordBuilder extends BaseFeedBlockRecordBuilder<ContentRecordBuilder> {

        public ContentRecordBuilder(final RecordDto record) {
            super(record);
        }

        public ContentRecordBuilder setMediaType(final String mediaType) {
            setField(ContentBlockMapper.MEDIA_TYPE, mediaType);
            return this;
        }

        public ContentRecordBuilder setModifierUid(final String uid) {
            setField(ContentBlockMapper.MODIFIER_UID, uid);
            return this;
        }

    }
}
