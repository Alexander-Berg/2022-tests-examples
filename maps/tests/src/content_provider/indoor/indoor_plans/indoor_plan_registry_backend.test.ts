import {expect} from 'chai';
import * as chai from 'chai';
import {stub, SinonStub} from 'sinon';
import * as sinonChai from 'sinon-chai';
import {IndoorPlanRegistryBackend} from '../../../../../src/vector_render_engine/content_provider/indoor/indoor_plans/indoor_plan_registry_backend';
import {PairOfCommunicators, createPairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';
import {IndoorPlanRegistryMessages, AddIndoorPlan, IndoorPlanRegistryMessageType, DestroyIndoorPlan} from '../../../../../src/vector_render_engine/content_provider/indoor/indoor_plans/indoor_plan_registry_messages';
import {IndoorPlanLoader} from '../../../../../src/vector_render_engine/content_provider/indoor/indoor_plans/indoor_plan_loader';
import {createIndoorPlan} from './indoor_plan';
import {IndoorPlanBackend} from '../../../../../src/vector_render_engine/content_provider/indoor/indoor_plans/indoor_plan_backend';
import {LoadManager} from '../../../../../src/vector_render_engine/util/load_manager';

chai.use(sinonChai);

describe('IndoorPlanRegistryBackend', () => {
    const PLAN_ID = '123';

    let communicators: PairOfCommunicators<IndoorPlanRegistryMessages>;
    let loader: IndoorPlanLoader;
    let loadStub: SinonStub;
    let loadManager: LoadManager;
    let onPlanAddedStub: SinonStub;
    let onPlanDestroyedStub: SinonStub;
    let registryBackend: IndoorPlanRegistryBackend;

    beforeEach(() => {
        communicators = createPairOfCommunicators();
        loadManager = new LoadManager();
        loader = new IndoorPlanLoader(loadManager, () => 'url');
        loadStub = stub(loader, 'loadPlan').callsFake(async () => createIndoorPlan());
        registryBackend = new IndoorPlanRegistryBackend(communicators.worker, loader);

        onPlanAddedStub = stub();
        onPlanDestroyedStub = stub();
        communicators.master.onMessage.addListener((message) => {
            switch (message.type) {
                case IndoorPlanRegistryMessageType.ADD:
                    onPlanAddedStub(message);
                    break;
                case IndoorPlanRegistryMessageType.DESTROY:
                    onPlanDestroyedStub(message);
                    break;
            }
        });
    });

    it('should load plans', (done) => {
        let plan: IndoorPlanBackend;

        onPlanAddedStub.callsFake((message: AddIndoorPlan) => {
            expect(message.planId).to.equal(PLAN_ID);
            const addedPlan = registryBackend.getPlan(PLAN_ID)!;
            expect(addedPlan.content).not.to.be.undefined;
            expect(addedPlan.content!.levels).to.deep.equal(message.levels);
            expect(addedPlan.content!.defaultLevelId).to.equal(message.defaultLevelId);
            expect(addedPlan.content!.boundary).to.deep.equal(message.boundary);
            done();
        });

        plan = registryBackend.getOrCreatePlan(PLAN_ID);
        expect(plan.content).to.be.undefined;
    });

    it('should not duplicate requests', (done) => {
        onPlanAddedStub.callsFake((message: AddIndoorPlan) => {
            expect(message.planId).to.equal(PLAN_ID);
            expect(loadStub).to.have.been.calledOnce;
            done();
        });

        const plan1 = registryBackend.getOrCreatePlan(PLAN_ID);

        const plan2 = registryBackend.getOrCreatePlan(PLAN_ID);
        expect(plan2).to.equal(plan1);

        const plan3 = registryBackend.getOrCreatePlan(PLAN_ID);
        expect(plan3).to.equal(plan1);
    });

    it('should destroy the plan when it is released', () => {
        const plan = registryBackend.getOrCreatePlan(PLAN_ID);
        plan.retain();

        plan.release();
        expect(onPlanDestroyedStub).to.have.been.calledOnce;
        expect(onPlanDestroyedStub.lastCall.args[0].planId).to.equal(PLAN_ID);
    });
});
