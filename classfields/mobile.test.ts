import type { TechParam } from '@vertis/schema-registry/ts-types-snake/auto/api/catalog_model';
import { Car_EngineType } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_model';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import groupCard from 'autoru-frontend/mockData/state/groupCard.mock';

import cardCars from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import cardCarsElectro from 'auto-core/react/dataDomain/card/mocks/card.cars.electro.mock';
import cardAtv from 'auto-core/react/dataDomain/card/mocks/card.atv.mock';
import cardMotorcycle from 'auto-core/react/dataDomain/card/mocks/card.motorcycle.mock';
import cardScooters from 'auto-core/react/dataDomain/card/mocks/card.scooters.mock';
import cardSnowmobile from 'auto-core/react/dataDomain/card/mocks/card.snowmobile.mock';
import cardAgricultural from 'auto-core/react/dataDomain/card/mocks/card.agricultural.mock';
import cardArtic from 'auto-core/react/dataDomain/card/mocks/card.artic.mock';
import cardAutoloader from 'auto-core/react/dataDomain/card/mocks/card.autoloader.mock';
import cardBulldozers from 'auto-core/react/dataDomain/card/mocks/card.bulldozers.mock';
import cardBus from 'auto-core/react/dataDomain/card/mocks/card.bus.mock';
import cardConstruction from 'auto-core/react/dataDomain/card/mocks/card.construction.mock';
import cardCrane from 'auto-core/react/dataDomain/card/mocks/card.crane.mock';
import cardDredge from 'auto-core/react/dataDomain/card/mocks/card.dredge.mock';
import cardLcv from 'auto-core/react/dataDomain/card/mocks/card.lcv.mock';
import cardMunicipal from 'auto-core/react/dataDomain/card/mocks/card.municipal.mock';
import cardTrailer from 'auto-core/react/dataDomain/card/mocks/card.trailer.mock';
import cardTruck from 'auto-core/react/dataDomain/card/mocks/card.truck.mock';

import type { Offer, TOfferSection } from 'auto-core/types/proto/auto/api/api_offer_model';

import techSummaryMobile from './mobile';

describe('cars', () => {
    it('должен вернуть колонки для CARS/USED', () => {
        const offer = cloneOfferWithHelpers(cardCars)
            .withSection('used');

        expect(techSummaryMobile(offer.value())).toMatchSnapshot();
    });

    it('должен вернуть колонки для CARS/NEW группа', () => {
        expect(techSummaryMobile(groupCard)).toMatchSnapshot();
    });

    it('должен вернуть колонки для CARS/NEW группа электрокаров', () => {
        const techParams: Array<TechParam> = [
            {
                id: '21605511',
                name: '320',
                nameplate: '320i xDrive',
                displacement: 1998,
                engine_type: 'ELECTRO',
                gear_type: 'ALL_WHEEL_DRIVE',
                transmission: 'AUTOMATIC',
                power: 184,
                power_kvt: 135,
                human_name: '320i xDrive 2.0 AT (184 л.с.) 4WD',
                acceleration: 7.6,
                clearance_min: 136,
                fuel_rate: 6.3,
                electric_range: 320,
            } as TechParam,
        ];
        const offer = cloneOfferWithHelpers(groupCard)
            .withGroupingInfo({ ...groupCard.groupping_info, tech_params: techParams })
            .withEngineType(Car_EngineType.ELECTRO)
            .value();

        expect(techSummaryMobile(offer)).toMatchSnapshot();
    });

    it('должен вернуть колонки для CARS/NEW объявление', () => {
        const offer = cloneOfferWithHelpers(cardCars)
            .withSection('new')
            .withComplectation({
                name: 'complectation name',
            });
        expect(techSummaryMobile(offer.value())).toMatchSnapshot();
    });

    it('должен вернуть колонки для CARS/NEW объявление в листинге группы', () => {
        const offer = cloneOfferWithHelpers(cardCars)
            .withSection('new')
            .withComplectation({
                available_options: [ '1', '2' ],
            });
        expect(techSummaryMobile(offer.value(), true)).toMatchSnapshot();
    });

    it('должен вернуть колонки для CARS/NEW объявление электрокара', () => {
        const offer = cloneOfferWithHelpers(cardCarsElectro)
            .withSection('new')
            .withComplectation({
                name: 'complectation name',
            });
        expect(techSummaryMobile(offer.value())).toMatchSnapshot();
    });

    it('должен вернуть колонки для CARS/NEW объявление электрокара без запаса хода', () => {
        const offer = cloneOfferWithHelpers(cardCarsElectro)
            .withSection('new')
            .withComplectation({
                name: 'complectation name',
            })
            .value();

        offer.vehicle_info.tech_param!.electric_range = 0;

        expect(techSummaryMobile(offer)).toMatchSnapshot();
    });

    it('должен вернуть колонки для CARS/NEW объявление электрокара в листинге группы', () => {
        const offer = cloneOfferWithHelpers(cardCarsElectro)
            .withSection('new')
            .withComplectation({
                available_options: [ '1', '2' ],
            });
        expect(techSummaryMobile(offer.value(), true)).toMatchSnapshot();
    });

    it('должен вернуть колонки для CARS/NEW объявление электрокара без запаса хода в листинге группы', () => {
        const offer = cloneOfferWithHelpers(cardCarsElectro)
            .withSection('new')
            .withComplectation({
                available_options: [ '1', '2' ],
            })
            .value();

        offer.vehicle_info.tech_param!.electric_range = 0;

        expect(techSummaryMobile(offer, true)).toMatchSnapshot();
    });
});

