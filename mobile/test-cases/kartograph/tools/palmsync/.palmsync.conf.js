const {formats} = require('@yandex-int/palmsync');
const dotenv = require('dotenv');
const commonConfig = require('../../../common/.common.palmsync.conf.js')

dotenv.config();

module.exports = {
    ...commonConfig,

    project: process.env.TESTPALM_PROJECT,

    sets: {
        yaml: {
            'mobile-app': {
                specs: [
                    '../../features/mobile-app/scenarios/**/*.yml',
                ],
                envs: [{
                    SET_NAME: 'mobile-app',
                }],
                browsers: ['none']
            },
        },
    },
    schemeExtension: [
        // custom names
        {
            name: "testing",
            required: false,
            meta: true,
            mergeable: true,
            format: formats.isArrayOfEnums(
                'regress',
                'smoke',
                'acceptance',
                'real_road_test'
            )
        },
        {
            name: "platforms",
            required: false,
            mergeable: true,
            meta: true,
            format: formats.isArrayOfEnums('ios', 'android')
        },
        {
            name: "components",
            required: false,
            mergeable: true,
            meta: true,
            format: formats.isEnum(
                'auth',
                'main_screen',
                'permission',
                'gps',
                'camera',
                'route',
                'settings',
                'share',
                'video_recorder',
                'foreground',
                'gallery',
                'real_route',
                'update',
                'dka'

            )
        },
        {
            name: "integration_run",
            meta: true,
            mergeable: true,
            format: formats.isArrayOfEnums(
                'am_sdk',
                'mrc'
            )
        },
        {
            name: "tags",
            required: false,
            mergeable: true,
            meta: true,
            format: formats.isArrayOfEnums(
                'assessors',
                'not_suitable_for_farm',
                'mobile_internet'
            )
        },

        // system names
        {
            name: "hermioneTitle",
            required: false,
            meta: true,
        },
        {
            name: "palmsync_synchronization_errors",
            required: false,
            meta: true,
        },
        {
            name: "browsers",
            required: false,
            meta: true,
        },
        {
            name: "filepath",
            required: false,
            meta: true,
        },
        {
            name: "scenarioType",
            required: false,
            meta: true,
        },
        {
            name: "feature",
            required: false,
            meta: true,
        }
    ]
};
