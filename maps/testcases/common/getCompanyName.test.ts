import { BrowserEnum } from './constants';
import {
  checkIsTestCompanyName,
  getCompanyName,
  getDateCreateCompanyFromName,
  checkIsOldCompany,
} from './getCompanyName';

const getDateFormat = (date: Date): string => {
  const day = date.getDate();
  const month = date.getMonth() + 1;
  const year = date.getFullYear();

  return `${month}.${day}.${year}`;
};

describe('getCompanyName', () => {
  it('checkIsTestCompanyName bad', () => {
    expect(checkIsTestCompanyName('hello world')).toBeFalsy();
  });

  it('checkIsTestCompanyName good', () => {
    const companyName = getCompanyName(111, BrowserEnum.chrome);

    expect(checkIsTestCompanyName(companyName)).toBeTruthy();
  });

  it('getDateCreateCompanyFromName', () => {
    expect(getDateCreateCompanyFromName('Y-Test 12234 edge 11 22.11.2021')).toBe('22.11.2021');
  });

  it('checkIsOldCompany', () => {
    const ONE_DAY = 24 * 60 * 60 * 1000;

    const currentDay = new Date();
    const day1 = new Date(currentDay.getTime() - ONE_DAY);
    const day2 = new Date(currentDay.getTime() - 2 * ONE_DAY);
    const day3 = new Date(currentDay.getTime() - 3 * ONE_DAY);
    const day4 = new Date(currentDay.getTime() - 4 * ONE_DAY);

    expect(checkIsOldCompany(`Y-Test 123 edge ${getDateFormat(currentDay)}`)).toBeFalsy();
    expect(checkIsOldCompany(`Y-Test 321 edge ${getDateFormat(day1)}`)).toBeFalsy();
    expect(checkIsOldCompany(`Y-Test 1234 edge ${getDateFormat(day2)}`)).toBeFalsy();
    expect(checkIsOldCompany(`Y-Test 1 edge ${getDateFormat(day3)}`)).toBeTruthy();
    expect(checkIsOldCompany(`Y-Test 2412 edge ${getDateFormat(day4)}`)).toBeTruthy();
  });
});
