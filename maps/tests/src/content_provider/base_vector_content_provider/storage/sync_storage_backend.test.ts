import {expect} from 'chai';
import {promisifyEventEmitter} from '../../../util/event_emitter';
import {SyncStorageBackend} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_backend';
import {createPairOfCommunicators} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/util/paired_backend_communicator';
import {SyncStorageMessages, SyncStorageMessageType} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';

interface TestItemBackend {
    readonly a: string;
    readonly b: number;
}

interface TestItemSerialized {
    readonly a: string;
    readonly b: number;
    readonly serializationFlag: boolean;
}

class TestSyncStorageBackend extends SyncStorageBackend<TestItemBackend, TestItemSerialized, string> {
    protected _serialize(item: TestItemBackend): TestItemSerialized {
        return {...item, serializationFlag: true};
    }
}

describe('SyncStorageBackend', () => {
    it('should create/remove items and sync updates via communicator', async () => {
        const ID = 'id#1';
        const A = 'str';
        const B = 123;
        const stubCommunicators = createPairOfCommunicators<SyncStorageMessages<TestItemSerialized, string>>();
        const storage = new TestSyncStorageBackend(stubCommunicators.worker);

        const syncAdd = promisifyEventEmitter(stubCommunicators.master.onMessage).then(([message]) => {
            if (message.type === SyncStorageMessageType.FROM_BACKEND_ADD) {
                expect(message.id).to.be.equal(ID);
                expect(message.item.a).to.be.equal(A);
                expect(message.item.b).to.be.equal(B);
                expect(message.item.serializationFlag).to.be.true;
            } else {
                throw new Error('Not FROM_BACKEND_ADD message');
            }
        });

        const item = {a: A, b: B};
        storage.add(ID, item);
        expect(storage.get(ID)).not.to.be.undefined;

        await syncAdd;

        const syncRemove = promisifyEventEmitter(stubCommunicators.master.onMessage).then(([message]) => {
            if (message.type === SyncStorageMessageType.FROM_BACKEND_REMOVE) {
                expect(message.id).to.be.equal(ID);
            } else {
                throw new Error('Not FROM_BACKEND_REMOVE message');
            }
        });

        storage.remove(ID);
        expect(storage.get(ID)).to.be.undefined;

        await syncRemove;
    });

});
