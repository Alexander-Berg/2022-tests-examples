import * as path from 'path';
import * as express from 'express';
import common, {Config} from './common';

const testing: Config = {
    ...common,
    taskPlannerUrl: 'http://core-nmaps-mrc-tasksplanner.common.testing.maps.yandex.net/',
    assetsRouter: express.Router().use('/assets', express.static(path.resolve(__dirname, '../assets/'))),
    blackboxYandexApi: 'blackbox-mimino.yandex.net/blackbox',
    blackboxYandexTeamApi: 'blackbox.yandex-team.ru/blackbox',
    passportUrl: 'passport.yandex-team.ru/passport'
};

export default testing;
