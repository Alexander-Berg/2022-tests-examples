import MockDate from 'mockdate';

import { builder } from './passportIssueDateValidator';

it('возвращает true для порогового года, но не наступившего ДР', () => {
    let testRunner: (passportIssueDateString: string) => boolean | void;

    MockDate.set('2021-05-11');

    builder('12.08.1976', {
        test: (params: { test: (passportIssueDateString: string) => boolean }) => testRunner = params.test,
    });

    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    expect(testRunner('14.04.2018')).toBe(true);
});

it('возвращает true для наступившего др и датой выдачи паспорта после в тот же год', () => {
    let testRunner: (passportIssueDateString: string) => boolean | void;

    MockDate.set('2021-05-11');

    builder('21.05.1993', {
        test: (params: { test: (passportIssueDateString: string) => boolean }) => testRunner = params.test,
    });

    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    expect(testRunner('01.06.2013')).toBe(true);
});

it('возвращает false для разницы даты рождения и даты выдачи паспорта меньше порога', () => {
    let testRunner: (passportIssueDateString: string) => boolean | void;

    MockDate.set('2021-05-11');

    builder('21.05.1993', {
        test: (params: { test: (passportIssueDateString: string) => boolean }) => testRunner = params.test,
    });

    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    expect(testRunner('01.06.2012')).toBe(false);
});
