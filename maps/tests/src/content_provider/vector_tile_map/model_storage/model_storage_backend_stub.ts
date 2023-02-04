import {ModelStorageBackend} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/model_storage/model_storage_backend';
import {createPairOfCommunicators} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/util/paired_backend_communicator';
import {SyncStorageBackendMessages} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';

export class ModelStorageBackendStub extends ModelStorageBackend {
    constructor() {
        super(createPairOfCommunicators<SyncStorageBackendMessages<string, string>>().master);
    }
}
