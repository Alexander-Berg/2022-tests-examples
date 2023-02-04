import {expect} from 'chai';
import * as chai from 'chai';
import {stub, SinonStub} from 'sinon';
import * as sinonChai from 'sinon-chai';
import {IndoorPlanLoader} from '../../../../../src/vector_render_engine/content_provider/indoor/indoor_plans/indoor_plan_loader';
import {createIndoorPlan} from './indoor_plan';
import {IndoorPlanProto} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/proto_aliases';
import {LoadManager} from '../../../../../src/vector_render_engine/util/load_manager';

chai.use(sinonChai);

describe('IndoorPlanLoader', () => {
    const PLAN = createIndoorPlan();

    let loadManager: LoadManager;
    let loader: IndoorPlanLoader;
    let loadStub: SinonStub;
    let decodeProtoStub: SinonStub;
    let urlProviderStub: SinonStub;

    beforeEach(() => {
        loadManager = new LoadManager();
        loadStub = stub(loadManager, 'load').resolves(new ArrayBuffer(0));
        decodeProtoStub = stub(IndoorPlanProto, 'decode').callsFake(() => PLAN);
        urlProviderStub = stub().returnsArg(0);
        loader = new IndoorPlanLoader(loadManager, urlProviderStub);
    });

    afterEach(() => {
        decodeProtoStub.restore();
        loadStub.restore();
    });

    it('should set url provider', async () => {
        const newUrl = 'testurl';
        const urlProviderStub2 = stub().returns(newUrl);
        loader.setUrlProvider(urlProviderStub2);

        await loader.loadPlan('123');

        expect(urlProviderStub).not.to.have.been.called;
        expect(urlProviderStub2).to.have.been.called;
    });

    it('should load plans', async () => {
        const planId = '123';
        const plan = await loader.loadPlan(planId);

        expect(plan.levels).to.deep.equal(PLAN.levels);
        expect(plan.defaultLevelId).to.equal(PLAN.defaultLevelId);
        expect(plan.boundary).to.deep.equal(PLAN.boundary);
    });
});
