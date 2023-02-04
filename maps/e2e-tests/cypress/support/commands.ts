// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add('login', (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add('drag', { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add('dismiss', { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite('visit', (originalFn, url, options) => { ... })

// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// eslint-disable-next-line import/no-unresolved
import 'cypress-file-upload';
import flattenDeep from 'lodash/flattenDeep';
import map from 'lodash/map';
import path from 'path';
import { getBaseUrl } from './utils';

const tokensFile = 'cypress/fixtures/login-tokens.json';

Cypress.Commands.add('setLoginCookies', cookies => {
  const { Session_id, yandexuid } = cookies;

  cy.setCookie(Session_id.name, Session_id.value, {
    domain: Session_id.domain,
    path: Session_id.path,
    secure: Session_id.secure,
  });
  cy.setCookie(yandexuid.name, yandexuid.value, {
    domain: yandexuid.domain,
    path: yandexuid.path,
    secure: yandexuid.secure,
  });
});

Cypress.Commands.add('readLoginCookies', (key: string, tld: SupportedTld = 'ru') => {
  return cy.task('readFileMaybe', tokensFile).then((tokens: any) => {
    if (tokens[tld] && tokens[tld][key]) {
      return tokens[tld][key];
    }
    return null;
  });
});

Cypress.Commands.add('writeLoginCookies', (key: string, tld: SupportedTld = 'ru') => {
  cy.task('readFileMaybe', tokensFile).then((tokens: any) => {
    cy.getCookie('Session_id')
      .should('not.be.null')
      .then(cookie => {
        cy.getCookie('yandexuid')
          .should('not.be.null')
          .then(yandexuid => {
            const newToken = {
              Session_id: cookie,
              yandexuid,
            };
            cy.writeFile(tokensFile, {
              ...tokens,
              [tld]: { ...tokens[tld], [key]: newToken },
            });
          });
      });
  });
});

Cypress.Commands.add(
  'manualPassportLogin',
  (loginKey: keyof AccountsT, tld: SupportedTld = 'ru') => {
    return cy.fixture('testData').then(({ accounts }) => {
      const login = accounts[loginKey];

      return cy.task('tusGetAuthData', login).then(({ account: { login, password } }: any) => {
        const passportUrl = `https://passport.yandex.${tld}/auth?mode=password&origin=courier`;

        cy.log('Need login in passport');
        cy.visit(passportUrl);

        const passportHost = `https://passport.yandex.${tld}`;

        cy.intercept({
          method: 'POST',
          url: `${passportHost}/registration-validations/auth/multi_step/start`,
        }).as('authLogin');
        cy.intercept({
          method: 'POST',
          url: `${passportHost}/registration-validations/auth/multi_step/commit_password`,
        }).as('authPassword');

        cy.url().then(url => {
          if (url.includes('list')) {
            cy.get('.AddAccountButton').click();
          }
        });

        cy.get('#passp-field-login').then($e => {
          if ($e.prop('type') != 'text') {
            cy.get('button[data-type="login"]').click();
          }
        });
        cy.get('#passp-field-login')
          .focus()
          .type(login + '{enter}')
          .should('have.value', login);
        cy.wait('@authLogin');
        cy.get('#passp-field-passwd')
          .focus()
          .type(password + '{enter}')
          .should('have.value', password);
        cy.wait('@authPassword');

        return cy.getCookie('Session_id').should('not.be.null');
      });
    });
  },
);

// -- This is a parent command --
Cypress.Commands.add('yandexLogin', (loginKey: keyof AccountsT | '', options?: LoginOptions) => {
  const tld = options?.tld || 'ru';
  const retpath = getBaseUrl(options);

  if (!loginKey) {
    cy.visit(retpath);
    return;
  }

  cy.fixture('testData').then(({ accounts }) => {
    const login = accounts[loginKey];
    const lowerLogin = login.toLowerCase();

    cy.readLoginCookies(lowerLogin, tld).then(token => {
      if (token) {
        cy.log('Login with cookie');
        cy.setLoginCookies(token);
        cy.preserveCookies(); // in some case needed of force cookie saving between tests
        cy.log(retpath);
        cy.visit(retpath);
        return;
      }

      cy.manualPassportLogin(loginKey, tld).then(() => {
        cy.writeLoginCookies(lowerLogin, tld);
        cy.wait(2000);
        // in some case needed of force cookie saving between tests
        cy.preserveCookies();
        cy.visit(retpath);
      });
    });
  });
});

Cypress.Commands.add('preserveCookies', () => {
  Cypress.Cookies.defaults({
    preserve: () => {
      return true;
    },
  });
});

Cypress.Commands.add('clearLocalforage', () => {
  indexedDB.deleteDatabase('localforage');
});

