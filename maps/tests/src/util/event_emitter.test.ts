import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import {expect} from 'chai';
import {spy} from 'sinon';
import {EventTrigger} from '../../../src/vector_render_engine/util/event_emitter';

chai.use(sinonChai);

describe('util/EventTrigger', () => {

    it('should work as people would expect', () => {
        const listener = spy();
        const eventEmitter = new EventTrigger<[number, string]>();

        eventEmitter.addListener(listener, 999);
        eventEmitter.fire(1, "2");
        expect(listener).to.have.been.calledWith(1, "2");
        expect(listener).to.have.been.calledOn(999);
        listener.reset();

        eventEmitter.fire(3, "4");
        expect(listener).to.have.been.calledWith(3, "4");
        listener.reset();

        eventEmitter.removeListener(listener);
        eventEmitter.fire(0, "0");
        expect(listener).to.not.have.been.called;
    });

});
