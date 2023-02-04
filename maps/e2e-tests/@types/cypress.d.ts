// in cypress/support/index.d.ts
// load type definitions that come with Cypress module
/// <reference types="cypress" />

type CompanyDataT = {
  companyId: number;
  date: string;
  companyName: string;
  couriers: CourierDataT[];
};

type CourierDataT = {
  id: number;
  number: string;
  name: string;
};

type CouriersNameT = {
  courierForRemove: string;
  courierForSearch: string;
  gumba: string;
  kypa: string;
  spaini: string;
  red: string;
  pink: string;
  green: string;
  lightGreen: string;
  orange: string;
  сarrot: string;
  blue: string;
  lightBlue: string;
  brown: string;
  vinous: string;
  courierWithSingleRoute: string;
  john: string;
};

type CouriersNumberT = Record<keyof CouriersNameT, string>;

type AccountsT = {
  adminAddRoute: string;
  managerAddRoute: string;

  superuser: string;
  admin: string;
  adminMulti: string;
  manager: string;
  managerEmpty: string;
  managerMulti: string;
  dispatcher: string;
  temp: string;
  companyAadmin: string;
  companyBadmin: string;
  companyBmanager: string;
  companyVadmin: string;
  companyVmanager: string;
  companyGadmin: string;
  companyGmanager: string;
  companyDadmin: string;
  companyDmanager: string;
  companyEadmin: string;
  companyEmanager: string;
  mvrpManager: string;
  mvrpViewManager: string;
  refBookManager: string;

  testCompanyManager0: string;
  testCompanyManager1: string;
  testCompanyManager2: string;
  testCompanyManager3: string;
  testCompanyManager4: string;
  testCompanyManager5: string;
  testCompanyManager6: string;
  testCompanyManager7: string;
  testCompanyManager8: string;

  exportRoutesAdmin: string;
  exportRoutesManager: string;

  appRoleAdmin: string;
  appRoleManager: string;
};

type RoutesNumberT = {
  MONTH_AGO: string;
  COURIER_REMOVE: string;
  BEFORE_YESTERDAY: string;
  YESTERDAY: string;
  TODAY: string;
  TOMORROW: string;
  AFTER_TOMORROW: string;
  BIG_ORDER_NUMBER: string;
  DIFFERENT_STATUSES: string;
  TWO_ADDITIONAL_ORDERS_TODAY: string;
  TWO_ADDITIONAL_ORDERS_TOMORROW: string;
  LOT_OF_ORDERS: string;
};

type SharedRoutesNumberT = {
  AToB: string;
  BToV: string;
};

type VehicleNameT = {
  atlantis: string;
  apollo: string;
  apollo11: string;
  challenger: string;
  endeavour: string;
  endurance: string;
  spaceShuttle: string;
  spaceTravel: string;
  testPhone1: string;
  testPhone2: string;
  testNumbers1: string;
  testNumbers2: string;
  testNumbers3: string;
  testSymbol1: string;
  testSymbol2: string;
};

type VehicleNumberT = {
  rus1: string;
  rus2: string;
  rus3: string;
  rus4: string;
  rus5: string;
  rus6: string;

  es1: string;
  es2: string;
  es3: string;
  es4: string;

  tr1: string;
  tr2: string;
  tr3: string;
  tr4: string;

  dashSign: string;
};

type ZonesNameT = {
  constellations1: string;
  constellations2: string;
  constellations3: string;
  testDigit1: string;
  testDigit2: string;
  bracket1: string;
  bracket2: string;
  bracket3: string;
  testName1: string;
  testName2: string;
};

type ZonesColorT = {
  brass: string;
  green: string;
  orange: string;
  lightBlue: string;
  pink: string;
  darkGreen: string;
  magenta: string;
  yellow: string;
  olive: string;
  blue: string;
};

type DepotNameRecordT = {
  castlePeach: string;
  yoshisHouse: string;
  toadsTent: string;
  englishPub: string;
  fourwordsallcapslock: string;
  ONEWORDCAPSLOCK: string;
  rocketjump5G: string;
  some: string;
  some2: string;
  additional1: string;
  additional2: string;
  additional3: string;
  additional4: string;

  compAVisible: string;
  compANotVisible: string;
  compBVisible: string;
  compBNotVisible: string;
  compVVisible: string;
  compVNotVisible: string;
  compGVisible: string;
  compGNotVisible: string;
  compDVisible: string;
  compDNotVisible: string;
  compEVisible: string;
  compENotVisible: string;
  depotLotOfOrders: string;
};

type TestDataT = {
  accounts: AccountsT;
  courierNameRecord: CouriersNameT;
  courierNumberRecord: CouriersNumberT;

  routeNumberRecord: RoutesNumberT;
  sharedRouteNumberRecord: SharedRoutesNumberT;

  depotNameRecord: DepotNameRecordT;
};

type CompaniesDataT = Record<string, CompanyDataT>;

type TaskHashesT = {
  demo_example: number;
  simple_example_ru: number;
  extended_example_ru: number;
  simple_example_en: number;
  extended_example_en: number;
};

type SupportedTld = 'ru' | 'com.tr' | 'com';

type LoginOptions = {
  link?: string;
  tld?: SupportedTld;
  basePath?: string;
};

type UrlOptions = LoginOptions;

type YCookie = {
  name: string;
  value: string;
  domain: string;
  url: string;
  path: string;
  secure: boolean;
};

type GenerateDataTypes = 'add-route';

declare namespace Cypress {
  interface Window {
    map: Map;
  }
  interface Chainable {
    manualPassportLogin(user: keyof AccountsT, tld?: SupportedTld): Chainable<Interception>;
    yandexLogin(user: keyof AccountsT | '', options?: LoginOptions): Chainable<Element>;
    makeData: (dataKey: GenerateDataTypes) => Chainable<void>;
    removeData: (dataKey: GenerateDataTypes) => Chainable<void>;
    setLoginCookies(cookies: { Session_id: YCookie; yandexuid: YCookie }): Chainable<Element>;
    readLoginCookies(
      key: string,
      tld?: SupportedTld,
    ): Chainable<{ Session_id: YCookie; yandexuid: YCookie } | null>;
    writeLoginCookies(key: string, tld?: SupportedTld): Chainable<Element>;
    readFileMaybe(): Chainable<Element>;
    tusGetAuthData(): Chainable<{ account: { login: string; password: string } }>;
    forceVisit(url: string): Chainable<Element>;
    waitForElement(
      selector: string,
      options?: Partial<Loggable & Timeoutable & Withinable & Shadow>,
    ): Chainable<JQuery<HTMLElement>>;
    openAndCloseVideo(options?: { onlyOpen?: boolean }): void;
    triggerOnLayer<K extends keyof DocumentEventMap>(
      on: string,
      options: { event: K; deltaX?: number; deltaY?: number } & Partial<
        TriggerOptions & ObjectLike & DocumentEventMap[K]
      >,
    ): Chainable<Element>;
    dragToElement(destination: string): Chainable<Element>;

    preserveCookies(): Chainable<Element>;
    clearLocalforage(): Chainable<Element>;

    deleteDownloadsFolder(): Chainable<Element>;
    validateRowInExcelFile(filename: string, rowIndex: number): Chainable<Element>;
    validateColumnInExcelFile(filename: string, columnIndex: number): Chainable<Element>;

    fixture(path: 'testData'): Chainable<TestDataT>;
    fixture(path: 'company-data'): Chainable<CompaniesDataT>;
    fixture(path: 'example-task-hashes'): Chainable<TaskHashesT>;
    fixture(path: 'parent-example-task-hashes'): Chainable<TaskHashesT>;
  }
}
