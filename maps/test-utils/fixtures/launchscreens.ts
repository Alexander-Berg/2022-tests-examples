import {
    LaunchscreenData, LaunchscreenDownloadableData
} from '../../@types/navi';

export const testLaunchscreenOne: LaunchscreenData = {
    id: '01-test-launchscreen-id',
    name: 'test launchscreen 01',
    isActive: true,
    landscapeImageUrl: 'https://test.ru/landscape.png',
    portraitImageUrl: 'https://test.ru/portrait.png'
};

export const jsonStructure: LaunchscreenDownloadableData = {
    reporting_id: 'sp-launchscreen_test-project_01-test-launchscreen-id',
    logo_portrait: 'portrait.png',
    logo_landscape: 'landscape.png',
    cursors: []
};
