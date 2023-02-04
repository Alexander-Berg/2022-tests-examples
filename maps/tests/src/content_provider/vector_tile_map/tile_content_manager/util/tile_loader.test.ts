import {expect} from 'chai';
import * as sinon from 'sinon';
import {TileLoader} from '../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/util/tile_loader';
import {VectorTileBackend} from '../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile/vector_tile_backend';
import {TileSize} from '../../../../../../src/vector_render_engine/content_provider/vector_tile_map/util/tile_size';

describe('TileLoader', () => {
    let loader: TileLoader;

    let stubLoad: sinon.SinonStub;
    let stubCancel: sinon.SinonStub;
    let stubAddListener: sinon.SinonStub;
    let stubRemoveListener: sinon.SinonStub;

    beforeEach(() => {
        stubLoad = sinon.stub();
        stubCancel = sinon.stub();
        stubAddListener = sinon.stub();
        stubRemoveListener = sinon.stub();
        const stubLoadManager: any = {
            load: stubLoad,
            cancel: stubCancel
        };
        const stubTileStorage: any = {
            onRemoved: {
                addListener: stubAddListener,
                removeListener: stubRemoveListener
            }
        };
        loader = new TileLoader(stubLoadManager, stubTileStorage);
        loader.update('tile({{x}},{{y}},{{z}})', TileSize.X1, false);
    });

    function makeTile(x: number, y: number, zoom: number): VectorTileBackend {
        return {
            tile: {x, y, zoom}
        } as any;
    }

    it('should manage "onTileRemoved" subscription', () => {
        expect(stubAddListener.callCount).to.equal(1);
        expect(stubRemoveListener.callCount).to.equal(0);
        const callback = stubAddListener.lastCall.args[0];

        loader.destructor();

        expect(stubAddListener.callCount).to.equal(1);
        expect(stubRemoveListener.callCount).to.equal(1);
        expect(stubRemoveListener.lastCall.args).to.deep.equal([callback]);
    });

    it('should pass "load" requests down to "LoadManager"', () => {
        const mock = Promise.resolve(new ArrayBuffer(0));
        stubLoad.returns(mock);

        const ret = loader.load(makeTile(1, 2, 10), 101);

        expect(ret).to.equal(mock);
        expect(stubLoad.lastCall.args).to.deep.equal(['tile(1,2,10)', 101, {withCredentials: false}]);
    });

    it('should pass "cancel" requests down to "LoadManager"', () => {
        const mock = Promise.resolve(new ArrayBuffer(0));

        loader.cancel(mock);

        expect(stubCancel.lastCall.args).to.deep.equal([mock]);
    });

    it('should build tile url properly', () => {
        const mock = Promise.resolve(new ArrayBuffer(0));
        stubLoad.returns(mock);

        loader.update('url://host={{hostAlias}}/{{x}},{{y}},{{z}}/{{zmin}}-{{zmax}}', TileSize.X1, false);
        loader.load(makeTile(1, 2, 10), 101);
        expect(stubLoad.lastCall.args).to.deep.equal(['url://host=1/1,2,10/10-10', 101, {withCredentials: false}]);

        loader.update('url://{{hostAlias}}/coords={{x}},{{y}},{{z}}/{{zmin}}-{{zmax}}', TileSize.X4, false);
        loader.load(makeTile(2, 3, 11), 102);
        expect(stubLoad.lastCall.args).to.deep.equal(['url://4/coords=2,3,11/12-12', 102, {withCredentials: false}]);

        loader.update('url://{{hostAlias}}/{{x}},{{y}},{{z}}/zrange={{zmin}}-{{zmax}}', TileSize.X16, false);
        loader.load(makeTile(3, 4, 12), 103);
        expect(stubLoad.lastCall.args).to.deep.equal(['url://3/3,4,12/zrange=14-14', 103, {withCredentials: false}]);

        loader.update('url://{{x}},{{y}},{{z}}', TileSize.X1, false);
        loader.load(makeTile(4, 5, 13), 104);
        expect(stubLoad.lastCall.args).to.deep.equal(['url://4,5,13', 104, {withCredentials: false}]);
    });

    it('should not interfere with "LoadManager" cache mechanism', () => {
        const mock1 = Promise.resolve(new ArrayBuffer(0));
        const mock2 = Promise.resolve(new ArrayBuffer(0));
        const mock3 = Promise.resolve(new ArrayBuffer(0));
        stubLoad.onCall(0).returns(mock1);
        stubLoad.onCall(1).returns(mock2);
        stubLoad.onCall(2).returns(mock3);

        expect(loader.load(makeTile(1, 2, 10), 101)).to.equal(mock1);
        expect(loader.load(makeTile(2, 3, 11), 102)).to.equal(mock2);
        expect(loader.load(makeTile(3, 4, 12), 103)).to.equal(mock3);
        expect(stubLoad.callCount).to.equal(3);
    });

    it('should cache resolved request results', async () => {
        const data1 = new Uint8Array([1, 2, 3]).buffer;
        const data2 = new Uint8Array([2, 3, 4]).buffer;
        const data3 = new Uint8Array([3, 4, 5]).buffer;
        stubLoad.onCall(0).resolves(data1);
        stubLoad.onCall(1).resolves(data2);
        stubLoad.onCall(2).resolves(data3);

        expect(await Promise.all([
            loader.load(makeTile(1, 2, 10), 101),
            loader.load(makeTile(2, 3, 11), 102)
        ])).to.deep.equal([data1, data2]);
        expect(stubLoad.callCount).to.equal(2);

        expect(await Promise.all([
            loader.load(makeTile(2, 3, 11), 103),
            loader.load(makeTile(1, 2, 10), 104),
            loader.load(makeTile(3, 4, 12), 105),
            loader.load(makeTile(1, 2, 10), 106)
        ])).to.deep.equal([data2, data1, data3, data1]);
        expect(stubLoad.callCount).to.equal(3);

        expect(await Promise.all([
            loader.load(makeTile(3, 4, 12), 107),
            loader.load(makeTile(2, 3, 11), 108)
        ])).to.deep.equal([data3, data2]);
        expect(stubLoad.callCount).to.equal(3);
    });

    it('should clear cache when tile is removed', async () => {
        const data1 = new Uint8Array([1, 2, 3]).buffer;
        const data2 = new Uint8Array([2, 3, 4]).buffer;
        const data3 = new Uint8Array([3, 4, 5]).buffer;
        stubLoad.onCall(0).resolves(data1);
        stubLoad.onCall(1).resolves(data2);
        stubLoad.onCall(2).resolves(data3);

        await Promise.all([
            loader.load(makeTile(1, 2, 10), 101),
            loader.load(makeTile(2, 3, 11), 102)
        ]);

        stubAddListener.lastCall.args[0].call(loader, makeTile(1, 2, 10));

        expect(await Promise.all([
            loader.load(makeTile(1, 2, 10), 101),
            loader.load(makeTile(2, 3, 11), 102)
        ])).to.deep.equal([data3, data2]);
        expect(stubLoad.callCount).to.equal(3);
    });

    it('should clear cache only when related tiles are removed', async () => {
        const data1 = new Uint8Array([1, 2, 3]).buffer;
        const data2 = new Uint8Array([2, 3, 4]).buffer;
        const data3 = new Uint8Array([3, 4, 5]).buffer;

        stubLoad.onCall(0).resolves(data1);
        stubLoad.onCall(1).resolves(data2);
        stubLoad.onCall(2).resolves(data3);

        await Promise.all([
            loader.load(makeTile(1, 2, 10), 101),
            loader.load(makeTile(2, 3, 11), 102),
        ]);
        // After this '1_2_10' and '2_3_11' are both referenced twice.
        await Promise.all([
            loader.load(makeTile(1, 2, 10), 103),
            loader.load(makeTile(2, 3, 11), 104),
        ]);

        // After this '1_2_10' is referenced once.
        stubAddListener.lastCall.args[0].call(loader, makeTile(1, 2, 10));

        // After this '1_2_10' is referenced twice, '2_3_11' - thrice.
        expect(await Promise.all([
            loader.load(makeTile(2, 3, 11), 105),
            loader.load(makeTile(1, 2, 10), 106),
        ])).to.deep.equal([data2, data1]);
        expect(stubLoad.callCount).to.equal(2);

        // After this '1_2_10' is referenced once, '2_3_11' - none.
        stubAddListener.lastCall.args[0].call(loader, makeTile(1, 2, 10));
        stubAddListener.lastCall.args[0].call(loader, makeTile(2, 3, 11));
        stubAddListener.lastCall.args[0].call(loader, makeTile(2, 3, 11));
        stubAddListener.lastCall.args[0].call(loader, makeTile(2, 3, 11));

        expect(await Promise.all([
            loader.load(makeTile(2, 3, 11), 105),
            loader.load(makeTile(1, 2, 10), 106),
        ])).to.deep.equal([data3, data1]);
        expect(stubLoad.callCount).to.equal(3);
    });

    it('should reset entire cache after update', async () => {
        const data1 = new Uint8Array([1, 2, 3]).buffer;
        const data2 = new Uint8Array([2, 3, 4]).buffer;
        const data3 = new Uint8Array([3, 4, 5]).buffer;
        const data4 = new Uint8Array([4, 5, 6]).buffer;
        stubLoad.onCall(0).resolves(data1);
        stubLoad.onCall(1).resolves(data2);
        stubLoad.onCall(2).resolves(data3);
        stubLoad.onCall(3).resolves(data4);

        await Promise.all([
            loader.load(makeTile(1, 2, 10), 101),
            loader.load(makeTile(2, 3, 11), 102)
        ]);

        loader.update('test', TileSize.X1, false);

        expect(await Promise.all([
            loader.load(makeTile(2, 3, 11), 102),
            loader.load(makeTile(1, 2, 10), 101)
        ])).to.deep.equal([data3, data4]);
        expect(stubLoad.callCount).to.equal(4);
    });

    it('should not reset cache after false update', async () => {
        const data1 = new Uint8Array([1, 2, 3]).buffer;
        const data2 = new Uint8Array([2, 3, 4]).buffer;
        stubLoad.onCall(0).resolves(data1);
        stubLoad.onCall(1).resolves(data2);

        await Promise.all([
            loader.load(makeTile(1, 2, 10), 101),
            loader.load(makeTile(2, 3, 11), 102)
        ]);

        loader.update('tile({{x}},{{y}},{{z}})', TileSize.X1, false);

        expect(await Promise.all([
            loader.load(makeTile(2, 3, 11), 102),
            loader.load(makeTile(1, 2, 10), 101)
        ])).to.deep.equal([data2, data1]);
        expect(stubLoad.callCount).to.equal(2);
    });

    it('should handle same requests race', async () => {
        let resolve1!: Function;
        const mock1 = new Promise((resolve) => { resolve1 = resolve; });
        let resolve2!: Function;
        const mock2 = new Promise((resolve) => { resolve2 = resolve; });
        // Actually *LoadManager* resolves same requests with same data.
        // These data items are different for the purpose of equality check.
        const data1 = new Uint8Array([1, 2, 3]).buffer;
        const data2 = new Uint8Array([2, 3, 4]).buffer;
        stubLoad.onCall(0).returns(mock1);
        stubLoad.onCall(1).returns(mock2);

        loader.load(makeTile(1, 2, 10), 101);
        loader.load(makeTile(1, 2, 10), 101);

        await new Promise((resolve) => {
            resolve1(data1);
            setTimeout(resolve, 1);
        });
        await new Promise((resolve) => {
            resolve2(data2);
            setTimeout(resolve, 1);
        });

        expect(await loader.load(makeTile(1, 2, 10), 101)).to.equal(data2);
    });

    it('should handle same requests and update race', async () => {
        let resolve1!: Function;
        const mock1 = new Promise((resolve) => { resolve1 = resolve; });
        let resolve2!: Function;
        const mock2 = new Promise((resolve) => { resolve2 = resolve; });
        const data1 = new Uint8Array([1, 2, 3]).buffer;
        const data2 = new Uint8Array([2, 3, 4]).buffer;
        stubLoad.onCall(0).returns(mock1);
        stubLoad.onCall(1).returns(mock2);

        loader.load(makeTile(1, 2, 10), 101);
        loader.update('tile({{z}},{{y}},{{x}})', TileSize.X1, false);
        loader.load(makeTile(1, 2, 10), 101);

        await new Promise((resolve) => {
            resolve2(data2);
            setTimeout(resolve, 1);
        });
        await new Promise((resolve) => {
            resolve1(data1);
            setTimeout(resolve, 1);
        });

        expect(await loader.load(makeTile(1, 2, 10), 101)).to.equal(data2);
    });

    it('should pass "withCredentials" option to "LoadManager"', () => {
        loader.update('tile({{z}},{{y}},{{x}})', TileSize.X1, true);
        stubLoad.returns(Promise.resolve(new ArrayBuffer(0)));

        loader.load(makeTile(1, 2, 3), 0);

        expect(stubLoad.lastCall.args[2]).to.deep.equal({withCredentials: true});
    });
});
