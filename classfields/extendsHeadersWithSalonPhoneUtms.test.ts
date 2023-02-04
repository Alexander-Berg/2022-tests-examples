import createHttpReq from 'autoru-frontend/mocks/createHttpReq';

import type { THttpRequest } from 'auto-core/http';

import extendsHeadersWithSalonPhoneUtms from './extendsHeadersWithSalonPhoneUtms';

let req: THttpRequest;
beforeEach(() => {
    req = createHttpReq();
});

it('должен распарсить куку salon_phone_utms и добавить поля в заголовки', () => {
    req.cookies.salon_phone_utms = decodeURIComponent(
        // eslint-disable-next-line max-len
        'utm_medium%3Ddirect.model%26utm_source%3Dyandex_direct%26utm_campaign%3Dhand_allweb_used_model_rsya_moskva-msk_chastniki_65863359%26utm_content%3Dcid%253A65863359%257Cgid%253A4700343932%257Caid%253A11188426639%257Cph%253A34150932804%257Cpt%253Anone%257Cpn%253A0%257Csrc%253Ayandex.ru%257Cst%253Acontext%257Ccgcid%253A0',
    );

    const prevHeaders = { 'x-foo': 'bar' };
    const nextHeaders = extendsHeadersWithSalonPhoneUtms({
        context: { req },
        headers: prevHeaders,
    });

    expect(prevHeaders === nextHeaders).toBe(false);
    expect(nextHeaders).toEqual({
        'x-foo': 'bar',
        'x-utm-source': 'yandex_direct',
        'x-utm-campaign': 'hand_allweb_used_model_rsya_moskva-msk_chastniki_65863359',
        // eslint-disable-next-line max-len
        'x-utm-content': 'cid%3A65863359%7Cgid%3A4700343932%7Caid%3A11188426639%7Cph%3A34150932804%7Cpt%3Anone%7Cpn%3A0%7Csrc%3Ayandex.ru%7Cst%3Acontext%7Ccgcid%3A0',
        'x-utm-medium': 'direct.model',
    });
});

it('не должен ничего делать и вернуть заголовки, если нет куки salon_phone_utms', () => {
    const prevHeaders = { 'x-foo': 'bar' };
    const nextHeaders = extendsHeadersWithSalonPhoneUtms({
        context: { req },
        headers: prevHeaders,
    });

    expect(prevHeaders === nextHeaders).toBe(true);
    expect(nextHeaders).toEqual({
        'x-foo': 'bar',
    });
});
