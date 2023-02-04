const { elements } = require('./elements');

const ignoreElements = [
    elements.personExport,
    elements.personName,
    '.yb-person header',
    '.yb-person-detail__updated',
    '.person-title',
    '.person-detail__change-info',
    '.person__oebs-status',
    'form[action="reexport-object.xml"]',
    '[data-detail-id=legalSample] div:nth-child(2)',
    '[data-detail-id=envelopeAddress] div:nth-child(2)'
];

module.exports.assertViewOpts = {
    ignoreElements,
    hideElements: [elements.updated, '.yb-change-person__mandatory-fields-warning'],
    tolerance: 8,
    antialiasingTolerance: 10
};
