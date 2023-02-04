jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});
jest.mock('auto-core/react/dataDomain/state/actions/authModalWithCallbackOpen', () => jest.fn().mockReturnValue({ type: '' }));
jest.mock('auto-core/react/dataDomain/garage/actions/addDreamcarByVehicleInfo', () => jest.fn().mockReturnValue({ type: '' }));

import { renderHook, act } from '@testing-library/react-hooks';
import { useDispatch, useSelector } from 'react-redux';
import _ from 'lodash';

import mockStore from 'autoru-frontend/mocks/mockStore';
import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';
import contextMock from 'autoru-frontend/mocks/contextMock';
import listing from 'autoru-frontend/mockData/state/listing';

import mockUser from 'auto-core/react/dataDomain/user/mocks';
import catalogTechParam from 'auto-core/react/dataDomain/catalogTechParam/mocks/catalogTechParam.mock';
import useElectroCard from 'auto-core/react/components/common/PageElectroCard/useElectroCard';
import { journalArticleMock, journalArticlesMock } from 'auto-core/react/dataDomain/journalArticles/mocks';
import { Category } from 'auto-core/react/dataDomain/journalArticles/types';
import breadcrumbsPublicApi from 'auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock';
import geo from 'auto-core/react/dataDomain/geo/mocks/geo.mock';
import garage from 'auto-core/react/dataDomain/garage/mocks/garage';
import authModalWithCallbackOpen from 'auto-core/react/dataDomain/state/actions/authModalWithCallbackOpen';
import addDreamcarByVehicleInfo from 'auto-core/react/dataDomain/garage/actions/addDreamcarByVehicleInfo';

import type TContext from 'auto-core/types/TContext';
import type { Card } from 'auto-core/types/proto/auto/api/vin/garage/garage_api_model';

const defaultState = {
    bunker: getBunkerMock([ 'promo/electro' ]),
    journalArticles: journalArticlesMock.withArticles(
        _.times(10, () => journalArticleMock.withCategory(Category.MAIN).value()),
    ).value(),
    listing,
    breadcrumbsPublicApi,
    catalogTechParam,
    geo,
    garage,
    config: {
        data: {
            pageParams: {
                mark: 'audi',
                model: 'e_tron',
                super_gen: '21447469',
                configuration_id: '21447519',
                tech_param_id: '22291114',
            },
        },
    },
    user: mockUser.withAuth(false).value(),
};

describe('гараж', () => {

    describe('isModelInGarage', () => {

        it('вернёт false, если модель не в гараже', () => {
            mockRedux();
            const { result } = render();
            expect(result.current.isModelInGarage).toEqual(false);
        });

        it('вернёт true, если модель в гараже', () => {
            const newState = _.cloneDeep(defaultState);
            newState.garage.data.listing = [
                {
                    vehicle_info: {
                        car_info: {
                            mark: 'AUDI',
                            model: 'E_TRON',
                            super_gen_id: '21447469',
                            configuration_id: '21447519',
                            tech_param_id: '22291114',
                        },
                    },
                } as Card,
            ];
            mockRedux(newState);
            const { result } = render();
            expect(result.current.isModelInGarage).toEqual(true);
        });

    });

    describe('onGarageClick', () => {

        it('откроет модал авторизации, если не авторизован', () => {
            mockRedux();
            const { result } = render();
            act(() => {
                result.current.onGarageClick();
            });
            expect(authModalWithCallbackOpen).toHaveBeenCalledTimes(1);
        });

        it('добавит тачку в гараж, если она не в гараже', () => {
            mockRedux({
                ...defaultState,
                user: mockUser.withAuth(true).value(),
            });
            const { result } = render();
            act(() => {
                result.current.onGarageClick();
            });
            expect(addDreamcarByVehicleInfo).toHaveBeenCalledTimes(1);
        });

        it('отправит в гараж, если тачка в гараже', () => {
            const newState = _.cloneDeep(defaultState);
            newState.garage.data.listing = [
                {
                    vehicle_info: {
                        car_info: {
                            mark: 'AUDI',
                            model: 'E_TRON',
                            super_gen_id: '21447469',
                            configuration_id: '21447519',
                            tech_param_id: '22291114',
                        },
                    },
                } as Card,
            ];
            newState.user = mockUser.withAuth(true).value();
            mockRedux(newState);
            const { result } = render();
            act(() => {
                result.current.onGarageClick();
            });
            expect(contextMock.pushState).toHaveBeenCalledWith('link/garage-card/?card_id=', { loadData: true });
        });

    });

});

it('сравнение моделей, вместо удаления отправит в сравнение моделей', () => {
    mockRedux();
    const { result } = render();
    act(() => {
        result.current.onRemoveFromCompareClick();
    });
    expect(contextMock.pushState).toHaveBeenCalledWith('link/compare/?content=models', { loadData: true });
});

function render() {
    return renderHook(() => useElectroCard(contextMock as unknown as TContext));
}

function mockRedux(state = defaultState) {
    const store = mockStore(state);

    (useDispatch as jest.MockedFunction<typeof useDispatch>).mockReturnValue(
        (...args) => store.dispatch(...args),
    );

    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation(
        (selector) => selector(store.getState()),
    );
}
