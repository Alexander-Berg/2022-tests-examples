import {merge} from 'lodash/fp';

import testing from 'server/config/testing/testing';

import common from 'server/config/stand-testing/common';
import platforms from 'server/config/stand-testing/platforms';
import localizations from 'server/config/stand-testing/localizations';

import {ConfigModule} from 'server/config/types';

const standTest: ConfigModule = {
    common: merge(testing.common, common),
    platforms: merge(testing.platforms, platforms),
    localizations: merge(testing.localizations, localizations)
};

export default standTest;
