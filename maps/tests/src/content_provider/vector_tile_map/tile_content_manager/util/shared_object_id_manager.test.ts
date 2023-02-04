import {expect} from 'chai';
import {SharedObjectIdManager} from 'vector_render_engine/content_provider/vector_tile_map/tile_content_manager/util/shared_object_id_manager';
import {ObjectIdProvider} from 'vector_render_engine/util/id_provider';

const FIRST_COLLIDING_ID = 1;
const FIRST_ID = 16;

describe('SharedObjectIdManager', () => {
    let manager: SharedObjectIdManager;
    let objectIdProvider: ObjectIdProvider;

    beforeEach(() => {
        let id = FIRST_ID;
        let collidingId = FIRST_COLLIDING_ID;
        objectIdProvider = {
            idGenerator: () => id++,
            collidingIdGenerator: () => collidingId++
        };
        manager = new SharedObjectIdManager(objectIdProvider);
    });

    it('should generate regular (non-shared) IDs', () => {
        const provider = manager.createSharedIdProvider('1_1_1');
        const id = provider.idGenerator()
        const collidingId = provider.collidingIdGenerator();

        expect(id).to.equal(FIRST_ID);
        expect(collidingId).to.equal(FIRST_COLLIDING_ID);
    });

    it('should generate shared IDS', () => {
        const provider = manager.createSharedIdProvider('1_1_1');

        const id_1 = provider.idGenerator('123');
        const id_2 = provider.idGenerator('123');
        const id_3 = provider.idGenerator('123');
        const collidingId_1 = provider.collidingIdGenerator('456');
        const collidingId_2 = provider.collidingIdGenerator('456');
        const collidingId_3 = provider.collidingIdGenerator('456');

        expect(id_2).to.equal(id_1);
        expect(id_3).to.equal(id_1);
        expect(collidingId_2).to.equal(collidingId_1);
        expect(collidingId_3).to.equal(collidingId_1);
    });

    it('should release tile IDs', () => {
        const provider_1 = manager.createSharedIdProvider('1_1_1');
        const provider_2 = manager.createSharedIdProvider('1_1_2');
        const provider_3 = manager.createSharedIdProvider('1_1_3');

        const id_1 = provider_1.idGenerator('123');
        const id_2 = provider_2.idGenerator('123');
        manager.releaseTileObjectIds('1_1_1');
        const id_3 = provider_2.idGenerator('123');
        manager.releaseTileObjectIds('1_1_2');
        const id_4 = provider_3.idGenerator('123');

        expect(id_2).to.equal(id_1);
        expect(id_3).to.equal(id_1);
        expect(id_4).not.to.equal(id_1);
    });
});
