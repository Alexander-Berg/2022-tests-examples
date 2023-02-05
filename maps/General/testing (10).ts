import {Config} from './interfaces';
import production from './production';

const testing: Config = {
    ...production,
    db: {
        ...production.db,
        hosts: [
            'man-m1oqdax1pftehtjv.db.yandex.net',
            'vla-fra7jx49dmntojrg.db.yandex.net'
        ],
        database: 'maps_moira_int_testing'
    },
    tvm: {
        daemonUrl: process.env.DEPLOY_TVM_TOOL_URL || 'http://localhost:1',
        authToken: process.env.TVMTOOL_LOCAL_AUTHTOKEN,
        clientProjectsMap: new Map([
            [2012180, ['alfred', 'alfred-log', 'alfred-payment']],
            [2012278, ['alfred', 'alfred-log', 'alfred-payment']],
            [2012734, ['test']],
            [2013878, ['toyota']],
            [2018658, ['toyota']],
            [2015345, ['gourmet_form', 'gourmet_code']],
            [2015997, ['renins', 'achievements']],
            [2015993, ['hankook']],
            [2019918, ['uralsib_unsorted', 'uralsib_moderated']],
            [2020639, ['uralsib_unsorted', 'uralsib_moderated']],
            [2007045, ['auth-landing']],
            [2019888, ['spec-admin-roles', 'spec-admin-navi', 'spec-admin-pictures']],
            [2024705, ['wargaming2020', 'wargaming2020_logs']],
            [2013874, [
                'hankook',
                'renins',
                'gourmet_form',
                'gourmet_code',
                'toyota',
                'auth-landing',
                'wargaming2020',
                'renins-fragile',
                'mts_ecosystem',
                'renins-rock'
            ]],
            // БЯК testing
            [2001692, ['maps-front-maps_discounts']],
            // БЯК development
            [2009603, ['maps-front-maps_discounts']],
            [2026398, ['ingrad2021', 'ingrad-emails', 'ingrad-phones', 'ingrad-pseudoids']],
            [2027007, ['renins-fragile']],
            [2029120, ['mts_ecosystem']],
            [2033395, ['renins-rock']]
        ])
    }
};

export default testing;
