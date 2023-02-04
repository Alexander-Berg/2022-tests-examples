import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import {expect} from 'chai';
import {spy} from 'sinon';
import {asyncImmediate} from '../../../src/vector_render_engine/util/function';

chai.use(sinonChai);

describe('util/function', () => {
     describe('asyncImmediate', () => {
        it('should run asynchronously from microtask queue', (done) => {
            const action = spy();
            const wrapper = asyncImmediate(action);
            wrapper();
            setTimeout(() => {
                expect(action).to.have.been.calledOnce;
                done();
            });
        });

        it('should throttle', (done) => {
            const action = spy();
            const wrapper = asyncImmediate(action, true);
            wrapper();
            wrapper();
            wrapper();
            setTimeout(() => {
                expect(action).to.have.been.calledOnce;
                done();
            });
        });
    });

});
