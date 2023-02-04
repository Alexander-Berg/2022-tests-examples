jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen', () => {
    return jest.fn(() => () => { });
});

import React from 'react';
import { shallow } from 'enzyme';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import DateMock from 'autoru-frontend/mocks/components/DateMock';

import { disk, nbsp } from 'auto-core/react/lib/html-entities';
import getServiceInfoMerged from 'auto-core/react/lib/offer/getServiceInfoMerged';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import paymentModalOpen from 'auto-core/react/dataDomain/state/actions/paymentModalOpen';
import type { TSalesServiceInfo } from 'auto-core/react/lib/offer/getServicePricesForPrivateAndReseller';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';
import dayjs from 'auto-core/dayjs';
import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import OwnerVasItem from './OwnerVasItem';

const paymentModalOpenMock = paymentModalOpen as jest.MockedFunction<typeof paymentModalOpen>;
const Context = createContextProvider(contextMock);

describe('модал оплаты по клику на ВАС', () => {
    it('откроет модал с нужными параметрами', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withActiveVas([])
            .withCustomVas({ service: TOfferVas.VIP, price: 9999 })
            .value();

        const page = renderShallowWrapper({ offer, service: getServiceInfoMerged(offer, TOfferVas.VIP) });

        page.find('.OwnerVasItem').simulate('click');

        expect(paymentModalOpen).toHaveBeenCalledWith({
            category: 'cars',
            from: 'new-lk-tab',
            offerId: '1085562758-1970f439',
            platform: 'PLATFORM_DESKTOP',
            services: [
                {
                    service: 'package_vip',
                },
            ],
            shouldShowSuccessTextAfter: true,
            shouldUpdateOfferAfter: true,
            shouldUpdateUserOffersAfter: false,
            successText: 'Опция успешно активирована',
        });
    });

    it('откроет модал с параметрами shouldUpdateOfferAfter=false и shouldUpdateUserOffersAfter=true, если есть скидка на ВАС и нет isCardPage', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withActiveVas([])
            .value();

        const page = renderShallowWrapper({ offer, service: getServiceInfoMerged(offer, TOfferVas.VIP) });

        page.find('.OwnerVasItem').simulate('click');

        expect(paymentModalOpenMock.mock.calls[0][0].shouldUpdateOfferAfter).toEqual(false);
        expect(paymentModalOpenMock.mock.calls[0][0].shouldUpdateUserOffersAfter).toEqual(true);
    });

    it('откроет модал с параметрами shouldUpdateOfferAfter=true и shouldUpdateUserOffersAfter=false, если есть скидка на ВАС и есть isCardPage', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withActiveVas([])
            .value();

        const page = renderShallowWrapper({ offer, service: getServiceInfoMerged(offer, TOfferVas.VIP), isCardPage: true });

        page.find('.OwnerVasItem').simulate('click');

        expect(paymentModalOpenMock.mock.calls[0][0].shouldUpdateOfferAfter).toEqual(true);
        expect(paymentModalOpenMock.mock.calls[0][0].shouldUpdateUserOffersAfter).toEqual(false);
    });

    it('откроет модал с параметрами shouldUpdateOfferAfter=true и shouldUpdateUserOffersAfter=false, если нет скидки на ВАС и нет isCardPage', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withActiveVas([])
            .withCustomVas({ service: TOfferVas.VIP, price: 9999 })
            .value();

        const page = renderShallowWrapper({ offer, service: getServiceInfoMerged(offer, TOfferVas.VIP) });

        page.find('.OwnerVasItem').simulate('click');

        expect(paymentModalOpenMock.mock.calls[0][0].shouldUpdateOfferAfter).toEqual(true);
        expect(paymentModalOpenMock.mock.calls[0][0].shouldUpdateUserOffersAfter).toEqual(false);
    });

    it('не откроет, если сервис disabled', () => {
        const offer = cloneOfferWithHelpers(offerMock).withActiveVas([]).value();
        const service = {
            ...getServiceInfoMerged(offer, TOfferVas.VIP),
            disabled: true,
        };

        const page = renderShallowWrapper({ offer, service });

        page.find('.OwnerVasItem').simulate('click');

        expect(paymentModalOpenMock).not.toHaveBeenCalled();
    });

    it('не откроет, если сервис активный', () => {
        const offer = cloneOfferWithHelpers(offerMock).withActiveVas([]).value();
        const service = {
            ...getServiceInfoMerged(offer, TOfferVas.VIP),
            is_active: true,
        };

        const page = renderShallowWrapper({ offer, service });

        page.find('.OwnerVasItem').simulate('click');

        expect(paymentModalOpenMock).not.toHaveBeenCalled();
    });

    it('не откроет, если сервис входит в активный пакет', () => {
        const offer = cloneOfferWithHelpers(offerMock).withActiveVas([ TOfferVas.VIP ]).value();
        const service = {
            ...getServiceInfoMerged(offer, TOfferVas.TOP),
            activeParentPackage: TOfferVas.VIP,
        };

        const page = renderShallowWrapper({ offer, service });

        page.find('.OwnerVasItem').simulate('click');

        expect(paymentModalOpenMock).not.toHaveBeenCalled();
    });

    it('откроет, если сервис активный и FRESH', () => {
        const offer = cloneOfferWithHelpers(offerMock).withActiveVas([ TOfferVas.FRESH ]).value();
        const service = {
            ...getServiceInfoMerged(offer, TOfferVas.FRESH),
            is_active: true,
        };

        const page = renderShallowWrapper({ offer, service });

        page.find('.OwnerVasItem').simulate('click');

        expect(paymentModalOpenMock).toHaveBeenCalled();
    });

    it('не будет открывать модал, если сервис Поднятие и ВИП активен', () => {
        const offer = cloneOfferWithHelpers(offerMock).withActiveVas([ TOfferVas.VIP ]).value();
        const service = {
            ...getServiceInfoMerged(offer, TOfferVas.FRESH),
            is_active: true,
        };

        const page = renderShallowWrapper({ offer, service });

        page.find('.OwnerVasItem').simulate('click');

        expect(paymentModalOpen).not.toHaveBeenCalled();
    });
});

