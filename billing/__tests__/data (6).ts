import { HOST } from 'common/utils/test-utils/common';
import { Permissions } from '../../../../common/constants';

export const intercompanies = {
    request: {
        url: `${HOST}/firm/intercompany_list`
    },
    response: [
        { cc: 'AM31', is_active: true, label: 'Yandex.Taxi AM' },
        { cc: 'AM32', is_active: true, label: 'Yandex.Taxi Corp AM' },
        { cc: 'AZ35', is_active: true, label: 'Uber Azerbaijan' }
    ]
};

export const services = {
    request: {
        url: `${HOST}/service/list`,
        data: {}
    },
    response: [
        {
            cc: 'adfox',
            url_orders: 'http://adfox.ru',
            in_contract: true,
            id: 102,
            name: 'ADFox.ru'
        },
        {
            cc: 'afisha_moviepass',
            url_orders: null,
            in_contract: true,
            id: 617,
            name: 'Afisha.MoviePass'
        },
        {
            cc: 'bug_bounty',
            url_orders: null,
            in_contract: true,
            id: 207,
            name: 'Bug bounty'
        }
    ]
};

export const fullPerms = Object.values(Permissions);
