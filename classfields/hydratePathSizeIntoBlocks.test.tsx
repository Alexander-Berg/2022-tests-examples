import React from 'react';

import { BlockName } from 'auto-core/react/components/mobile/CreditWizard/CreditWizardBlocks/types/blockName';

import type { BlockMeta } from '../../types/blockMeta';
import type { CreditWizardBlockProps } from '../../types/creditWizardBlockProps';

import { hydratePathSizeIntoBlocks } from './hydratePathSizeIntoBlocks';

const StubGenerator = (props: CreditWizardBlockProps) => (<div { ...props }/>);

describe('hydratePathSizeIntoBlocks', () => {
    const blocks: Array<BlockMeta> = [
        {
            name: BlockName.PASSPORT,
            generator: StubGenerator,
            nextBlocks: [
                BlockName.OWN_DRIVER_LICENSE,
            ],
            maxPath: 0,
        },
        {
            name: BlockName.OWN_DRIVER_LICENSE,
            generator: StubGenerator,
            nextBlocks: [
                BlockName.SURNAME_CHANGED, // size 1
                BlockName.REGISTRATION_ADDRESS, // size 3
                BlockName.SELF_EMPLOYMENT_ABOUT_COMPANY, // size 1
            ],
            maxPath: 0,
        },
        {
            name: BlockName.SURNAME_CHANGED,
            generator: StubGenerator,
            nextBlocks: [
                BlockName.PREVIOUS_SURNAME,
            ],
            maxPath: 0,
        },
        {
            name: BlockName.PREVIOUS_SURNAME,
            generator: StubGenerator,
            nextBlocks: [
                BlockName.EMPLOYMENT_ABOUT_COMPANY,
            ],
            maxPath: 0,
        },
        {
            name: BlockName.REGISTRATION_ADDRESS,
            generator: StubGenerator,
            nextBlocks: [
                BlockName.RESIDENCE_ADDRESS_CHOOSE,
            ],
            maxPath: 0,
        },
        {
            name: BlockName.RESIDENCE_ADDRESS_CHOOSE,
            generator: StubGenerator,
            nextBlocks: [
                BlockName.RESIDENCE_ADDRESS,
            ],
            maxPath: 0,
        },
        {
            name: BlockName.RESIDENCE_ADDRESS,
            generator: StubGenerator,
            nextBlocks: [
                BlockName.EMPLOYMENT_CHOOSE,
            ],
            maxPath: 0,
        },
        {
            name: BlockName.EMPLOYMENT_CHOOSE,
            generator: StubGenerator,
            nextBlocks: [
                BlockName.EMPLOYMENT_ABOUT_COMPANY,
            ],
            maxPath: 0,
        },
        {
            name: BlockName.SELF_EMPLOYMENT_ABOUT_COMPANY,
            generator: StubGenerator,
            nextBlocks: [
                BlockName.EMPLOYMENT_ABOUT_COMPANY,
            ],
            maxPath: 0,
        },
        {
            name: BlockName.EMPLOYMENT_ABOUT_COMPANY,
            generator: StubGenerator,
            nextBlocks: [
            ],
            maxPath: 0,
        },
    ];

    it('должен посчитать правильно посчитать максимальную длинну', () => {
        const hydratedBlocks = hydratePathSizeIntoBlocks(blocks);

        const getPath = (blockName: BlockName) =>
            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
            hydratedBlocks.find(
                ({ name }) => name === blockName,
            )!.maxPath;

        expect(getPath(BlockName.PASSPORT)).toBe(6);
        expect(getPath(BlockName.OWN_DRIVER_LICENSE)).toBe(5);
        expect(getPath(BlockName.SURNAME_CHANGED)).toBe(2);
        expect(getPath(BlockName.PREVIOUS_SURNAME)).toBe(1);
        expect(getPath(BlockName.REGISTRATION_ADDRESS)).toBe(4);
        expect(getPath(BlockName.RESIDENCE_ADDRESS_CHOOSE)).toBe(3);
        expect(getPath(BlockName.RESIDENCE_ADDRESS)).toBe(2);
        expect(getPath(BlockName.EMPLOYMENT_CHOOSE)).toBe(1);
        expect(getPath(BlockName.SELF_EMPLOYMENT_ABOUT_COMPANY)).toBe(1);
        expect(getPath(BlockName.EMPLOYMENT_ABOUT_COMPANY)).toBe(0);
    });
});
