import Config from './interface';
import production from './production';

const testing: Config = {
    ...production,
    tvm: {
        source: 2017303,
        destinations: {
            search: 2008261,
            ugcSearch: 2000870
        },
        blackboxEnvironment: 'Test',
        secret: {
            id: 'sec-01dveermjkftdb47c9ty4q37fz',
            version: 'ver-01dveermkg9zdczv4eb03hyeft',
            variableName: 'client_secret'
        }
    }
};

export default testing;
