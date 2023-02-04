import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { PromoCodeModel } from 'app/server/tmpl/android/v1/cells/promocode/Model';
import type { PriceModifier_Feature } from '@vertis/schema-registry/ts-types-snake/auto/salesman/user/api_model';

let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '10.0.0',
        },
    } as unknown as Request;
    jest.spyOn(Math, 'random').mockReturnValue(0);
});

describe('promo code cell', () => {
    const id = new PromoCodeModel().identifier;

    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.useless_report_promocode = undefined;

        const result = render(report, req);
        const promoCode = result.find((node) => node.id === id);

        expect(promoCode).toBeUndefined();
    });

    it('block exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.useless_report_promocode = buildPromoCodeItem();

        const result = render(report, req);
        const promoCode = result.find((node) => node.id === id);
        const xml = promoCode ? yogaToXml([ promoCode ]) : '';

        expect(promoCode).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildPromoCodeItem(): PriceModifier_Feature {
    return {
        id: '123',
        deadline: '1595405273648',
    } as PriceModifier_Feature;
}
