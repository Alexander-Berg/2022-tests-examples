import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import {
    Multiposting_Classified_ClassifiedName as ClassifiedName,
    OfferStatus,
} from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import type { MultipostingClassified } from 'auto-core/types/Multiposting';

import type { CabinetOffer } from 'www-cabinet/react/types';

import OfferSnippetMultiposting from './OfferSnippetMultiposting';

const props = {
    applyService: () => {},
    canWriteSaleResource: true,
    clientId: 'someid',
    index: 0,
    isAgency: false,
    isClient: true,
    isSaleBanned: false,
    isSaleExpired: false,
    isSaleInactive: false,
    isSaleNeedActivation: false,
    isServicesDisabled: false,
    shouldShowButtonsWithoutText: false,
    showErrorNotification: () => {},
    showInfoNotification: () => {},
    updateClassified: () => {},
    confirm: () => {},
    isChecked: false,
    isSingle: false,
    onCheck: () => {},
    showCheckbox: false,
    offer: cloneOfferWithHelpers(offerMock).withMultiposting().value() as CabinetOffer,
    updateFavoriteDiscounts: () => {},
    bunkerFavorite: {},
};

describe('отобразит ошибку', () => {
    it('у неоплаченного на Авто.ру объявления', () => {
        const tree = shallow(<OfferSnippetMultiposting { ...props } isSaleExpired={ true }/>);
        expect(tree.find('.OfferSnippetMultiposting__errorList')).toMatchSnapshot();
    });

    it('у неактивированного на Авто.ру объявления', () => {
        const tree = shallow(<OfferSnippetMultiposting { ...props } isSaleNeedActivation={ true }/>);
        expect(tree.find('.OfferSnippetMultiposting__errorList')).toMatchSnapshot();
    });

    it('у заблокированного по одной причине на Авто.ру объявления', () => {
        const offer = _.cloneDeep(props.offer);
        offer.bansReasons = [
            { title: 'три шестьсот и пять шестьсот', text_lk_dealer: 'С какой стати?' },
        ];
        const tree = shallow(<OfferSnippetMultiposting { ...props } isSaleNeedActivation={ true }/>);

        expect(tree.find('.OfferSnippetMultiposting__errorList')).toMatchSnapshot();
    });

    it('у заблокированного по нескольким причинам на Авто.ру объявления', () => {
        const offer = _.cloneDeep(props.offer);
        offer.bansReasons = [
            {
                title: 'разрыв',
                text_lk_dealer: `Скажите мне пожалуйста, вот когда я вот подключился, в прошлом году, в ноябре месяце,
                    и до 26 апреля сего года не было ни единого разрыва, хотя у вас сессии должны были завершаться, и я не отключался, чем это было вызвано?`,
            },
            { title: 'заголовок бана 2', text_lk_dealer: 'текст бана 2' },
        ];
        const tree = shallow(<OfferSnippetMultiposting { ...props } isSaleNeedActivation={ true }/>);

        expect(tree.find('.OfferSnippetMultiposting__errorList')).toMatchSnapshot();
    });

    it('у объявления с расхождением пробега', () => {
        const offer = _.cloneDeep(props.offer);
        offer.tags = [ 'vin_offers_bad_mileage' ];
        const tree = shallow(<OfferSnippetMultiposting { ...props } offer={ offer }/>);

        expect(tree.find('.OfferSnippetMultiposting__errorList')).toMatchSnapshot();
    });

    it('у заблокированного на Авито объявления', () => {
        const offer = _.cloneDeep(props.offer);
        findClassified(offer, ClassifiedName.AVITO).status = OfferStatus.BANNED;
        const tree = shallow(<OfferSnippetMultiposting { ...props } offer={ offer }/>);

        expect(tree.find('.OfferSnippetMultiposting__errorList')).toMatchSnapshot();
    });
});

function findClassified(offer: CabinetOffer, classifiedName: ClassifiedName) {
    return offer?.multiposting?.classifieds
        .find((item: MultipostingClassified) => item.name === classifiedName) as MultipostingClassified;
}
