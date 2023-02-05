import {Config} from './interfaces';
import production from './production';

const integrationalTesting: Config = {
    ...production,
    db: {
        ...production.db,
        hosts: [
            process.env.DB_TESTING_HOST as string
        ],
        database: 'template1',
        port: 5439,
        ssl: undefined
    },
    checkTvmServiceTicket: false
};

export default integrationalTesting;
