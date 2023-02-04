import * as vehiclesReferenceBookKeyset from '../../../../src/translations/vehicles-reference-book';
import * as dashboardOrdersKeyset from '../../../../src/translations/dashboard-orders';
import selectors from '../../../src/constants/selectors';
import { depotNameRecord } from '../../../src/constants/depots';

context('Depots control складов.', function () {
  beforeEach(() => {
    cy.preserveCookies();
  });

  before(function () {
    cy.yandexLogin('manager');
    cy.get(selectors.content.dashboard.view);
  });

  describe('Change depot', function () {
    before(function () {
      cy.get(selectors.sidebar.companySelector.control).click();
      cy.get(selectors.sidebar.companySelector.depots.dropdown);
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-107
    describe('Mouse', function () {
      before(function () {
        cy.fixture('testData').then(({ depotNameRecord }) => {
          cy.get(selectors.sidebar.companySelector.depots.search.input).type(
            depotNameRecord.castlePeach,
            { force: true },
          );
          cy.get(selectors.sidebar.companySelector.depots.dropdownItems.firstDepot).should('exist');
          cy.get(selectors.sidebar.companySelector.depots.dropdownItems.firstDepot).click({
            force: true,
          });
        });
      });

      it('Depot address is changed', function () {
        cy.get(selectors.sidebar.companySelector.depots.currentDepotAddress)
          .invoke('text')
          .should('eq', 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2');
      });

      it('Count of depots is changes', function () {
        cy.get(selectors.content.dashboard.dayOrderNumber)
          .invoke('text')
          .should('eq', `12 ${dashboardOrdersKeyset.ru.orders.many}`);
      });
    });
  });

  describe('Scroll into depot menu select', function () {
    before(function () {
      cy.get(selectors.sidebar.companySelector.control).click();
      cy.get(selectors.sidebar.companySelector.depots.dropdown);
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-112
    it('Move to last depot', function () {
      cy.viewport(550, 750);
      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.lastDepot).scrollIntoView();
      cy.get(`${selectors.sidebar.companySelector.depots.dropdownItems.lastDepot} div`).should(
        'exist',
      );
    });

    after(function () {
      cy.get(selectors.sidebar.companySelector.control).click();
    });
  });

  // @see https://testpalm.yandex-team.ru/testcase/courier-109
  describe('Search by depots', function () {
    before(function () {
      cy.get(selectors.sidebar.companySelector.control).click();
      cy.get(selectors.sidebar.companySelector.depots.dropdown);
    });

    afterEach(function () {
      cy.get(selectors.sidebar.companySelector.depots.search.input).clear({ force: true });
    });

    it('Rus', function () {
      cy.get(selectors.sidebar.companySelector.depots.search.input).type('зам', { force: true });
      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.anyDepot).should(
        'have.length',
        1,
      );
    });

    it('Rus CAPS', function () {
      cy.get(selectors.sidebar.companySelector.depots.search.input).type('ЗАМ', { force: true });
      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.anyDepot).should(
        'have.length',
        1,
      );
    });

    it('Eng', function () {
      cy.get(selectors.sidebar.companySelector.depots.search.input).type('word', { force: true });
      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.anyDepot).should(
        'have.length',
        2,
      );
    });

    it('Eng CAPS', function () {
      cy.get(selectors.sidebar.companySelector.depots.search.input).type('WORD', { force: true });
      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.anyDepot).should(
        'have.length',
        2,
      );
    });

    it('Bracket', function () {
      cy.get(selectors.sidebar.companySelector.depots.search.input).type('(', { force: true });
      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.anyDepot).should(
        'have.length',
        2,
      );
    });

    it('Number', function () {
      cy.get(selectors.sidebar.companySelector.depots.search.input).type('5G', { force: true });
      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.anyDepot).should(
        'have.length',
        1,
      );
    });
  });
});

const isZoneVisible = (el: JQuery): boolean => {
  const visibilityLimit = 4;
  return (
    Number.parseInt(el.css('height'), 10) > visibilityLimit &&
    Number.parseInt(el.css('width'), 10) > visibilityLimit
  );
};

