import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';
import 'jest-enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import OfferSnippetMainInfo from './OfferSnippetMainInfo';

const offerMockHelper = cloneOfferWithHelpers(cardMock);
const additionalContentMock = <div>Дополнительная инфа</div>;

const defaultProps = {
    index: 0,
    isChecked: false,
    isSingle: false,
    offer: offerMockHelper.value(),
    onCheck: () => {},
    additionalContent: additionalContentMock,
    isAgency: false,
    isClient: true,
    canWriteSaleResource: true,
};

it('покажет плейсхолдер, если у объявления нет фотографий', () => {
    const tree = renderComponent({
        ...defaultProps,
        offer: offerMockHelper.withImages([]).value(),
    });

    expect(tree.find('SalePhotoPlaceholder')).toExist();
});

describe('бейджи', () => {
    it('не отрендерит контейнер для бейджей, если нет ни одного бейджа', () => {
        const tree = renderComponent();
        expect(tree.find('.OfferSnippetMainInfo__badgeContainer')).not.toExist();
    });

    describe('покажет бейдж, если для этого есть условия', () => {

        it('забронировано', () => {
            const tree = renderComponent({
                ...defaultProps,
                offer: offerMockHelper.withBooking().value(),
            });

            expect(tree.find('.OfferSnippetMainInfo__badgeContainer').text()).toContain('Забронировано');
        });

        it('доступно для бронирования', () => {
            const tree = renderComponent({
                ...defaultProps,
                offer: offerMockHelper.withBookingStatus().value(),
            });

            expect(tree.find('.OfferSnippetMainInfo__badgeContainer').text()).toContain('Можно забронировать');
        });

        it('В пути', () => {
            const tree = renderComponent({
                ...defaultProps,
                offer: offerMockHelper.withCategory('cars').withSection('new').withAvailability('ON_ORDER').value(),
            });

            expect(tree.find('.OfferSnippetMainInfo__badgeContainer').text()).toContain('В пути');
        });

        it('панорама', () => {
            const tree = renderComponent({
                ...defaultProps,
                offer: offerMockHelper.withPanoramaExterior().value(),
            });

            expect(tree.find('.OfferSnippetMainInfo__badge_panorama')).toExist();
        });

    });
});

function renderComponent(props = defaultProps): ShallowWrapper {
    return shallow(
        <OfferSnippetMainInfo { ...props }/>,
        { context: { ...contextMock, linkToDesktop: () => {} } },
    );
}
