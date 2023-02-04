/* global window */
import { getAllureMock } from '../tests-helpers/utils';

// Mock для puppeteer
if (typeof window !== 'undefined') {
    window.__allure__ = getAllureMock();
}
