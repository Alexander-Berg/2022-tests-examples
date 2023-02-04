import * as chai from 'chai';
import {expect} from 'chai';
import {spy} from 'sinon';
import * as sinonChai from 'sinon-chai';
import {GlyphAtlasRegistryBackend} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/glyph_atlas/glyph_atlas_registry_backend';
import {FontLoaderStub} from './util/font_loader_stub';
import {createPairOfCommunicators, PairOfCommunicators} from '../util/backend_communicator';
import {
    GlyphAtlasRegistryMessages,
    GlyphAtlasRegistryUpdateType
} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/glyph_atlas/glyph_atlas_registry_messages';
import {
    GlyphRange,
    GlyphRangeIndex
} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/glyph_atlas/util/glyph_range';
import {promisifyEventEmitter} from '../../../util/event_emitter';
import {LoadManager} from '../../../../../src/vector_render_engine/util/load_manager';

chai.use(sinonChai);

describe('GlyphAtlasRegistryBackend', () => {
    let stubCommunicators: PairOfCommunicators<GlyphAtlasRegistryMessages>;
    let loadManager: LoadManager;
    let stubLoader: FontLoaderStub;
    let registry: GlyphAtlasRegistryBackend;

    beforeEach(() => {
        stubCommunicators = createPairOfCommunicators();
        loadManager = new LoadManager();
        stubLoader = new FontLoaderStub(loadManager);
        registry = new GlyphAtlasRegistryBackend(stubCommunicators.worker, stubLoader, 100, 100);
    });

    it('should load/allocate glyphs and submit updates to the main thread', async () => {
        const ranges = new GlyphRangeIndex([
            ['font#1', 0],
            ['font#2', 0]
        ]);

        const syncAdd = promisifyEventEmitter(stubCommunicators.master.onMessage).then(([message]) => {
            if (message.type === GlyphAtlasRegistryUpdateType.FROM_BACKEND_ADD_ATLAS) {
                expect(message.width).to.be.equal(100);
                expect(message.height).to.be.equal(100);
            } else {
                throw new Error('Not FROM_BACKEND_ADD_ATLAS message');
            }
        });

        await registry.loadGlyphsAndGetAtlas(ranges, 0).then((atlas) => {
            expect(atlas.allocatedRanges.containsAll(ranges)).to.be.true;
        });

        await syncAdd;
    });

    it('should not load ranges that was already loaded', async () => {
        const loadRange = spy(stubLoader, 'fillFont');
        const ranges1 = new GlyphRangeIndex([
            ['font#1', 0],
            ['font#2', 0]
        ]);
        const ranges2 = new GlyphRangeIndex([
            ['font#2', 0],
            ['font#3', 0]
        ]);

        await registry.loadGlyphsAndGetAtlas(ranges1, 0);
        expect(loadRange).to.have.been.callCount(2);

        loadRange.reset();

        await registry.loadGlyphsAndGetAtlas(ranges2, 0);
        expect(loadRange).to.have.been.callCount(1);
    });

    it('should allocate glyphs into new atlas', async () => {
        const firstRange: GlyphRange = ['font#1', 0];
        let firstAtlasId = 0;
        await registry.loadGlyphsAndGetAtlas(new GlyphRangeIndex([firstRange]), 0)
            .then((atlas) => firstAtlasId = atlas.id);

        let latestRange = firstRange;
        let latestAtlasId = firstAtlasId;
        let i = 2;
        while (latestAtlasId === firstAtlasId) {
            latestRange = ['font#' + i, 0];
            await registry.loadGlyphsAndGetAtlas(new GlyphRangeIndex([latestRange]), 0)
                .then((atlas) => latestAtlasId = atlas.id);
            i++;
        }

        const loadRange = spy(stubLoader, 'fillFont');
        await registry.loadGlyphsAndGetAtlas(new GlyphRangeIndex([firstRange, latestRange]), 0).then((atlas) => {
            expect(atlas.allocatedRanges.containsAll(new GlyphRangeIndex([firstRange, latestRange]))).to.be.true;
        });
        expect(loadRange).to.not.have.be.called;
    });

});
