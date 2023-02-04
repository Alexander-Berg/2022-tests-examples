import selectors from 'constants/selectors';
import forEach from 'lodash/forEach';

// https://testpalm.yandex-team.ru/testcase/courier-757

Cypress.on('uncaught:exception', () => {
  return false;
});

const keys = [
  {
    key: 'vehicle_id',
    title: 'ID',
    taskId: '6c27337-9be9aa9f-10ea9fff-7ff770f4',
    data: [
      '1the bestZumazumbaMahir ÖzoğulАльфаапельсинБрод',
      '(129) Марко Поло(29)№2@arkein*Valid*Важное/2132314&^%$#@',
      'АльфаапельсинБродЁлковоМетёлкинаМетизыСветсок',
    ],
  },
  {
    key: 'return_to_depot',
    title: 'Возврат на склад',
    taskId: '55040f06-fb09ba33-4c9db75c-3d4772a7',
    data: ['ДаДаДаДаДаДаДаДа', 'НетНетНетНетНетНетНетНет', 'ДаДаДаДаДаДаДаДа'],
  },
  {
    taskId: '55040f06-fb09ba33-4c9db75c-3d4772a7',
    key: 'total_transit_duration_s',
    title: 'Общее время движения',
    data: [
      '01:1201:1802:0301:4200:5700:5500:3703:27',
      '00:1900:2600:3000:3000:3000:3000:3000:30',
      '1.07:0420:3309:5005:4705:4705:1705:1705:14',
    ],
  },
  {
    taskId: '55040f06-fb09ba33-4c9db75c-3d4772a7',
    key: 'orders_count',
    title: 'Число заказов',
    data: ['101846117', '11111122', '221116421914131111'],
  },
];

context('Routing Metrics Sorting', () => {
  forEach(keys, ({ key, data, title, taskId }) => {
    const colsSelector = `.metrics__route-value_col_${key}`;
    const headerSelector = `.metrics__cell_header[data-test-anchor="${key}"]`;
    context(`Sorting ${key}`, () => {
      beforeEach(() => {
        cy.openTaskById(taskId);
        cy.get(selectors.routesTimeline.tabSwitcher).click({ force: true });
      });
      it(`${key}`, () => {
        cy.get(selectors.metrics.table)
          .find(colsSelector)
          .invoke('text')
          .then(text => expect(text.trim()).equal(data[0]));
      });
      it(`${key} up`, () => {
        cy.get(selectors.metrics.table).find(headerSelector).click({ force: true });

        cy.get(selectors.metrics.table)
          .find(headerSelector)
          .invoke('text')
          .then(text => expect(text.trim()).to.include('▲'));

        cy.get(selectors.metrics.table)
          .find(colsSelector)
          .invoke('text')
          .then(text => {
            expect(text.trim()).equal(data[1]);
          });
      });

      it(`${key} down`, () => {
        cy.get(selectors.metrics.table).find(headerSelector).click({ force: true });

        cy.get(selectors.metrics.table).find(headerSelector).click({ force: true });

        cy.get(selectors.metrics.table)
          .find(headerSelector)
          .invoke('text')
          .then(text => expect(text.trim()).to.include('▼'));

        cy.get(selectors.metrics.table)
          .find(colsSelector)
          .invoke('text')
          .then(text => {
            expect(text.trim()).equal(data[2]);
          });
      });
      it(`${key} Return`, () => {
        cy.get(selectors.metrics.table).find(headerSelector).click({ force: true });

        cy.get(selectors.metrics.table).find(headerSelector).click({ force: true });

        cy.get(selectors.metrics.table).find(headerSelector).click({ force: true });

        cy.get(selectors.metrics.table)
          .find(headerSelector)
          .invoke('text')
          .then(text => expect(text.trim()).equal(title));

        cy.get(selectors.metrics.table)
          .find(colsSelector)
          .invoke('text')
          .then(text => {
            expect(text.trim()).equal(data[0]);
          });
      });
    });
  });

  context(`Saving sort parameters when switching`, () => {
    const [data] = keys;
    const headerSelector = `.metrics__cell_header[data-test-anchor="vehicle_id"]`;

    before(() => {
      cy.openTaskById(data.taskId);
    });
    it(`When selecting "asc"`, () => {
      cy.get(selectors.routesTimeline.tabSwitcher).click({ force: true });
      cy.get(selectors.metrics.table).find(headerSelector).click({ force: true });
      cy.get(selectors.metrics.tabSwitcher).click({ force: true });
      cy.get(selectors.routesTimeline.tabSwitcher).click({ force: true });
      cy.get(selectors.metrics.table)
        .find(headerSelector)
        .invoke('text')
        .then(text => expect(text.trim()).to.include('▲'));
    });
    it(`When selecting "desc"`, () => {
      cy.get(selectors.metrics.table).find(headerSelector).click({ force: true });
      cy.get(selectors.metrics.tabSwitcher).click({ force: true });
      cy.get(selectors.routesTimeline.tabSwitcher).click({ force: true });
      cy.get(selectors.metrics.table)
        .find(headerSelector)
        .invoke('text')
        .then(text => expect(text.trim()).to.include('▼'));
    });
  });
});
