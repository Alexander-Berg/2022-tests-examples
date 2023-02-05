import Config from './interface';
import production from './production';

const testing: Config = {
    ...production,
    hosts: {
        ...production.hosts,
        environment: 'testing'
    }
};

export default testing;