describe('правильно рендерит description', () => {
    it('когда неактивный ВАС', () => {
        const offer = cloneOfferWithHelpers(offerMock).withActiveVas([]).value();

        const page = renderShallowWrapper({ offer, service: getServiceInfoMerged(offer, TOfferVas.TOP) });

        expect(page.find('.OwnerVasItem__description').html()).toContain('Поднятие в ТОП');
    });

    it('когда неактивный VIP', () => {
        const offer = cloneOfferWithHelpers(offerMock).withActiveVas([]).value();

        const page = renderShallowWrapper({ offer, service: getServiceInfoMerged(offer, TOfferVas.VIP) });

        expect(page.find('.OwnerVasItem__description').html()).toContain(`VIP ${ disk } 60${ nbsp }дней`);
    });

    it('когда включен в активный пакет', () => {
        const offer = cloneOfferWithHelpers(offerMock).withActiveVas([ TOfferVas.VIP ]).value();
        const service = {
            ...getServiceInfoMerged(offer, TOfferVas.TOP),
            is_active: true,
            prolongable: true,
        };

        const page = renderShallowWrapper({ offer, service });

        expect(page.find('.OwnerVasItem__description').text()).toEqual('Автопродление');
    });

    it('когда активный ВАС FRESH', () => {
        const offer = cloneOfferWithHelpers(offerMock).withActiveVas([ TOfferVas.VIP ]).value();
        const service = {
            ...getServiceInfoMerged(offer, TOfferVas.FRESH),
            is_active: true,
            create_date: String(dayjs('2021-04-01').valueOf()),
        };

        const page = renderShallowWrapper({ offer, service });

        expect(page.find('.OwnerVasItem__description').text()).toEqual('Подключено вчера');
    });

    it('когда активный ВАС без автопродления', () => {
        const offer = cloneOfferWithHelpers(offerMock).withActiveVas([ TOfferVas.VIP ]).value();
        const service = {
            ...getServiceInfoMerged(offer, TOfferVas.TOP),
            is_active: true,
            expire_date: String(dayjs('2021-04-04').valueOf()),
        };

        const page = renderShallowWrapper({ offer, service });

        expect(page.find('.OwnerVasItem__description').text()).toEqual('Активно ещё<DaysLeft />');
        expect(page.find('.OwnerVasItem__description').find('DaysLeft').dive().text()).toEqual(`2${ nbsp }дня`);
    });
});

function renderShallowWrapper({ isCardPage, offer, service }: { offer: Offer; service: TSalesServiceInfo; isCardPage?: boolean }) {
    return shallow(
        <Context>
            <DateMock date="2021-04-02">
                <OwnerVasItem
                    offer={ offer }
                    openPaymentModal={ paymentModalOpenMock }
                    pageType="sales"
                    service={ service }
                    onAutoProlongationToggle={ jest.fn() }
                    onAutoRenewChange={ jest.fn() }
                    updatePricesHandler={ jest.fn() }
                    loadStatsForOffer={ jest.fn() }
                    hasTiedCards
                    isCardPage={ isCardPage }
                />
            </DateMock>
        </Context>,
    ).dive().dive();
}
