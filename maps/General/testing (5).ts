import Config from './interface';
import production from './production';

const testing: Config = {
    ...production,
    hosts: {
        ...production.hosts,
        environment: 'testing'
    },
    yasm: {
        ...production.yasm,
        accountTemplate: 'maps_core_quotateka_testing_client_dashboard'
    }
};

export default testing;
