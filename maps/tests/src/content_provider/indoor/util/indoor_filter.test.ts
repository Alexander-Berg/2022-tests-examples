import {expect} from 'chai';
import * as chai from 'chai';
import {stub, SinonStub} from 'sinon';
import * as sinonChai from 'sinon-chai';
import {IndoorPlanRegistry} from '../../../../../src/vector_render_engine/content_provider/indoor/indoor_plans/indoor_plan_registry';
import {IndoorPlanRegistryMessages, IndoorPlanRegistryMessageType} from '../../../../../src/vector_render_engine/content_provider/indoor/indoor_plans/indoor_plan_registry_messages';
import {PairOfCommunicators, createPairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';
import {IndoorFilter} from '../../../../../src/vector_render_engine/content_provider/indoor/util/indoor_filter';
import {RenderablePrimitive} from '../../../../../src/vector_render_engine/render/primitive/renderable_primitive';
import {createIndoorMetadata} from '../indoor_metadata';
import {createIndoorPlan} from '../indoor_plans/indoor_plan';
import {IndoorPlan} from '../../../../../src/vector_render_engine/content_provider/indoor/indoor_plans/indoor_plan';

chai.use(sinonChai);

describe('IndoorFilter', () => {
    const PRIMITIVE_1 = {id: 1, metadata: createIndoorMetadata('plan1', '1')} as any;
    const PRIMITIVE_2 = {id: 2, metadata: createIndoorMetadata('plan1', '2')} as any;
    const PRIMITIVE_3 = {id: 3, metadata: createIndoorMetadata('plan2', '1')} as any;
    const PRIMITIVE_4 = {id: 4, metadata: createIndoorMetadata('plan2', '2')} as any;

    let filter: IndoorFilter<RenderablePrimitive>;
    let planRegistry: IndoorPlanRegistry;
    let communicators: PairOfCommunicators<IndoorPlanRegistryMessages>;
    let onUpdateStub: SinonStub;

    function addPlan(planId: string): IndoorPlan {
        communicators.worker.sendMessage({
            type: IndoorPlanRegistryMessageType.ADD,
            planId,
            ...createIndoorPlan() as any
        }, true);
        return planRegistry.getPlan(planId)!;
    }

    beforeEach(() => {
        communicators = createPairOfCommunicators();
        planRegistry = new IndoorPlanRegistry(communicators.master, {indoorPlanUrlTemplate: 'url'});

        filter = new IndoorFilter(planRegistry);

        onUpdateStub = stub();
        filter.onUpdate.addListener(onUpdateStub);
    });

    it('should test primitives according to plans visibility and active level', () => {
        expect(filter.test(PRIMITIVE_1)).to.be.false;
        expect(filter.test(PRIMITIVE_2)).to.be.false;
        expect(filter.test(PRIMITIVE_3)).to.be.false;
        expect(filter.test(PRIMITIVE_4)).to.be.false;

        const plan1 = addPlan('plan1');

        plan1.isVisible = true;
        expect(filter.test(PRIMITIVE_1)).to.be.true;
        expect(filter.test(PRIMITIVE_2)).to.be.false;

        plan1.setActiveLevel('2');
        expect(filter.test(PRIMITIVE_1)).to.be.false;
        expect(filter.test(PRIMITIVE_2)).to.be.true;

        plan1.isVisible = false;
        expect(filter.test(PRIMITIVE_1)).to.be.false;
        expect(filter.test(PRIMITIVE_2)).to.be.false;
    });

    it('should fire onUpdate when visible plans are added or removed', () => {
        const plan = addPlan('plan1');

        // Should not be called as plan that is added is invisible
        expect(onUpdateStub).to.have.not.been.called;

        plan.isVisible = true;
        onUpdateStub.reset();
        communicators.worker.sendMessage({
            type: IndoorPlanRegistryMessageType.DESTROY,
            planId: 'plan1'
        }, true);
        expect(onUpdateStub).to.have.been.calledOnce;
    });

    it('should fire onUpdate when visibility of a plan changes', () => {
        const plan = addPlan('plan1');

        plan.isVisible = true;
        expect(onUpdateStub).to.have.been.calledOnce;

        onUpdateStub.reset();
        plan.isVisible = false;
        expect(onUpdateStub).to.have.been.calledOnce;
    });

    it('should fire onUpdate when active level of the visible plan changes', () => {
        const plan = addPlan('plan1');
        plan.isVisible = true;

        onUpdateStub.reset();
        plan.setActiveLevel('2');
        expect(onUpdateStub).to.have.been.calledOnce;
    });

    it('should not fire onUpdate when active level of the invisible plan changed', () => {
        const plan = addPlan('plan1');

        onUpdateStub.reset();
        plan.setActiveLevel('2');
        expect(onUpdateStub).to.have.not.been.called;
    });
});
