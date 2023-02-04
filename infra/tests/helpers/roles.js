import { Role } from 'testcafe';

import { HOST, ROBOT_LOGIN, ROBOT_PASSWORD, TIMEOUTS } from './constants';

export const robotRole = Role(
   `https://passport.yandex-team.ru/passport?mode=auth&retpath=${encodeURIComponent(HOST)}`,
   async t => {
      await t
         .typeText('input[name=login]', ROBOT_LOGIN)
         .typeText('input[name=passwd]', ROBOT_PASSWORD)
         .click('button[type=submit]')
         .wait(TIMEOUTS.afterPassport);
   },
);