Cypress.Commands.add('forceVisit', url => {
  cy.window().then(win => {
    return win.open(url, '_self');
  });
});

Cypress.Commands.add('waitForElement', (selector: string, { timeout = 5000, ...options } = {}) => {
  return cy.get(selector, { ...options, timeout });
});

const VIDEO_CLOSE_SELECTOR = '[aria-label="help video"] .bb-ui-close-button';

Cypress.Commands.add('openAndCloseVideo', (options?: { onlyOpen?: boolean }) => {
  cy.wait(1000);
  cy.window().then(window => {
    // Property '_debug' does not exist on type Window
    // @ts-expect-error bad types
    window._debug.hints.reset(); // This opened video modal

    if (!options?.onlyOpen) {
      cy.get(VIDEO_CLOSE_SELECTOR).should('exist').click();
    }
  });
});

Cypress.Commands.add<'triggerOnLayer', 'element'>(
  'triggerOnLayer',
  { prevSubject: 'element' },
  (subject, on, options) => {
    // @ts-expect-error bad types
    cy.get(subject).then($el => {
      const rect = $el[0].getBoundingClientRect();

      cy.get(on).trigger(options.event, {
        button: 0,
        force: true,
        pageX: (options.deltaX || 0) + rect.x,
        pageY: (options.deltaY || 0) + rect.y,
        ...options,
      });

      return cy.wrap($el);
    });
  },
);

function createCustomEvent(eventName: string, options: Record<string, number>): CustomEvent {
  const event = document.createEvent('CustomEvent');
  event.initCustomEvent(eventName, true, true, null);

  // @ts-expect-error bad types
  event.clientX = options.clientX;
  // @ts-expect-error bad types
  event.clientY = options.clientY;

  return event;
}

Cypress.Commands.add('dragToElement', { prevSubject: 'element' }, (source, destination) => {
  // @ts-expect-error bad types
  cy.get(source).then($source => {
    cy.get(destination).then($target => {
      const source = $source[0];
      const target = $target[0];

      const sourceCoordinates = source.getBoundingClientRect();
      const targetCoordinates = target.getBoundingClientRect();

      source.dispatchEvent(
        createCustomEvent('dragstart', {
          clientX: sourceCoordinates.left + 1,
          clientY: sourceCoordinates.top + 1,
        }),
      );

      source.dispatchEvent(
        createCustomEvent('drag', {
          clientX: sourceCoordinates.left + 1,
          clientY: sourceCoordinates.top + 1,
        }),
      );

      target.dispatchEvent(
        createCustomEvent('dragenter', {
          clientX: targetCoordinates.left + 1,
          clientY: targetCoordinates.top + 1,
        }),
      );

      target.dispatchEvent(
        createCustomEvent('dragover', {
          clientX: targetCoordinates.left + 1,
          clientY: targetCoordinates.top + 1,
        }),
      );

      target.dispatchEvent(
        createCustomEvent('drop', {
          clientX: targetCoordinates.left + 1,
          clientY: targetCoordinates.top + 1,
        }),
      );

      source.dispatchEvent(
        createCustomEvent('dragend', {
          clientX: targetCoordinates.left + 1,
          clientY: targetCoordinates.top + 1,
        }),
      );

      return cy.wrap($source);
    });
  });
});

Cypress.Commands.add('deleteDownloadsFolder', () => {
  const downloadsFolder = Cypress.config('downloadsFolder');
  cy.task('deleteFolder', downloadsFolder);
});

Cypress.Commands.add<'validateRowInExcelFile', 'element'>(
  'validateRowInExcelFile',
  { prevSubject: true },
  (subject, filename: string, rowIndex: number) => {
    const downloadsFolder = Cypress.config('downloadsFolder');
    const downloadedFilename = path.join(downloadsFolder, filename);
    cy.task('readExcelFile', downloadedFilename).then((list: any) => {
      const rowToString = flattenDeep(list[rowIndex]).toString();
      expect(rowToString, 'header line').to.equal(subject.toString());
    });
  },
);

Cypress.Commands.add<'validateColumnInExcelFile', 'element'>(
  'validateColumnInExcelFile',
  { prevSubject: true },
  (subject, filename: string, columnIndex: number) => {
    const downloadsFolder = Cypress.config('downloadsFolder');
    const downloadedFilename = path.join(downloadsFolder, filename);
    cy.task('readExcelFile', downloadedFilename).then((list: any) => {
      const string = flattenDeep(map(list, row => row[columnIndex])).toString();
      expect(string).to.equal(subject.toString());
    });
  },
);

Cypress.Commands.add('makeData', dataKey => {
  return cy.task('makeData', dataKey);
});

Cypress.Commands.add('removeData', dataKey => {
  return cy.task('removeData', dataKey);
});