// @see https://testpalm.yandex-team.ru/courier/testcases/626
describe('Edit depot radius', function () {
  beforeEach(() => {
    cy.preserveCookies();
  });

  before(function () {
    cy.clearCookies();
    cy.yandexLogin('admin');
    cy.waitForElement(selectors.sidebar.menu.settingsGroup);
  });

  it('Open depots settings page from sidebar', function () {
    cy.get(selectors.sidebar.menu.settingsGroup).click();
    cy.get(selectors.sidebar.menu.depots).click();

    cy.url().should('include', 'depots-settings');
    cy.get(selectors.content.depots.list.search).should('exist');
    cy.get(selectors.content.depots.list.anyDepotItem).should('exist');
  });

  it('Open depot page from depots list', () => {
    cy.get(selectors.content.depots.list.anyDepotItem)
      .contains(depotNameRecord.additional4)
      .click();

    cy.get(selectors.content.depots.singleDepot.viewLoaded).should('exist');
    cy.get(selectors.settings.depots.inputs.radius).should('exist').and('have.value', '500');
    cy.get(selectors.settings.depots.buttons.saveDepot).should('be.disabled');
  });

  it('Sets zero in radius input on clear', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear();

    cy.get(selectors.settings.depots.inputs.radius).should(
      'have.value',
      vehiclesReferenceBookKeyset.ru.extendedForm_shift_perStopLackPenalty_placeholder,
    );
  });

  it('Zone is not visible when radius is zero', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear();

    cy.get(selectors.settings.depots.map.zone).should($el => {
      expect(isZoneVisible($el)).eq(false);
    });

    cy.get(selectors.settings.depots.map.zoom.plus).click().click().click().click().click();

    cy.get(selectors.settings.depots.map.zone).should($el => {
      expect(isZoneVisible($el)).eq(false);
    });
  });

  it('Save depot with zero radius', () => {
    cy.get(selectors.settings.depots.buttons.saveDepot).click();

    cy.url().should('include', 'depots-settings');
    cy.get(selectors.content.depots.list.anyDepotItem).should('exist');
  });

  it('Open saved depot with zero radius', () => {
    cy.get(selectors.content.depots.list.anyDepotItem)
      .contains(depotNameRecord.additional4)
      .click();

    cy.get(selectors.content.depots.singleDepot.viewLoaded).should('exist');
    cy.get(selectors.settings.depots.inputs.radius).should(
      'have.value',
      vehiclesReferenceBookKeyset.ru.extendedForm_shift_perStopLackPenalty_placeholder,
    );
    cy.get(selectors.settings.depots.map.zone).should($el => {
      expect(isZoneVisible($el)).eq(false);
    });
  });

  it('Zone is visible when radius is specified', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear();
    cy.get(selectors.settings.depots.inputs.radius).type('500');

    cy.get(selectors.settings.depots.inputs.radius).should('have.value', '500');
    cy.get(selectors.settings.depots.map.zone).should($el => {
      expect(isZoneVisible($el)).eq(true);
    });
    cy.get(selectors.settings.depots.buttons.saveDepot).should('not.be.disabled');
  });

  it('Save depot with specified radius', () => {
    cy.get(selectors.settings.depots.buttons.saveDepot).click();

    cy.url().should('include', 'depots-settings');
    cy.get(selectors.content.depots.list.anyDepotItem).should('exist');
  });

  it('Open saved depot with specified radius', () => {
    cy.get(selectors.content.depots.list.anyDepotItem)
      .contains(depotNameRecord.additional4)
      .click();

    cy.get(selectors.content.depots.singleDepot.viewLoaded).should('exist');
    cy.get(selectors.settings.depots.inputs.radius).should('have.value', '500');
    cy.get(selectors.settings.depots.map.zone).should($el => {
      expect(isZoneVisible($el)).eq(true);
    });
  });

  it('Zone is covering map when max radius specified', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear();
    cy.get(selectors.settings.depots.inputs.radius).type('99999');
    cy.get(selectors.settings.depots.map.zoom.minus).click().click().click().click().click();

    cy.get(selectors.settings.depots.inputs.radius).should('have.value', '99999');
    cy.get(selectors.settings.depots.map.zone).should($el => {
      expect(isZoneVisible($el)).eq(true);
    });
    cy.get(selectors.settings.depots.buttons.saveDepot).should('not.be.disabled');
  });

  it('Zone is not visible when dragging depot pin on map', () => {
    cy.get(selectors.settings.depots.map.zoom.plus)
      .click()
      .click()
      .click()
      .click()
      .click()
      .wait(500);

    cy.get(selectors.settings.depots.map.placemark)
      .triggerOnLayer(selectors.settings.depots.map.events, { event: 'mousedown' })
      .triggerOnLayer(selectors.settings.depots.map.events, {
        event: 'mousemove',
        deltaX: 20,
        deltaY: 0,
      });

    cy.get(selectors.settings.depots.map.zone).should('not.exist');

    cy.get(selectors.settings.depots.map.placemark).triggerOnLayer(
      selectors.settings.depots.map.events,
      { event: 'mouseup' },
    );
    cy.get(selectors.settings.depots.map.zone).should($el => {
      expect(isZoneVisible($el)).eq(true);
    });
  });

  it('Changing depot address after pin move', () => {
    cy.get(selectors.settings.depots.inputs.address)
      .invoke('val')
      .then(previousAddress => {
        cy.get(selectors.settings.depots.map.placemark)
          .triggerOnLayer(selectors.settings.depots.map.events, { event: 'mousedown' })
          .triggerOnLayer(selectors.settings.depots.map.events, {
            event: 'mousemove',
            deltaX: 10,
            deltaY: 0,
          })
          .triggerOnLayer(selectors.settings.depots.map.events, { event: 'mouseup' });

        cy.get(selectors.settings.depots.inputs.address)
          .should('not.have.value', previousAddress)
          .and('not.have.value', '');
      });
  });

  it('Save depot with changed address', () => {
    cy.get(selectors.settings.depots.buttons.saveDepot).click();

    cy.url().should('include', 'depots-settings');
    cy.get(selectors.content.depots.list.anyDepotItem).should('exist');
  });

  it('Open saved depot with changed radius', () => {
    cy.get(selectors.content.depots.list.anyDepotItem)
      .contains(depotNameRecord.additional4)
      .click();

    cy.get(selectors.content.depots.singleDepot.viewLoaded).should('exist');
    cy.get(selectors.settings.depots.inputs.radius).should('have.value', '99999');
    cy.get(selectors.settings.depots.map.zone).should('have.length.above', 9);

    cy.get(selectors.settings.depots.map.zoom.scale).then($scale => {
      const scalePosition = $scale[0].getBoundingClientRect();
      cy.get(selectors.settings.depots.map.zoom.runner).then($runner => {
        const runnerPosition = $runner[0].getBoundingClientRect();
        expect(runnerPosition.top).is.approximately(
          scalePosition.top + scalePosition.height / 2,
          20,
        );
      });
    });
  });

  after(() => {
    cy.get(selectors.settings.depots.inputs.radius).clear();
    cy.get(selectors.settings.depots.inputs.radius).type('500');
    cy.get(selectors.settings.depots.buttons.saveDepot).click();

    cy.clearCookies();
  });
});

