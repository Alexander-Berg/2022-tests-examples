const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const block = require('./go2group');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен сделать редирект на групповую карточку', () => {
    const params = {
        currency: 'RUR',
        custom_state_key: 'CLEARED',
        dealer_org_type: [ '1', '2', '4' ],
        geo_radius: '1000',
        image: 'true',
        in_stock: 'false',
        rid: '2',
        state: [ 'NEW', 'USED' ],
        mark: 'TOYOTA',
        model: 'RAV_4',
        tech_param_id: '21678504',
        complectation_id: '21678548',
        offer_id: '1093402072-2c98f693',
        sort: 'cr_date-desc',
        utm_source: 'email_newoffers',
        utm_medium: 'email',
        utm_content: 'group-click-offer',
    };
    return de.run(block, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'SUBSCRIPTIONS_GO_TO_GROUP',
                    id: 'REDIRECTED',
                    // eslint-disable-next-line max-len
                    location: '/cars/all/group/toyota/rav_4/21678504/21678548/?sale_id=1093402072&sale_hash=2c98f693&currency=RUR&geo_radius=1000&in_stock=ANY_STOCK&rid=2&sort=cr_date-desc&utm_source=email_newoffers&utm_medium=email&utm_content=group-click-offer&customs_state_group=CLEARED',
                },
            });
        });
});

it('должен сделать редирект на морду, если произошла ошибка', () => {
    // специально убираем offer_id, чтобы вызвать jserror
    const params = {
        currency: 'RUR',
        custom_state_key: 'CLEARED',
        dealer_org_type: [ '1', '2', '4' ],
        geo_radius: '1000',
        image: 'true',
        in_stock: 'false',
        rid: '2',
        state: [ 'NEW', 'USED' ],
        mark: 'TOYOTA',
        model: 'RAV_4',
        tech_param_id: '21678504',
        complectation_id: '21678548',
        sort: 'cr_date-desc',
        utm_source: 'email_newoffers',
        utm_medium: 'email',
        utm_content: 'group-click-offer',
    };
    return de.run(block, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'SUBSCRIPTIONS_GO_TO_GROUP',
                    id: 'REDIRECTED',
                    location: '/',
                },
            });
        });
});
