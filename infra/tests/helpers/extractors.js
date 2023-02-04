import { Selector } from 'testcafe';

export function getDataE2eSelector(marker, positionSymbol = '') {
   return `[data-e2e${positionSymbol}="${marker}"]`;
}

export function getDataQaSelector(marker, positionSymbol = '') {
   return `[data-qa${positionSymbol}="${marker}"]`;
}

export function SelectDataE2e(marker) {
   return Selector(getDataE2eSelector(marker));
}

export function SelectDataQa(marker) {
   return Selector(getDataQaSelector(marker));
}

export function getDataTestSelector(marker, positionSymbol = '') {
   return `[data-test${positionSymbol}="${marker}"]`;
}

export function SelectDataTest(marker) {
   return Selector(getDataTestSelector(marker));
}

SelectDataTest.startsWith = marker => Selector(getDataTestSelector(marker, '^'));
SelectDataTest.endsWith = marker => Selector(getDataTestSelector(marker, '$'));
