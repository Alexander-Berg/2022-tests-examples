import {expect} from 'chai';
import {SinonStub, stub} from 'sinon';
import {createPairOfCommunicators, PairOfCommunicators} from '../util/backend_communicator';
import {promisifyEventEmitter} from '../../../util/event_emitter';
import {IconAtlasRegistryBackend} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/icon_atlas/icon_atlas_registry_backend';
import {IconAtlasBackend} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/icon_atlas/icon_atlas_backend';
import {IconAtlasRegistryMessages, IconAtlasRegistryUpdateType, AddAtlas, UpdateAtlas} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/icon_atlas/icon_atlas_registry_messages';
import {LoadManager} from '../../../../../src/vector_render_engine/util/load_manager';

describe('IconAtlasRegistryBackend', () => {
    let stubCommunicators: PairOfCommunicators<IconAtlasRegistryMessages>;
    let registry: IconAtlasRegistryBackend;
    let loadManager: LoadManager;
    let stubLoad: SinonStub;
    let stubAllocate: SinonStub;
    let stubFlushUpdates: SinonStub;
    let stubRelease: SinonStub;

    beforeEach(() => {
        stubCommunicators = createPairOfCommunicators();
        loadManager = new LoadManager();
        registry = new IconAtlasRegistryBackend(stubCommunicators.worker, loadManager, 400, 300);
        (registry as any)._onSetup({
            options: {iconFormat: 'bmp'}
        });

        let iconLocation = 0;
        stubLoad = stub(loadManager, 'load');
        stubLoad.resolves(makeIconData(0, 0, new Uint8Array(0)));
        stubAllocate = stub(IconAtlasBackend.prototype, 'allocate').callsFake(() => ++iconLocation);
        stubFlushUpdates = stub(IconAtlasBackend.prototype, 'flushUpdates').returns(null);
        stubRelease = stub(IconAtlasBackend.prototype, 'release');
    });

    afterEach(() => {
        stubLoad.restore();
        stubAllocate.restore();
        stubFlushUpdates.restore();
        stubRelease.restore();
    });

    it('should load icon', async () => {
        stubLoad.resolves(makeIconData(16, 15, IMAGE.data));

        const rawIcon = await registry.loadIcon('icon-1', 0);

        expect(rawIcon).to.deep.equal(IMAGE);
    });

    it('should add icon and create atlas', async () => {
        const pendingMessage = promisifyEventEmitter(stubCommunicators.master.onMessage);

        const iconData = registry.addIcon(IMAGE, 'icon-1');
        const [message] = await pendingMessage;

        expect(iconData.atlasId).to.equal(0);
        expect(iconData.imageSize).to.deep.equal({width: 16, height: 15});

        const atlas = registry.getAtlas(0)!;
        expect(atlas.id).to.equal(0);
        expect(atlas.width).to.equal(400);
        expect(atlas.height).to.equal(300);

        expect((atlas.allocate as SinonStub).lastCall.args).to.deep.equal([IMAGE.size, IMAGE.data]);

        expect(message.type).to.equal(IconAtlasRegistryUpdateType.FROM_BACKEND_ADD_ATLAS);
        expect((message as AddAtlas).atlasId).to.equal(0);
        expect((message as AddAtlas).atlasWidth).to.equal(400);
        expect((message as AddAtlas).atlasHeight).to.equal(300);
    });

    it('should use existing atlas if there is enough space', () => {
        registry.addIcon(IMAGE, 'icon-1');
        stubAllocate.resetHistory();

        registry.addIcon(IMAGE, 'icon-2');

        expect(registry.getAtlas(1)).to.equal(undefined);
        expect(stubAllocate.callCount).to.equal(1);
    });

    it('should create another altas if there is not enough space', async () => {
        registry.addIcon(IMAGE, 'icon-1');
        stubAllocate.reset();
        stubAllocate.onCall(0).returns(false);
        stubAllocate.onCall(1).returns(true);
        const pendingMessage = promisifyEventEmitter(stubCommunicators.master.onMessage);

        registry.addIcon(IMAGE, 'icon-2');
        const [message] = await pendingMessage;

        expect(registry.getAtlas(0)).not.to.equal(undefined);
        expect(registry.getAtlas(1)).not.to.equal(undefined);
        expect(stubAllocate.callCount).to.equal(2);
        expect(stubRelease.callCount).to.equal(1);

        expect(message.type).to.equal(IconAtlasRegistryUpdateType.FROM_BACKEND_ADD_ATLAS);
        expect((message as AddAtlas).atlasId).to.equal(1);
    });

    it('should throw error if cannot allocate space for icon', () => {
        stubAllocate.onCall(0).returns(false);
        stubAllocate.onCall(1).returns(false);

        try {
            registry.addIcon(IMAGE, 'icon-1');
            expect.fail('Should fail');
        } catch (err) {
            expect(err.message).to.equal('Couldn\'t allocate icon in atlas');
        }
    });

    it('should not add same icon twice', () => {
        const iconData1 = registry.addIcon(IMAGE, 'icon-1', 0);

        const iconData2 = registry.addIcon(IMAGE, 'icon-1', 0);

        expect(registry.getAtlas(1)).to.equal(undefined);
        expect(stubAllocate.callCount).to.equal(1);
        expect(iconData1).to.deep.equal(iconData2);
    });

    it('should send update message if there are changes', () => {
        registry.addIcon(IMAGE, 'icon-1');
        stubAllocate.resetHistory();
        stubAllocate.onCall(0).returns(false);
        registry.addIcon(IMAGE, 'icon-2');
        stubAllocate.resetHistory();
        stubAllocate.onCall(0).returns(false);
        registry.addIcon(IMAGE, 'icon-3');

        const testRegion1 = {data: {buffer: 'buffer-1'}};
        const testRegion3 = {data: {buffer: 'buffer-3'}};
        stubFlushUpdates.onCall(0).returns(testRegion1);
        stubFlushUpdates.onCall(1).returns(undefined);
        stubFlushUpdates.onCall(2).returns(testRegion3);
        const messages: UpdateAtlas[] = [];
        stubCommunicators.master.onMessage.addListener((message) => {
            messages.push(message as UpdateAtlas);
        });

        registry.flushAtlasesUpdates();

        expect(messages).to.deep.equal([
            {
                type: IconAtlasRegistryUpdateType.FROM_BACKEND_UPDATE_ATLAS,
                atlasId: 0,
                atlasRegion: testRegion1
            },
            {
                type: IconAtlasRegistryUpdateType.FROM_BACKEND_UPDATE_ATLAS,
                atlasId: 2,
                atlasRegion: testRegion3
            }
        ]);
    });

    it('should return icon data', () => {
        const image1 = {size: {width: 4, height: 3}, data: new Uint8Array([1, 2, 3, 4, 5, 6, 7, 8])};
        const image2 = {size: {width: 6, height: 5}, data: new Uint8Array([1, 2, 3, 4, 5, 6, 7, 8])};
        stubAllocate.onCall(0).returns('test-location-1');
        stubAllocate.onCall(1).returns('test-location-2');
        registry.addIcon(image1, 'icon-1');
        registry.addIcon(image2, 'icon-2');

        expect(registry.getIconData('icon-1')).to.deep.equal({
            atlasId: 0,
            atlasLocation: 'test-location-1',
            imageSize: {width: 4, height: 3},
            data: image1
        });
        expect(registry.getIconData('icon-2')).to.deep.equal({
            atlasId: 0,
            atlasLocation: 'test-location-2',
            imageSize: {width: 6, height: 5},
            data: image2
        });
        expect(registry.getIconData('icon-3')).to.equal(undefined);
    });

    it('should not reuse icons with old url template', () => {
        const iconData1 = registry.addIcon(IMAGE, 'icon', 0);

        stubCommunicators.master.sendMessage({
            type: IconAtlasRegistryUpdateType.TO_BACKEND_SETUP,
            options: {
                iconUrlTemplate: 'some-url'
            }
        }, true);
        const iconData2 = registry.addIcon(IMAGE, 'icon', 0);

        console.log(iconData1, iconData2);

        expect(stubAllocate.callCount).to.equal(2);
        expect(iconData1).not.to.deep.equal(iconData2);
    });
});

const IMAGE = {
    size: {
        width: 16,
        height: 15
    },
    data: new Uint8Array([1, 2, 3, 0, 1, 0, 0, 1])
};

function makeIconData(width: number, height: number, rawData: Uint8Array): ArrayBuffer {
    const data = new Uint32Array(rawData.buffer);
    const array = new Uint32Array(3 + data.length);
    array[0] = 0x97452C59;
    array[1] = width;
    array[2] = height;
    for (let i = 0; i < data.length; ++i) {
        array[3 + i] = data[i];
    }
    return array.buffer;
}
