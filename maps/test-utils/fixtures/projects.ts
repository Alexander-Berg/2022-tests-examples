import {ProjectData} from '../../@types/navi';
import {ProjectStatus, Country} from '../../common/navi/constants';

export const testProject: ProjectData = {
    id: 'test-project',
    trackerTask: '',
    status: ProjectStatus.DRAFT,
    description: '',
    projectName: 'test-project',
    country: Country.RU,
    client: {
        id: 'test-client',
        clientName: 'test-client'
    },
    entrypoints: {},
    actions: {},
    introscreens: {},
    launchscreens: {}
};
