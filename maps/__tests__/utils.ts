import {run} from 'bottender/dist/bot/Bot';
import {ContextSimulator} from 'bottender/dist/test-utils';

const simulators = ['telegram', 'q'].map((name) => ({
    name,
    create: () => new ContextSimulator({
        platform: name
    })
}));

export {simulators, run};
