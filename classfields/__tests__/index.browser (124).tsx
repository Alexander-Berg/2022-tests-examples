import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { QRCode } from '../';

import { CorrectionLevel } from '../generateQRCode';

const testData: Array<{
    version: number;
    content: string;
    correctionLevel: CorrectionLevel;
    width: number;
    height: number;
    indent?: number;
}> = [
    {
        version: 1,
        content: 'tel:+79998887766',
        correctionLevel: 'L',
        width: 100,
        height: 100,
        indent: 6,
    },
    {
        version: 3,
        content: 'Найти в Яндексе',
        correctionLevel: 'M',
        width: 150,
        height: 150,
        indent: 3,
    },
    {
        version: 5,
        content: 'Ceteros assentior omittantur cum ad. Per in illud petentium',
        correctionLevel: 'Q',
        width: 200,
        height: 200,
    },
    {
        version: 6,
        content: 'Таким образом реализация ОКРВП.',
        correctionLevel: 'H',
        width: 250,
        height: 250,
        indent: 0,
    },
    {
        version: 10,
        content:
            // eslint-disable-next-line max-len
            'Sale liber et vel. Per cu iracundia splendide. Vel in dicant cetero phaedrum, usu populo interesset cu, eum ea facer nostrum pericula. Postulant assueverit ea his. Sale liber et vel. Sea esse deserunt. Eu eam dolores lobortis percipitur, quo equidem deleniti disputando.',
        correctionLevel: 'L',
        width: 250,
        height: 250,
        indent: 0,
    },
    {
        version: 20,
        content:
            // eslint-disable-next-line max-len
            'Magna copiosae apeirian ius at. Eu cum iuvaret debitis voluptatibus, esse perfecto reformidans id has. Vix paulo sanctus scripserit ex, te iriure insolens voluptatum qui. Sale liber et vel. Mandamus abhorreant deseruisse mea at, mea elit deserunt persequeris at, in putant fuisset honestatis qui. Ceteros assentior omittantur cum ad. Eu cum iuvaret debitis voluptatibus, esse perfecto reformidans id has. . An nam debet instructior, commodo mediocrem id cum. An eos iusto solet, id mel dico habemus. Eu eam dolores lobortis percipitur, quo te equidem deleniti disputando. . Mandamus abhorreant deseruisse mea at, mea elit deserunt persequeris at, putant fuisset qui.',
        correctionLevel: 'M',
        width: 250,
        height: 250,
        indent: 0,
    },
    {
        version: 30,
        content:
            // eslint-disable-next-line max-len
            'Ceteros assentior omittantur cum ad. Postulant assueverit ea his. Lorem ipsum dolor sit amet, an eos lorem ancillae expetenda, vim et utamur quaestio. Per in illud petentium iudicabit, integre sententiae pro no. Vel in dicant cetero phaedrum, usu populo interesset cu, eum ea facer nostrum pericula. Oratio accumsan et mea. Lorem ipsum dolor sit amet, an eos lorem ancillae expetenda, vim et utamur quaestio. Elitr accommodare deterruisset eam te, vim munere pertinax consetetur at. Sea esse deserunt ei, no diam ubique euripidis has. Nec labore cetero theophrastus no, ei vero facer veritus nec. Per in illud petentium iudicabit, integre sententiae pro no. Nec labore cetero theophrastus no, ei vero facer veritus nec. Tation delenit percipitur at vix. Magna copiosae apeirian ius at. Vel in dicant cetero phaedrum, usu populo interesset cu, eum ea facer nostrum pericula. Lorem ipsum dolor sit amet, an eos lorem ancillae expetenda, vim et utamur quaestio.',
        correctionLevel: 'Q',
        width: 400,
        height: 400,
        indent: 0,
    },
    {
        version: 40,
        content:
            // eslint-disable-next-line max-len
            'Eam id posse dictas voluptua, veniam laoreet oportere no mea, quis regione suscipiantur mea an. Per cu iracundia splendide. Eu eam dolores lobortis percipitur, quo te equidem deleniti disputando. Lorem ipsum dolor sit amet, an eos lorem ancillae expetenda, vim et utamur quaestio. Elitr accommodare deterruisset eam te, vim munere pertinax consetetur at. Sale liber et vel. Ius dicat feugiat no, vix cu modo dicat principes. Odio contentiones sed cu, usu commodo prompta prodesset id. Per in illud petentium iudicabit, integre sententiae pro no. Oratio accumsan et mea. Postulant assueverit ea his. Eu cum iuvaret debitis voluptatibus, esse perfecto reformidans id has. Lorem ipsum dolor sit amet, an eos lorem ancillae expetenda, vim et utamur quaestio. Sale liber et vel. Tation delenit percipitur at vix. Vivendum dignissim conceptam pri ut, ei vel partem audiam sapientem. Magna copiosae apeirian ius at. Postulant assueverit ea his. Eu cum iuvaret debitis voluptatibus. Ceteros assentior omittantur cum ad. An eos iusto solet, id mel dico habemus. Sea esse deserunt ei, no diam ubique euripidis has. Ius dicat feugiat no, vix cu modo dicat principes. Magna copiosae apeirian ius at. Eu cum iuvaret debitis voluptatibus, esse perfecto reformidans id has.',
        correctionLevel: 'H',
        width: 500,
        height: 500,
        indent: 0,
    },
];

describe('QRCode', () => {
    testData.forEach((testCase) => {
        it(`QR версия ${testCase.version} коррекция ${testCase.correctionLevel}`, async () => {
            await render(
                <QRCode
                    content={testCase.content}
                    correctionLevel={testCase.correctionLevel}
                    indent={testCase.indent}
                    width={testCase.width}
                    height={testCase.height}
                />,
                {
                    viewport: { width: testCase.width + 50, height: testCase.height + 50 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