// @see https://testpalm.yandex-team.ru/courier/testcases/628
describe('Depot radius validation', function () {
  beforeEach(() => {
    cy.preserveCookies();
  });

  before(function () {
    cy.clearCookies();
    cy.yandexLogin('admin');

    cy.waitForElement(selectors.sidebar.menu.settingsGroup).click();
    cy.get(selectors.sidebar.menu.depots).click();
    cy.get(selectors.content.depots.list.anyDepotItem)
      .contains(depotNameRecord.additional4)
      .click();
  });

  it('Not accepting text on edit page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().type('type text value - текст');

    cy.get(selectors.settings.depots.inputs.radius).should(
      'have.value',
      vehiclesReferenceBookKeyset.ru.extendedForm_shift_perStopLackPenalty_placeholder,
    );
  });

  it('Not accepting negative values on edit page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().type('-100');

    cy.get(selectors.settings.depots.inputs.radius).should('have.value', '100');
  });

  it('Not accepting float number with comma on edit page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().type('55,91');

    cy.get(selectors.settings.depots.inputs.radius).should('have.value', '5591');
  });

  it('Not accepting float number with dot on edit page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().type('55.91');

    cy.get(selectors.settings.depots.inputs.radius).should('have.value', '5591');
  });

  it('Not accepting number over limit on edit page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().type('100000');

    cy.get(selectors.settings.depots.inputs.radius).should('have.value', '10000');
  });

  it('Not accepting text from clipboard on edit page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().focus();
    cy.window().its('navigator.clipboard').invoke('writeText', 'type text value - текст');
    cy.document().invoke('execCommand', 'paste');

    cy.get(selectors.settings.depots.inputs.radius).should(
      'have.value',
      vehiclesReferenceBookKeyset.ru.extendedForm_shift_perStopLackPenalty_placeholder,
    );
  });

  it('Not accepting negative values from clipboard on edit page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().focus();
    cy.window().its('navigator.clipboard').invoke('writeText', '-100');
    cy.document().invoke('execCommand', 'paste');

    cy.get(selectors.settings.depots.inputs.radius).should(
      'have.value',
      vehiclesReferenceBookKeyset.ru.extendedForm_shift_perStopLackPenalty_placeholder,
    );
  });

  it('Not accepting float number with comma from clipboard on edit page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().focus();
    cy.window().its('navigator.clipboard').invoke('writeText', '55,91');
    cy.document().invoke('execCommand', 'paste');

    cy.get(selectors.settings.depots.inputs.radius).should(
      'have.value',
      vehiclesReferenceBookKeyset.ru.extendedForm_shift_perStopLackPenalty_placeholder,
    );
  });

  it('Not accepting float number with dot from clipboard on edit page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().focus();
    cy.window().its('navigator.clipboard').invoke('writeText', '55.91');
    cy.document().invoke('execCommand', 'paste');

    cy.get(selectors.settings.depots.inputs.radius).should(
      'have.value',
      vehiclesReferenceBookKeyset.ru.extendedForm_shift_perStopLackPenalty_placeholder,
    );
  });

  it('Not accepting number over limit from clipboard on edit page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().focus();
    cy.window().its('navigator.clipboard').invoke('writeText', '123456');
    cy.document().invoke('execCommand', 'paste');

    cy.get(selectors.settings.depots.inputs.radius).should('have.value', '1234');
  });

  it('Not accepting text on create page', () => {
    cy.get(selectors.content.depots.singleDepot.back).click();
    cy.get(selectors.content.depots.list.createDepotButton).click();

    cy.get(selectors.settings.depots.inputs.radius).clear().type('type text value - текст');

    cy.get(selectors.settings.depots.inputs.radius).should(
      'have.value',
      vehiclesReferenceBookKeyset.ru.extendedForm_shift_perStopLackPenalty_placeholder,
    );
  });

  it('Not accepting negative values on create page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().type('-100');

    cy.get(selectors.settings.depots.inputs.radius).should('have.value', '100');
  });

  it('Not accepting float number with comma on create page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().type('55,91');

    cy.get(selectors.settings.depots.inputs.radius).should('have.value', '5591');
  });

  it('Not accepting float number with dot on create page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().type('55.91');

    cy.get(selectors.settings.depots.inputs.radius).should('have.value', '5591');
  });

  it('Not accepting number over limit on create page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().type('100000');

    cy.get(selectors.settings.depots.inputs.radius).should('have.value', '10000');
  });

  it('Not accepting text from clipboard on create page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().focus();
    cy.window().its('navigator.clipboard').invoke('writeText', 'this is a test');
    cy.document().invoke('execCommand', 'paste');

    cy.get(selectors.settings.depots.inputs.radius).should(
      'have.value',
      vehiclesReferenceBookKeyset.ru.extendedForm_shift_perStopLackPenalty_placeholder,
    );
  });

  it('Not accepting negative values from clipboard on create page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().focus();
    cy.window().its('navigator.clipboard').invoke('writeText', '-100');
    cy.document().invoke('execCommand', 'paste');

    cy.get(selectors.settings.depots.inputs.radius).should(
      'have.value',
      vehiclesReferenceBookKeyset.ru.extendedForm_shift_perStopLackPenalty_placeholder,
    );
  });

  it('Not accepting float number with comma from clipboard on create page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().focus();
    cy.window().its('navigator.clipboard').invoke('writeText', '55,91');
    cy.document().invoke('execCommand', 'paste');

    cy.get(selectors.settings.depots.inputs.radius).should(
      'have.value',
      vehiclesReferenceBookKeyset.ru.extendedForm_shift_perStopLackPenalty_placeholder,
    );
  });

  it('Not accepting float number with dot from clipboard on create page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().focus();
    cy.window().its('navigator.clipboard').invoke('writeText', '55.91');
    cy.document().invoke('execCommand', 'paste');

    cy.get(selectors.settings.depots.inputs.radius).should(
      'have.value',
      vehiclesReferenceBookKeyset.ru.extendedForm_shift_perStopLackPenalty_placeholder,
    );
  });

  it('Not accepting number over limit from clipboard on create page', () => {
    cy.get(selectors.settings.depots.inputs.radius).clear().focus();
    cy.window().its('navigator.clipboard').invoke('writeText', '123456');
    cy.document().invoke('execCommand', 'paste');

    cy.get(selectors.settings.depots.inputs.radius).should('have.value', '1234');
  });

  after(() => {
    cy.clearCookies();
  });

  // @see https://testpalm.yandex-team.ru/courier/testcases/627
  describe('Edit depot radius on create page', function () {
    beforeEach(() => {
      cy.preserveCookies();
    });

    before(function () {
      cy.clearCookies();
      cy.yandexLogin('admin');
      cy.waitForElement(selectors.sidebar.menu.settingsGroup);
    });

    after(() => {
      cy.clearCookies();
    });

    it('Open depots settings page from sidebar', function () {
      cy.get(selectors.sidebar.menu.settingsGroup).click();
      cy.get(selectors.sidebar.menu.depots).click();

      cy.url().should('include', 'depots-settings');
      cy.get(selectors.content.depots.list.search).should('exist');
      cy.get(selectors.content.depots.list.anyDepotItem).should('exist');
    });

    it('Open create depot page from depots list', () => {
      cy.get(selectors.content.depots.list.createDepotButton).click();

      cy.get(selectors.content.depots.singleDepot.view).should('exist');
      cy.get(selectors.settings.depots.buttons.createDepot).should('be.disabled');
      cy.get(selectors.settings.depots.inputs.radius).should('have.value', '500');
      cy.get(selectors.settings.depots.inputs.number).should('have.value', '');
      cy.get(selectors.settings.depots.inputs.name).should('have.value', '');
      cy.get(selectors.settings.depots.inputs.address).should('have.value', '');
    });

    const randomDepotNumber = new Date().getTime().toString();
    it('Zone is not visible when radius is zero', () => {
      cy.get(selectors.settings.depots.inputs.number).type(randomDepotNumber);
      cy.get(selectors.settings.depots.inputs.name).type(`test_depot_${randomDepotNumber}`);
      cy.get(selectors.settings.depots.inputs.address).type('Москва');
      cy.get(selectors.settings.depots.geocodingSuggestOption).click();
      cy.get(selectors.settings.depots.inputs.radius).clear();
      cy.get(selectors.settings.depots.map.zoom.plus)
        .click()
        .click()
        .click()
        .click()
        .click()
        .wait(300);

      cy.get(selectors.settings.depots.inputs.radius).should(
        'have.value',
        vehiclesReferenceBookKeyset.ru.extendedForm_shift_perStopLackPenalty_placeholder,
      );
      cy.get(selectors.settings.depots.buttons.createDepot).should('not.be.disabled');

      cy.get(selectors.settings.depots.map.zone).should($el => {
        expect(isZoneVisible($el)).eq(false);
      });
    });

    it('Create depot with zero radius', () => {
      cy.get(selectors.settings.depots.buttons.createDepot).click();

      cy.get(selectors.content.depots.list.anyDepotItem).should('exist');
      cy.get(selectors.content.depots.list.anyDepotItem)
        .last()
        .should('contain.text', `test_depot_${randomDepotNumber}`);
    });

    it('Open created depot with zero radius', () => {
      cy.get(selectors.content.depots.list.anyDepotItem)
        .contains(`test_depot_${randomDepotNumber}`)
        .click();

      cy.get(selectors.settings.depots.map.zoom.plus)
        .click()
        .click()
        .click()
        .click()
        .click()
        .wait(300);

      cy.get(selectors.content.depots.singleDepot.viewLoaded).should('exist');
      cy.get(selectors.settings.depots.inputs.radius).should(
        'have.value',
        vehiclesReferenceBookKeyset.ru.extendedForm_shift_perStopLackPenalty_placeholder,
      );
      cy.get(selectors.settings.depots.map.zone).should($el => {
        expect(isZoneVisible($el)).eq(false);
      });
    });

    it('Returns to depots list on back button click', () => {
      cy.get(selectors.content.depots.singleDepot.back).click();

      cy.get(selectors.content.depots.list.anyDepotItem).should('exist');
    });

    const randomDepotMaxRadiusNumber = randomDepotNumber + +'0';
    it('Zone is covering map when max radius specified', () => {
      cy.get(selectors.content.depots.list.createDepotButton).click();

      cy.get(selectors.settings.depots.inputs.number).type(randomDepotMaxRadiusNumber);
      cy.get(selectors.settings.depots.inputs.name).type(
        `test_depot_${randomDepotMaxRadiusNumber}`,
      );
      cy.get(selectors.settings.depots.inputs.address).type('Москва');
      cy.get(selectors.settings.depots.geocodingSuggestOption).click();
      cy.get(selectors.settings.depots.inputs.radius).clear().type('99999');

      cy.get(selectors.settings.depots.map.placemark).should('exist');
      cy.get(selectors.settings.depots.map.zone).should('have.length.above', 9);
      cy.get(selectors.settings.depots.buttons.createDepot).should('not.be.disabled');
    });

    it('Create depot with max radius specified', () => {
      cy.get(selectors.settings.depots.buttons.createDepot).click();

      cy.get(selectors.content.depots.list.anyDepotItem).should('exist');
      cy.get(selectors.content.depots.list.anyDepotItem)
        .last()
        .should('contain.text', `test_depot_${randomDepotMaxRadiusNumber}`);
    });

    it('Open created depot with max radius', () => {
      cy.get(selectors.content.depots.list.anyDepotItem)
        .contains(`test_depot_${randomDepotMaxRadiusNumber}`)
        .click();

      cy.get(selectors.content.depots.singleDepot.viewLoaded).should('exist');
      cy.get(selectors.settings.depots.inputs.radius).should('have.value', '99999');
      cy.get(selectors.settings.depots.map.zone).should('have.length.above', 9);
    });

    after(() => {
      cy.get(selectors.settings.depots.buttons.deleteDepot).click();
      cy.get(selectors.modal.dialog.submit).click();

      cy.get(selectors.content.depots.list.anyDepotItem)
        .contains(`test_depot_${randomDepotNumber}`)
        .click();

      cy.get(selectors.settings.depots.buttons.deleteDepot).click();
      cy.get(selectors.modal.dialog.submit).click();
    });
  });
});

