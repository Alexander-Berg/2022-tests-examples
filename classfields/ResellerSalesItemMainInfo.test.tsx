import 'jest-enzyme';

import React from 'react';
import { shallow } from 'enzyme';
import MockDate from 'mockdate';

import { OfferPosition_OrderedPosition_Sort, OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import ResellerSalesItemMainInfo from './ResellerSalesItemMainInfo';
import type { Props } from './ResellerSalesItemMainInfo';

let props: Props;

beforeEach(() => {
    props = {
        isStatUnfolded: false,
        offer: offerMock,
        onOfferStateChange: jest.fn(),
        onPriceChange: jest.fn(),
        onPaymentModalOpen: jest.fn(),
        isControlsVisible: false,
        isVipActive: false,
    };
});

afterEach(() => {
    MockDate.reset();
});

describe('цвет часов', () => {
    it('обычный, если включено продление и осталось больше дня', () => {
        MockDate.set('2021-01-01');

        props.offer = cloneOfferWithHelpers(offerMock)
            .withExpireDate(String(new Date('2021-01-11T12:00:00Z').getTime()))
            .withCustomVas({ service: TOfferVas.PLACEMENT, prolongation_forced_not_togglable: true, prolongation_allowed: true })
            .withActiveVas([ TOfferVas.PLACEMENT ], { prolongable: true })
            .value();
        const page = shallowRenderComponent({ props });

        const daysLeft = page.find('.ResellerSalesItemMainInfo__cell_type_daysLeft');
        const icon = daysLeft.find('.ResellerSalesItemMainInfo__cellIcon');

        expect(icon.hasClass('ResellerSalesItemMainInfo__cellIcon_color_default')).toBe(true);
    });

    it('желтый, если включено продление и осталось меньше дня', () => {
        MockDate.set('2021-01-01T00:00:00Z');

        props.offer = cloneOfferWithHelpers(offerMock)
            .withExpireDate(String(new Date('2021-01-01T12:00:00Z').getTime()))
            .withCustomVas({ service: TOfferVas.PLACEMENT, prolongation_forced_not_togglable: true, prolongation_allowed: true })
            .withActiveVas([ TOfferVas.PLACEMENT ], { prolongable: true })
            .value();
        const page = shallowRenderComponent({ props });

        const daysLeft = page.find('.ResellerSalesItemMainInfo__cell_type_daysLeft');
        const icon = daysLeft.find('.ResellerSalesItemMainInfo__cellIcon');

        expect(icon.hasClass('ResellerSalesItemMainInfo__cellIcon_color_yellow')).toBe(true);
    });

    it('красный, если не включено продление', () => {
        MockDate.set('2021-01-01T00:00:00Z');

        props.offer = cloneOfferWithHelpers(offerMock)
            .withExpireDate(String(new Date('2021-01-11T12:00:00Z').getTime()))
            .withCustomVas({ service: TOfferVas.PLACEMENT, prolongation_forced_not_togglable: true, prolongation_allowed: true })
            .withActiveVas([ TOfferVas.PLACEMENT ], { prolongable: false })
            .value();
        const page = shallowRenderComponent({ props });

        const daysLeft = page.find('.ResellerSalesItemMainInfo__cell_type_daysLeft');
        const icon = daysLeft.find('.ResellerSalesItemMainInfo__cellIcon');

        expect(icon.hasClass('ResellerSalesItemMainInfo__cellIcon_color_red')).toBe(true);
    });

    it('обычный, размещение на 60 дней и осталось больше 7 дней', () => {
        MockDate.set('2021-01-01');

        props.offer = cloneOfferWithHelpers(offerMock)
            .withExpireDate(String(new Date('2021-01-11T12:00:00Z').getTime()))
            .withCustomVas({ service: TOfferVas.PLACEMENT, prolongation_forced_not_togglable: false })
            .value();
        const page = shallowRenderComponent({ props });

        const daysLeft = page.find('.ResellerSalesItemMainInfo__cell_type_daysLeft');
        const icon = daysLeft.find('.ResellerSalesItemMainInfo__cellIcon');

        expect(icon.hasClass('ResellerSalesItemMainInfo__cellIcon_color_default')).toBe(true);
    });

    it('желтый, размещение на 60 дней и осталось меньше 7 дней но больше дня', () => {
        MockDate.set('2021-01-01');

        props.offer = cloneOfferWithHelpers(offerMock)
            .withExpireDate(String(new Date('2021-01-03T12:00:00Z').getTime()))
            .withCustomVas({ service: TOfferVas.PLACEMENT, prolongation_forced_not_togglable: false })
            .value();
        const page = shallowRenderComponent({ props });

        const daysLeft = page.find('.ResellerSalesItemMainInfo__cell_type_daysLeft');
        const icon = daysLeft.find('.ResellerSalesItemMainInfo__cellIcon');

        expect(icon.hasClass('ResellerSalesItemMainInfo__cellIcon_color_yellow')).toBe(true);
    });

    it('красный, размещение на 60 дней и осталось меньше дня', () => {
        MockDate.set('2021-01-01');

        props.offer = cloneOfferWithHelpers(offerMock)
            .withExpireDate(String(new Date('2021-01-01T12:00:00Z').getTime()))
            .withCustomVas({ service: TOfferVas.PLACEMENT, prolongation_forced_not_togglable: false })
            .value();
        const page = shallowRenderComponent({ props });

        const daysLeft = page.find('.ResellerSalesItemMainInfo__cell_type_daysLeft');
        const icon = daysLeft.find('.ResellerSalesItemMainInfo__cellIcon');

        expect(icon.hasClass('ResellerSalesItemMainInfo__cellIcon_color_red')).toBe(true);
    });
});

describe('метрика при клике на линк со статой', () => {
    it('раскрытие', () => {
        props.isStatUnfolded = false;
        const page = shallowRenderComponent({ props });
        const link = page.find('.ResellerSalesItemMainInfo__column_stat .ResellerSalesItemMainInfo__cell:last-child');
        link.simulate('click');

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'offer-stats', 'show' ]);
    });

    it('скрытие', () => {
        props.isStatUnfolded = true;
        const page = shallowRenderComponent({ props });
        const link = page.find('.ResellerSalesItemMainInfo__column_stat .ResellerSalesItemMainInfo__cell:last-child');
        link.simulate('click');

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'offer-stats', 'close' ]);
    });
});

