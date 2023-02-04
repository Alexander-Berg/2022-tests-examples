import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import {expect} from 'chai';
import {spy, stub, SinonStub} from 'sinon';
import {Pool} from '../../../src/vector_render_engine/util/pool';

chai.use(sinonChai);

class StringContainer {
    str: string = '';

    destroy(): void {}

    reset(): void {}
}

describe('Pool', () => {
    let pool: Pool<StringContainer>;
    let factoryStub: SinonStub;

    beforeEach(() => {
        factoryStub = stub().callsFake(() => new StringContainer());
        pool = new Pool(1, factoryStub);
    });

    it('should acquire and release objects', () => {
        const obj = pool.acquire();
        expect(factoryStub).to.have.been.calledOnce;

        const resetSpy = spy(obj, 'reset');

        pool.release(obj);
        expect(resetSpy).to.have.been.calledOnce;
    });

    it('should destroy object when capacity is reached', () => {
        const obj1 = pool.acquire();
        const obj2 = pool.acquire();

        const destroySpy = spy(obj2, 'destroy');

        pool.release(obj1);
        pool.release(obj2);

        expect(destroySpy).to.have.been.calledOnce;
    });

    it('should destroy pool', () => {
        const obj = pool.acquire();
        const destroySpy = spy(obj, 'destroy');
        pool.release(obj);

        pool.destroy();
        expect(destroySpy).to.have.been.calledOnce;
    });

    it('should fail on releasing unused object', () => {
        const obj = new StringContainer();
        expect(() => pool.release(obj)).to.throw;
    });

    it('should fail on destroying pool while objects are in use', () => {
        pool.acquire();
        expect(() => pool.destroy()).to.throw;
    });
});