// @see https://testpalm.yandex-team.ru/courier/testcases/551
describe('Depot address input clear', function () {
  before(function () {
    cy.clearCookies();
    cy.yandexLogin('admin');

    cy.waitForElement(selectors.sidebar.menu.settingsGroup).click();
    cy.get(selectors.sidebar.menu.depots).click();
    cy.get(selectors.content.depots.list.createDepotButton).click();
  });

  it('should show suggests when typing', () => {
    cy.get(selectors.suggest.input).type('москва');

    cy.get(selectors.suggest.list).should('exist');
    cy.get(selectors.suggest.listOptions).should('have.length.above', 0);
  });

  it('should show pin on map when select suggest', () => {
    cy.get(selectors.suggest.listOptions)
      .first()
      .invoke('text')
      .then(address => {
        cy.get(selectors.suggest.listOptions).first().click();

        cy.get(selectors.suggest.clear).should('exist');
        cy.get(selectors.suggest.input).should('have.value', address);
        cy.get(selectors.settings.depots.map.placemark).should('exist');
      });
  });

  it('should clear address input after suggests', () => {
    cy.get(selectors.suggest.clear).click();

    cy.get(selectors.suggest.input).should('have.value', '');
  });

  it('should clear address input after suggest select', () => {
    cy.get(selectors.suggest.input).type('москва');
    cy.get(selectors.suggest.clear).click();

    cy.get(selectors.suggest.input).should('have.value', '');
  });
});
