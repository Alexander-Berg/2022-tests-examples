const { pick } = require('lodash');
const videoUrlsBuilder = require('./videoUrlsBuilder');
const generateValidCombinations = require('../lib/generateValidCombinations');
const { ALLOWED_VIDEO_PARAMS, DEMO_PARAMS } = require('../lib/constants');
const validateVideoParamsCombination = require('../lib/validateVideoParamsCombination');

it('Возвращает все возможные ссылки видео по параметрам', () => {
    const videoCombinations = generateValidCombinations(ALLOWED_VIDEO_PARAMS, validateVideoParamsCombination);
    const params = pick(DEMO_PARAMS, ALLOWED_VIDEO_PARAMS);

    expect(videoUrlsBuilder(params, { videoCombinations })).toMatchSnapshot();
});