it('ссылка на листинг с поцицией оффера', () => {
    const newProps = {
        ...props,
        offer: cloneOfferWithHelpers(offerMock).withSearchPosition(3).value(),
    };

    const page = shallowRenderComponent({ props: newProps });
    const link = page.find('ResellerSalesItemSearchPosition');

    expect(link).toHaveProp(
        'url',
        'link/listing/?category=cars&mark=FORD&model=ECOSPORT&section=used&geo_radius=200&geo_id=213&scrollToPosition=3',
    );
});

describe('относительная позиция в поиске', () => {
    it('если данных нет, не покажет блок', () => {
        props.offer = cloneOfferWithHelpers(offerMock)
            .withSearchPositions([])
            .value();
        const page = shallowRenderComponent({ props });

        const block = page.find('RelativeSearchPositionPopup');
        expect(block.isEmptyRender()).toBe(true);
    });

    it('если оффер не активен, не покажет блок', () => {
        props.offer = cloneOfferWithHelpers(offerMock)
            .withStatus(OfferStatus.INACTIVE)
            .withSearchPositions([ {
                positions: [
                    { position: 12, sort: OfferPosition_OrderedPosition_Sort.SIMPLE_RELEVANCE, total_count: 101 },
                ],
                total_count: 1,
            } ])
            .value();
        const page = shallowRenderComponent({ props });

        const block = page.find('RelativeSearchPositionPopup');
        expect(block.isEmptyRender()).toBe(true);
    });

    it('если есть данные, покажет блок с корректной ссылкой', () => {
        props.offer = cloneOfferWithHelpers(offerMock)
            .withSearchPositions([ {
                positions: [
                    { position: 12, sort: OfferPosition_OrderedPosition_Sort.SIMPLE_RELEVANCE, total_count: 101 },
                ],
                total_count: 1,
            } ])
            .value();
        const page = shallowRenderComponent({ props });

        const block = page.find('RelativeSearchPositionPopup');
        expect(block.isEmptyRender()).toBe(false);
        expect(block.prop('linkUrl')).toBe(
            'link/listing/?category=cars&mark=FORD&model=ECOSPORT&section=used&geo_radius=200&geo_id=213&scrollToPosition=5&page=1',
        );
    });
});

function shallowRenderComponent({ props }: { props: Props }) {
    const ContextProvider = createContextProvider(contextMock);

    const page = shallow(
        <ContextProvider>
            <ResellerSalesItemMainInfo { ...props }/>
        </ContextProvider>,
    );

    return page.dive();
}
