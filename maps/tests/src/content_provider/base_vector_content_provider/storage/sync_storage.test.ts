import {expect} from 'chai';
import {promisifyEventEmitter} from '../../../util/event_emitter';
import {SyncStorage} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage';
import {SyncStorageMessages, SyncStorageMessageType} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {createPairOfCommunicators} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/util/paired_backend_communicator';

interface TestItem {
    readonly a: string;
    readonly b: number;
}

interface TestItemSerialized {
    readonly a: string;
    readonly b: number;
    readonly serializationFlag: boolean;
}

class TestSyncStorage extends SyncStorage<TestItem, TestItemSerialized, string> {
    protected _deserialize(item: TestItemSerialized): TestItem {
        return {
            a: item.a,
            b: item.b
        };
    }
}

describe('SyncStorage', () => {
    it('should add/remove items by backend messages', async () => {
        const ID = 'id#1';
        const A = 'str';
        const B = 123;
        const stubCommunicators = createPairOfCommunicators<SyncStorageMessages<TestItemSerialized, string>>();
        const storage = new TestSyncStorage(stubCommunicators.master);

        const adding = promisifyEventEmitter(storage.onAdded).then(([item]) => {
            expect(item.a).to.be.equal(A);
            expect(item.b).to.be.equal(B);
        });

        stubCommunicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id: ID,
            item: {a: A, b: B, serializationFlag: true}
        }, true);

        await adding;

        expect(storage.get(ID)).not.to.be.undefined;

        const removing = promisifyEventEmitter(storage.onRemoved).then(([item]) => {
            expect(item.a).to.be.equal(A);
            expect(item.b).to.be.equal(B);
        });

        stubCommunicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_REMOVE,
            id: ID
        }, true);

        await removing;

        expect(storage.get(A)).to.be.undefined;
    });

});
