const { elements } = require('./elements');

module.exports.assertViewOpts = {
    ignoreElements: [elements.personName, elements.personExport],
    hideElements: [elements.updated],
    tolerance: 8,
    antialiasingTolerance: 10
};

module.exports.assertViewOptsExport = {
    ignoreElements: [elements.personId, elements.reexportBtn],
    hideElements: [elements.exportedDate, elements.updated],
    tolerance: 8,
    antialiasingTolerance: 10
};
