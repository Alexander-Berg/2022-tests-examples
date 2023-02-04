import { ClientFunction } from 'testcafe';

export const getRawConfig = ClientFunction(() => window.CONFIG);

export const getUser = ClientFunction(() => window.USER);