describe('moto', () => {
    it.each([
        [ 'ATV', 'new', cardAtv ],
        [ 'ATV', 'used', cardAtv ],
        [ 'MOTORCYCLE', 'new', cardMotorcycle ],
        [ 'MOTORCYCLE', 'used', cardMotorcycle ],
        [ 'SCOOTERS', 'new', cardScooters ],
        [ 'SCOOTERS', 'used', cardScooters ],
        [ 'SNOWMOBILE', 'new', cardSnowmobile ],
        [ 'SNOWMOBILE', 'used', cardSnowmobile ],
    ])('должен вернуть колонки для %s/%s', (category: string, section: string, offerMock: Offer) => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withSection(section as TOfferSection);

        expect(techSummaryMobile(offer.value())).toMatchSnapshot();
    });
});

describe('trucks', () => {
    it.each([
        [ 'AGRICULTURAL', 'new', cardAgricultural ],
        [ 'AGRICULTURAL', 'used', cardAgricultural ],
        [ 'ARTIC', 'new', cardArtic ],
        [ 'ARTIC', 'used', cardArtic ],
        [ 'AUTOLOADER', 'new', cardAutoloader ],
        [ 'AUTOLOADER', 'used', cardAutoloader ],
        [ 'BULLDOZERS', 'new', cardBulldozers ],
        [ 'BULLDOZERS', 'used', cardBulldozers ],
        [ 'BUS', 'new', cardBus ],
        [ 'BUS', 'used', cardBus ],
        [ 'CONSTRUCTION', 'new', cardConstruction ],
        [ 'CONSTRUCTION', 'used', cardConstruction ],
        [ 'CRANE', 'new', cardCrane ],
        [ 'CRANE', 'used', cardCrane ],
        [ 'DREDGE', 'new', cardDredge ],
        [ 'DREDGE', 'used', cardDredge ],
        [ 'LCV', 'new', cardLcv ],
        [ 'LCV', 'used', cardLcv ],
        [ 'MUNICIPAL', 'new', cardMunicipal ],
        [ 'MUNICIPAL', 'used', cardMunicipal ],
        [ 'TRAILER', 'new', cardTrailer ],
        [ 'TRAILER', 'used', cardTrailer ],
        [ 'TRUCK', 'new', cardTruck ],
        [ 'TRUCK', 'used', cardTruck ],
    ])('должен вернуть колонки для %s/%s', (category: string, section: string, offerMock: Offer) => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withSection(section as TOfferSection);

        expect(techSummaryMobile(offer.value())).toMatchSnapshot();
    });
});
