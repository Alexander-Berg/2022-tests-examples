import { convertTimeDuration, convertTimestamp, convertTimeFromTo } from 'app/server/tmpl/common/models/converters';
import MockDate from 'mockdate';

beforeEach(() => {
    MockDate.set('2021-08-26');
});

afterEach(() => {
    MockDate.reset();
});

describe('convertTimeFromTo', () => {
    it.each<[string, string, string | undefined]>([
        [ '1435741200000', '1507798800000', '1 июля 2015 — 12 октября 2017' ],
        [ '1436864400000', '1507798800000', '14 июля 2015 — 12 октября 2017' ],
        [ '1435741200000', '1530446400000', '1 июля 2015 — 1 июля 2018' ],
        [ '1378512000000', '', '7 сентября 2013 — по настоящее время' ],
        [ '1381104000000', '', '7 октября 2013 — по настоящее время' ],
        [ '1498899600000', '1507798800000', '1 июля 2017 — 12 октября 2017' ],
        [ '1506848400000', '1507798800000', '1 октября 2017 — 12 октября 2017' ],
        [ '1507712400000', '1507798800000', '11 октября 2017 — 12 октября 2017' ],
        [ '1507798800000', '1507798800000', '12 октября 2017' ],
    ])('from=%s, to=%s => %s', (timeFrom, timeTo, result) => {
        expect(convertTimeFromTo(timeFrom, timeTo)).toEqual(result);
    });
});

describe('convertTimestamp', () => {
    it.each<[string, string]>([
        [ '1435741200000', '1 июля 2015' ],
        [ '1507798800000', '12 октября 2017' ],
        [ '', '26 августа 2021' ],
    ])('from=%s, to=%s => %s', (time, result) => {
        // придется делать now() на лету, чтобы сработал mockdate,
        time = time || String(Date.now());
        expect(convertTimestamp(time)).toEqual(result);
    });
});

describe('convertTimeDuration', () => {
    it.each<[string, string, string | undefined]>([
        [ '1506848400000', '1507798800000', '11\xA0дней' ],
        [ '1507712400000', '1507798800000', '1\xA0день' ],
        [ '1507798800000', '1507798800000', '1\xA0день' ],
        [ '1435741200000', '1439499600000', '1.5\xA0месяца' ],
        [ '1435741200000', '1441065800000', '2\xA0месяца' ],
        [ '1498899600000', '1507798800000', '3.5\xA0месяца' ],
        [ '1435741200000', '1507798800000', '2\xA0года 4\xA0месяца' ],
        [ '1378497600000', '1629907854682', '8\xA0лет' ],
        [ '1435741200000', '1530446400000', '3\xA0года' ],
        [ '1378497600000', '1410120000000', '1\xA0год 1\xA0месяц' ],
        [ '1378497600000', '1409947200000', '1\xA0год' ],
    ])('from=%s, to=%s => %s', (timeFrom, timeTo, result) => {
        expect(convertTimeDuration(timeFrom, timeTo)).toEqual(result);
    });
});
