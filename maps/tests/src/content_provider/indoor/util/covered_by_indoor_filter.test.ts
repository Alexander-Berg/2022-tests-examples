import {expect} from 'chai';
import * as chai from 'chai';
import {stub, SinonStub} from 'sinon';
import * as sinonChai from 'sinon-chai';
import {CoveredByIndoorFilter} from '../../../../../src/vector_render_engine/content_provider/indoor/util/covered_by_indoor_filter';

chai.use(sinonChai);

describe('CoveredByIndoorFilter', () => {
    let filter: CoveredByIndoorFilter;
    let onUpdateStub: SinonStub;

    beforeEach(() => {
        filter = new CoveredByIndoorFilter();
        onUpdateStub = stub();
        filter.onUpdate.addListener(onUpdateStub);
    });

    it('should be deactivated by default', () => {
        expect(filter.test(PRIMITIVE_1)).to.equal(true);
        expect(filter.test(PRIMITIVE_2)).to.equal(true);
        expect(filter.test(PRIMITIVE_3)).to.equal(true);
        expect(filter.test(PRIMITIVE_4)).to.equal(true);
    });

    it('should filter out primitive that is covered by indoor only when active', () => {
        filter.activate();

        expect(filter.test(PRIMITIVE_1)).to.equal(false);
        expect(filter.test(PRIMITIVE_2)).to.equal(true);
        expect(filter.test(PRIMITIVE_3)).to.equal(true);
        expect(filter.test(PRIMITIVE_4)).to.equal(true);

        filter.deactivate();

        expect(filter.test(PRIMITIVE_1)).to.equal(true);
        expect(filter.test(PRIMITIVE_2)).to.equal(true);
        expect(filter.test(PRIMITIVE_3)).to.equal(true);
        expect(filter.test(PRIMITIVE_4)).to.equal(true);
    });

    it('should fire onUpdate when activated or deactivated', () => {
        filter.activate();
        expect(onUpdateStub).to.have.been.calledOnce;

        onUpdateStub.resetHistory();

        filter.deactivate();
        expect(onUpdateStub).to.have.been.calledOnce;
    });

    it('should not fire onUpdate when state has not changed', () => {
        filter.deactivate();
        expect(onUpdateStub).to.not.have.been.called;

        filter.activate();
        onUpdateStub.resetHistory();

        filter.activate();
        expect(onUpdateStub).to.not.have.been.called;
    });
});


const PRIMITIVE_1 = {id: 1, metadata: new Map([['indoor_covered', 'true']])} as any;
const PRIMITIVE_2 = {id: 2, metadata: new Map([['indoor_covered', 'false']])} as any;
const PRIMITIVE_3 = {id: 3, metadata: new Map()} as any;
const PRIMITIVE_4 = {id: 4} as any;
