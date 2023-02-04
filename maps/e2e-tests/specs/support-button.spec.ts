import selectors from 'constants/selectors';

Cypress.on('uncaught:exception', () => {
  return false;
});

context('Support Button', () => {
  context('Check Support Button', () => {
    it('Be visible', () => {
      cy.openTaskById('30ce2c3d-2fb2f0df-79680613-5c495d25');

      cy.get(selectors.supportButton).should('exist');
    });

    it('Correct links', () => {
      cy.openTaskById('30ce2c3d-2fb2f0df-79680613-5c495d25');

      cy.window().then(win => {
        const TLD = win.location.hostname.split('.').slice(-1)[0];

        const SUPPORT_LINK_RU = 'https://forms.yandex.ru/surveys/10012082/';
        const SUPPORT_LINK_COM =
          'https://forms.yandex.com/surveys/10033440.4f70b2e78e8c8029db4e8aaea5a78e8e4b35cbeb/';

        if (TLD === 'localhost' || TLD === 'ru') {
          cy.get(selectors.supportButton).should('have.attr', 'data-href', SUPPORT_LINK_RU);
        } else {
          cy.get(selectors.supportButton).should('have.attr', 'data-href', SUPPORT_LINK_COM);
        }
      });
    });
  });

  context('Check Support Button [No Task]', () => {
    it('Be visible', () => {
      cy.openEmptyTask();

      cy.get(selectors.supportButton).should('exist');
    });

    it('Correct links', () => {
      cy.openEmptyTask();

      cy.window().then(win => {
        const TLD = win.location.hostname.split('.').slice(-1)[0];

        const SUPPORT_LINK_RU = 'https://forms.yandex.ru/surveys/10012082/';
        const SUPPORT_LINK_COM =
          'https://forms.yandex.com/surveys/10033440.4f70b2e78e8c8029db4e8aaea5a78e8e4b35cbeb/';

        if (TLD === 'localhost' || TLD === 'ru') {
          cy.get(selectors.supportButton).should('have.attr', 'data-href', SUPPORT_LINK_RU);
        } else {
          cy.get(selectors.supportButton).should('have.attr', 'data-href', SUPPORT_LINK_COM);
        }
      });
    });
  });
});
