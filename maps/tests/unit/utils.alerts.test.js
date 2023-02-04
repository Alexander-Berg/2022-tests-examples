'use strict';

const {generateGolovanAppAlert, generateGolovanL7Alert} = require('../../lib/utils/alerts');

const qtoolsConfig = require('./fixtures/qtools-for-alerts.json');

describe('utils/alerts', () => {
    describe('#generateGolovanAppAlert()', () => {
        qtoolsConfig.geoSpecific.monitorings.components.forEach((component) => {
            it(`should make correct generation for component ${component.environment}/${component.name}`, () => {
                const golovanAlert = generateGolovanAppAlert({
                    fullEnvName: `${qtoolsConfig.abcServiceName}_${component.environment}`,
                    component,
                    maintainers: ['robot-ololo']
                });

                expect(mockQtoolsVersion(golovanAlert)).toMatchSnapshot();
            });
        });
    });

    describe('#generateGolovanL7Alert()', () => {
        qtoolsConfig.geoSpecific.monitorings.l7.forEach((balancer) => {
            it(`should make correct generation for balancer ${balancer.id}`, () => {
                const golovanAlert = generateGolovanL7Alert({
                    balancer,
                    maintainers: ['robot-ololo']
                });

                expect(mockQtoolsVersion(golovanAlert)).toMatchSnapshot();
            });
        });
    });
});

function mockQtoolsVersion(alert) {
    return alert.replace(/<% set qtools_version = '(.*)' %>/, '<% set qtools_version = \'TBD\' %>');
}
