import selectors from '../../../../src/constants/selectors';

context('Change delivery settings', () => {
  const companySelector = selectors.settings.company;

  const returnToDefaultState = (): void => {
    cy.get(companySelector.inputs.radiusOfAutoDetection)
      .click()
      .find('input')
      .clear()
      .type(RADIUS_OF_AUTO_DETECTION.DEFAULT);

    cy.get(companySelector.inputs.serviceDurationCoefficient)
      .click()
      .find('input')
      .clear()
      .type(SERVICE_DURATION_COEFFICIENT.DEFAULT);

    cy.get(companySelector.submit.common).click();
    cy.get(companySelector.inputs.automaticDeliveryDetectionCheckbox).click();
    cy.get(companySelector.submit.common).click();
  };

  const COMPANY_PATHNAME_REGEX = /all\/settings$/;

  const RADIUS_OF_AUTO_DETECTION = {
    NEW: '300',
    DEFAULT: '400',
  };

  const SERVICE_DURATION_COEFFICIENT = {
    NEW: '0.5',
    DEFAULT: '0.6',
  };

  before(() => {
    cy.yandexLogin('admin');
  });

  it('should open company settings', () => {
    cy.get(selectors.sidebar.menu.settingsGroup).click();
    cy.get(selectors.sidebar.menu.company).click();
    cy.location('pathname').should('match', COMPANY_PATHNAME_REGEX);
  });

  it('should display additional settings', () => {
    cy.get(companySelector.inputs.automaticDeliveryDetectionCheckbox).click();
    cy.get(companySelector.inputs.radiusOfAutoDetection).should('exist');
    cy.get(companySelector.inputs.serviceDurationCoefficient).should('exist');
  });

  it('should save button become active', () => {
    cy.get(companySelector.inputs.radiusOfAutoDetection)
      .click()
      .focused()
      .clear()
      .type(RADIUS_OF_AUTO_DETECTION.NEW);

    cy.get(companySelector.inputs.serviceDurationCoefficient)
      .click()
      .focused()
      .clear()
      .type(SERVICE_DURATION_COEFFICIENT.NEW);

    cy.get(companySelector.submit.common).should('be.enabled');
  });

  it('should display chip', () => {
    cy.get(companySelector.submit.common).click();
    cy.get(companySelector.submit.common).should('be.disabled');
    cy.get(companySelector.chips.common).should('exist');
  });

  it('should have saved values after reload', () => {
    cy.reload();

    cy.get(companySelector.inputs.radiusOfAutoDetection)
      .find('input')
      .should('have.value', RADIUS_OF_AUTO_DETECTION.NEW);

    cy.get(companySelector.inputs.serviceDurationCoefficient)
      .find('input')
      .should('have.value', SERVICE_DURATION_COEFFICIENT.NEW);

    cy.get(companySelector.submit.common).should('be.disabled');
  });

  after(() => {
    returnToDefaultState();
  });
});
