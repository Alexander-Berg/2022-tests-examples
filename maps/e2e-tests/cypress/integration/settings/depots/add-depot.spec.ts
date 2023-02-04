import selectors from '../../../../src/constants/selectors';
import forEach from 'lodash/forEach';

type TestFiledsT = Extract<
  keyof typeof selectors.settings.depots.inputs,
  'name' | 'radius' | 'number'
>;

// @see https://testpalm.yandex-team.ru/courier/testcases/264
context('Add new depot', () => {
  const depotsSelector = selectors.settings.depots;

  const DEPOT_PATHNAME = 'all/depots-settings';

  const DEPOT_PATHNAME_REGEX = new RegExp(DEPOT_PATHNAME);
  const DEPOT_NEW_PATHNAME_REGEX = new RegExp(`${DEPOT_PATHNAME}/new`);
  const DEPOT_EDIT_PATHNAME_REGEX = new RegExp(`${DEPOT_PATHNAME}/\\d+`);

  const TEST_NUMBER = 'testDepotNumber';
  const TEST_NAME = 'testDepotName';
  const TEST_RADIUS = '500';
  const TEST_ADDRESS = 'улица Грина, 14, Москва, Россия';
  const TEST_TIMEZONE = 'America/Costa_Rica';

  const INPUT_VALUES: Record<TestFiledsT, string> = {
    number: TEST_NUMBER,
    name: TEST_NAME,
    radius: TEST_RADIUS,
  };

  const inputSelectors = depotsSelector.inputs;
  const inputKeys: Array<TestFiledsT> = ['name', 'number', 'radius'];

  const selectSelectors = depotsSelector.selects;
  const checkboxSelectors = depotsSelector.checkbox;
  const buttonSelectors = depotsSelector.buttons;

  const checkValues = (): void => {
    forEach(inputKeys, inputKey =>
      cy.get(depotsSelector.inputs[inputKey]).should('have.value', INPUT_VALUES[inputKey]),
    );
    cy.get(selectSelectors.address).should('contain.text', TEST_ADDRESS);
    cy.get(selectSelectors.timeZone).should('contain.text', TEST_TIMEZONE);
    cy.get(checkboxSelectors.allowEditRoute).should('not.be.checked');
  };

  before(() => {
    cy.yandexLogin('admin');
  });

  it('should open depots settings', () => {
    cy.get(selectors.sidebar.menu.settingsGroup).click();
    cy.get(selectors.sidebar.menu.depots).click();
    cy.url().should('match', DEPOT_PATHNAME_REGEX);
  });

  it('should open new depot page', () => {
    cy.get(depotsSelector.buttons.addDepot).click();
    cy.url().should('match', DEPOT_NEW_PATHNAME_REGEX);

    forEach(inputSelectors, inputSelector => cy.get(inputSelector).should('exist'));

    forEach(selectSelectors, selectSelector => cy.get(selectSelector).should('exist'));

    forEach(checkboxSelectors, checkboxSelector => cy.get(checkboxSelector).should('exist'));

    cy.get(depotsSelector.map.container).should('exist');

    cy.get(buttonSelectors.createDepot).should('be.disabled');
  });

  it('should show pin on map after clicking suggest option', () => {
    forEach(inputKeys, inputKey =>
      cy.get(inputSelectors[inputKey]).clear().type(INPUT_VALUES[inputKey]),
    );

    cy.get(selectSelectors.address)
      .find('input')
      .first()
      .clear()
      .type(TEST_ADDRESS)
      .get(selectSelectors.address)
      .find(depotsSelector.geocodingSuggestOption)
      .first()
      .click();

    cy.get(selectors.settings.depots.map.placemark).should('exist');
  });

  it('should change address after pin move', () => {
    cy.get(selectors.settings.depots.map.placemark)
      .triggerOnLayer(selectors.settings.depots.map.events, { event: 'mousedown' })
      .triggerOnLayer(selectors.settings.depots.map.events, {
        event: 'mousemove',
        deltaY: 20,
      })
      .triggerOnLayer(selectors.settings.depots.map.events, { event: 'mouseup' });

    cy.get(selectors.settings.depots.inputs.address).should(
      'have.value',
      '1-я Горловская улица, 4с6, Москва, Россия',
    );
  });

  it('should change address after pin move outside map', () => {
    cy.get(selectors.settings.depots.map.zoom.plus).click().wait(500);

    cy.get(selectors.settings.depots.inputs.address)
      .invoke('val')
      .then(previousAddress => {
        cy.get(selectors.settings.depots.map.ground).then($el => {
          const mapGroundInitialTransform = $el.css('transform');
          cy.get(selectors.settings.depots.map.placemark)
            .triggerOnLayer(selectors.settings.depots.map.events, { event: 'mousedown' })
            .triggerOnLayer(selectors.settings.depots.map.events, {
              event: 'mousemove',
              deltaY: -500,
            })
            .wait(500)
            .then(() => {
              expect($el.css('transform')).not.eq(mapGroundInitialTransform);
            })
            .triggerOnLayer(selectors.settings.depots.map.events, {
              event: 'mousemove',
              deltaY: 500,
            });

          cy.get(selectors.settings.depots.map.placemark).triggerOnLayer(
            selectors.settings.depots.map.events,
            { event: 'mouseup' },
          );

          cy.get(selectors.settings.depots.inputs.address)
            .should('not.have.value', previousAddress)
            .and('not.have.value', '');
        });
      });
  });

  it('should create new depot', () => {
    cy.get(selectSelectors.address)
      .find('input')
      .first()
      .clear()
      .type(TEST_ADDRESS)
      .get(selectSelectors.address)
      .find(depotsSelector.geocodingSuggestOption)
      .first()
      .click()
      .get(selectSelectors.timeZone)
      .click()
      .find('input')
      .type(TEST_TIMEZONE)
      .get(depotsSelector.dropdownOption)
      .first()
      .click()
      .get(depotsSelector.buttons.createDepot)
      .click();
  });

  it('should add new depot to list', () => {
    cy.url().should('match', DEPOT_PATHNAME_REGEX);

    cy.get(depotsSelector.depotOption)
      .last()
      .should('contain', TEST_NAME)
      .and('contain', TEST_ADDRESS);
  });

  it('should open edit depot page', () => {
    cy.get(depotsSelector.depotOption).last().click();
    cy.url().should('match', DEPOT_EDIT_PATHNAME_REGEX);
    cy.get(buttonSelectors.deleteDepot).should('exist');
    cy.get(buttonSelectors.saveDepot).should('exist').and('be.disabled');

    checkValues();
  });

  it('should save values after reload', () => {
    cy.reload();
    cy.get(buttonSelectors.deleteDepot).should('exist');
    cy.get(buttonSelectors.saveDepot).should('exist').and('be.disabled');

    checkValues();
  });

  after(() => {
    cy.get(buttonSelectors.deleteDepot).click().get(selectors.modal.dialog.submit).click();
  });
});
