import * as pg from 'pg';
import {config} from 'src/config';

export const masterConnectionOptions: pg.ClientConfig = {
    host: config['db.hosts'][0],
    port: config['db.port'],
    database: config['db.name'],
    user: config['db.user'],
    password: config['db.password']
};
