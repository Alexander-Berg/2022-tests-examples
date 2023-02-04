jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(() => jest.fn()),
    };
});
jest.mock('auto-core/react/dataDomain/cookies/actions/set');

import { renderHook } from '@testing-library/react-hooks';
import { useSelector } from 'react-redux';

import createLinkMock from 'autoru-frontend/mocks/createLinkMock';

import setCookie from 'auto-core/react/dataDomain/cookies/actions/set';
import configMock from 'auto-core/react/dataDomain/config/mock';
import userMock from 'auto-core/react/dataDomain/user/mocks';
import { COOKIE_NAME as SAFE_DEAL_COOKIE_NAME } from 'auto-core/react/components/desktop/AutoPopupLoader/items/safe_deal_seller_onboarding_modal';
import { COOKIE_NAME as NPS_MODAL_COOKIE_NAME } from 'auto-core/react/components/desktop/AutoPopupLoader/items/nps_modal';

import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';
import type { AppState } from 'www-poffer/react/store/AppState';

import useRedirectAfterSubmit from './useRedirectAfterSubmit';

let defaultState: Partial<AppState>;

beforeEach(() => {
    defaultState = {
        config: configMock.value(),
        cookies: {},
        offerDraft: offerDraftMock.value(),
    };

    jest.spyOn(global.location, 'assign').mockImplementation(() => {});
});

describe('после размещения для частника', () => {
    it('правильно делает редирект', () => {
        const state = {
            ...defaultState,
            config: configMock.withPageParams({ form_type: 'add' }).value(),
        };
        const redirectAfterSubmit = renderCustomHook({ state });
        redirectAfterSubmit('offer_id-offer_hash');

        expect(global.location.assign).toHaveBeenCalledTimes(1);
        expect(global.location.assign).toHaveBeenCalledWith(
            'link/card/?category=cars&section=used&mark=FORD&model=ECOSPORT&sale_id=offer_id&sale_hash=offer_hash&page_from=add-page',
        );
    });

    describe('БС кука', () => {
        it('поставит куки про БС, если ее нет', () => {
            const state = {
                ...defaultState,
                config: configMock.withPageParams({ form_type: 'add' }).value(),
            };
            const redirectAfterSubmit = renderCustomHook({ state });
            redirectAfterSubmit('offer_id-offer_hash');

            expect(setCookie).toHaveBeenCalledTimes(2);
            expect(setCookie).toHaveBeenCalledWith(SAFE_DEAL_COOKIE_NAME, '0', { expires: 30 });
        });

        it('не поставит куки про БС и НПС, если они уже стоят', () => {
            const state = {
                ...defaultState,
                config: configMock.withPageParams({ form_type: 'add' }).value(),
                cookies: { [SAFE_DEAL_COOKIE_NAME]: '-1', [NPS_MODAL_COOKIE_NAME]: 'offer_published' },
            };
            const redirectAfterSubmit = renderCustomHook({ state });
            redirectAfterSubmit('offer_id-offer_hash');

            expect(setCookie).toHaveBeenCalledTimes(0);
        });
    });

    describe('НПС кука', () => {
        it('поставит куки про НПС, если ее нет', () => {
            const state = {
                ...defaultState,
                config: configMock.withPageParams({ form_type: 'add' }).value(),
            };
            const redirectAfterSubmit = renderCustomHook({ state });
            redirectAfterSubmit('offer_id-offer_hash');

            expect(setCookie).toHaveBeenCalledTimes(2);
            expect(setCookie).toHaveBeenCalledWith(NPS_MODAL_COOKIE_NAME, 'offer_published', { expires: 30 });
        });
    });
});

describe('редирект после редактирования', () => {

    describe('дилер', () => {
        it('desktop', () => {
            const state = {
                ...defaultState,
                config: configMock.withPageParams({ form_type: 'edit', sale_id: 'sale_id', sale_hash: 'sale_hash' }).value(),
                user: userMock.withDealer(true).value(),
            };

            const redirectAfterSubmit = renderCustomHook({ state });
            redirectAfterSubmit('offer_id-offer_hash');

            expect(global.location.assign).toHaveBeenCalledTimes(1);
            expect(global.location.assign).toHaveBeenCalledWith(
                'link/card/?category=cars&section=used&mark=FORD&model=ECOSPORT&sale_id=offer_id&sale_hash=offer_hash&page_from=edit-page',
            );
        });
    });

    describe('частник', () => {
        it('desktop', () => {
            const state = {
                ...defaultState,
                config: configMock.withPageParams({ form_type: 'edit', sale_id: 'sale_id', sale_hash: 'sale_hash' }).value(),
            };

            const redirectAfterSubmit = renderCustomHook({ state });
            redirectAfterSubmit('offer_id-offer_hash');

            expect(global.location.assign).toHaveBeenCalledTimes(1);
            expect(global.location.assign).toHaveBeenCalledWith(
                'link/vas-landing/?category=cars&sale_id=sale_id&sale_hash=sale_hash',
            );
        });

        it('mobile', () => {
            const state = {
                ...defaultState,
                config: configMock
                    .withPageParams({ form_type: 'edit', sale_id: 'sale_id', sale_hash: 'sale_hash' })
                    .withBrowser({ isMobile: true })
                    .value(),
            };

            const redirectAfterSubmit = renderCustomHook({ state });
            redirectAfterSubmit('offer_id-offer_hash');

            expect(global.location.assign).toHaveBeenCalledTimes(1);
            expect(global.location.assign).toHaveBeenCalledWith(
                'link/card/?category=cars&section=used&mark=FORD&model=ECOSPORT&sale_id=offer_id&sale_hash=offer_hash&page_from=edit-page',
            );
        });
    });

});

function renderCustomHook({ state }: { state: Partial<AppState> }) {
    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation((selector) => selector(state));

    const { result } = renderHook(() => useRedirectAfterSubmit(createLinkMock('link')));

    return result.current;
}
