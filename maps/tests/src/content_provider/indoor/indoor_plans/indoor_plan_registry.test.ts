import {stub, SinonStub} from 'sinon';
import {expect} from 'chai';
import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import {createPairOfCommunicators, PairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';
import {IndoorPlanRegistry} from '../../../../../src/vector_render_engine/content_provider/indoor/indoor_plans/indoor_plan_registry';
import {IndoorPlanRegistryMessages, IndoorPlanRegistryMessageType} from '../../../../../src/vector_render_engine/content_provider/indoor/indoor_plans/indoor_plan_registry_messages';
import {IndoorPlanRegistryOptions} from '../../../../../src/vector_render_engine/content_provider/indoor/indoor_plans/indoor_plan_registry_options';
import {IndoorPlan} from '../../../../../src/vector_render_engine/content_provider/indoor/indoor_plans/indoor_plan';
import {createIndoorPlan} from './indoor_plan';

chai.use(sinonChai);

describe('IndoorPlanRegistry', () => {
    let communicators: PairOfCommunicators<IndoorPlanRegistryMessages>;
    let registry: IndoorPlanRegistry;
    let onPlanAddedStub: SinonStub;

    beforeEach(() => {
        communicators = createPairOfCommunicators();
        const options: IndoorPlanRegistryOptions = {
            indoorPlanUrlTemplate: 'someurl'
        };
        registry = new IndoorPlanRegistry(communicators.master, options);

        onPlanAddedStub = stub();
        registry.onPlanAdded.addListener(onPlanAddedStub);
    });

    it('should add a plan', () => {
        communicators.worker.sendMessage({
            type: IndoorPlanRegistryMessageType.ADD,
            planId: '123',
            ...createIndoorPlan() as any
        }, true);

        const receivedPlan: IndoorPlan = onPlanAddedStub.lastCall.args[0];
        expect(onPlanAddedStub).to.have.been.calledOnce;
        expect(receivedPlan).to.be.instanceof(IndoorPlan);
        expect(receivedPlan.id).to.equal('123');

        expect(registry.getPlan('123')).to.deep.equal(receivedPlan);
    });

    it('should destroy a plan', () => {
        communicators.worker.sendMessage({
            type: IndoorPlanRegistryMessageType.ADD,
            planId: '123',
            ...createIndoorPlan() as any
        }, true);

        expect(registry.getPlan('123')).not.to.be.undefined;

        communicators.worker.sendMessage({
            type: IndoorPlanRegistryMessageType.DESTROY,
            planId: '123'
        }, true);

        expect(registry.getPlan('123')).to.be.undefined;
    });
});
