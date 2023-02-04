import {ModelStorage} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/model_storage/model_storage';
import {createPairOfCommunicators} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/util/paired_backend_communicator';
import {SyncStorageMessages} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {VectorModel} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/model_storage/vector_model';

export class ModelStorageStub extends ModelStorage {
    constructor() {
        super(createPairOfCommunicators<SyncStorageMessages<string, string>>().master);
    }

    addModel(model: VectorModel): void {
        this._add(model.id, model);
    }

    removeModel(model: VectorModel): void {
        this._remove(model.id);
    }
}
