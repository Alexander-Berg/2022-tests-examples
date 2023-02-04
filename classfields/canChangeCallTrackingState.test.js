const _ = require('lodash');

const canChangeCallTrackingState = require('./canChangeCallTrackingState');

const clientMock = require('www-cabinet/react/dataDomain/client/mocks/active.mock');

const CUSTOMER_ROLES = require('www-cabinet/data/client/customer-roles');

let state;

beforeEach(() => {
    state = {
        bunker: {
            'cabinet/calltracking_regions_mandatory': [],
        },
        client: _.cloneDeep(clientMock),
        config: { customerRole: CUSTOMER_ROLES.client },
    };
});

it('должен отдавать true, если клиент находится в регионе без принудительного коллтрекинга', () => {
    state.bunker['cabinet/calltracking_regions_mandatory'].push(100);

    const result = canChangeCallTrackingState(state);

    expect(result).toBe(true);
});

it('должен отдавать false, если клиент находится в регионе с принудительным коллтрекингом', () => {
    state.bunker['cabinet/calltracking_regions_mandatory'].push(1);

    const result = canChangeCallTrackingState(state);

    expect(result).toBe(false);
});

it('должен отдавать true, если клиент находится в регионе с принудительным коллтрекингом и текущая роль - менеджер', () => {
    state.bunker['cabinet/calltracking_regions_mandatory'].push(1);
    state.config.customerRole = CUSTOMER_ROLES.manager;

    const result = canChangeCallTrackingState(state);

    expect(result).toBe(true);
});
