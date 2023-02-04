import {simulators, run} from './utils';
import {handlers} from '../../handlers';
import {Context} from '../../types/context';

describe('Command /ping', () => {
    simulators.forEach(({name, create}) => {
        describe(`via ${name}`, () => {
            it('should work', async () => {
                const simulator = create();
                const context = simulator.createTextContext('/ping') as Context;

                await run(handlers)(context, {});

                expect(context.sendText).toBeCalledWith('pong');
            });

            it('should work with long text', async () => {
                const context = create().createTextContext('/ping for my mom') as Context;

                await run(handlers)(context, {});

                expect(context.sendText).toBeCalledWith('pong');
            });
        });
    });
});
