import Config from './interface';
import production from './production';

const testing: Config = {
    ...production,
    hosts: {
        ...production.hosts,
        environment: 'testing'
    },
    tvm: {
        ...production.tvm,
        clientId: 2033395,
        dsts: {
            ...production.tvm.dsts,
            moira: '2012240'
        }
    },
    bunker: {
        ...production.bunker,
        url: 'http://bunker-api-dot.yandex.net',
        version: 'latest',
        cacheTtlMs: 30000 // 30 second
    }
};

export default testing;
