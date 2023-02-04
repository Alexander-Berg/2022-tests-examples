import {
    allocateAttributes,
    Attribute,
    AttributeAlignment,
    AttributeDescriptor,
    AttributeMapping,
    AttributePointer
} from '../../../src/vector_render_engine/render/attrib_mapping';
import {Type} from '../../../src/vector_render_engine/render/gl/enums';
import {expect} from 'chai';

const TIGHTLY_PACKED_MAPPING: [Attribute, AttributeDescriptor][] = [
    [
        Attribute.POSITION,
        {
            type: Type.FLOAT,
            size: 2,
            normalized: false
        }
    ],
    [
        Attribute.NORMAL,
        {
            type: Type.BYTE,
            size: 4,
            normalized: true
        }
    ],
    [
        Attribute.UV,
        {
            type: Type.SHORT,
            size: 2,
            normalized: true
        }
    ]
];
const TIGHTLY_PACKED_ATTRIBUTE_ORDER = TIGHTLY_PACKED_MAPPING.map((a) => a[0]);

const MAPPING_WITH_PADDINGS: [Attribute, AttributeDescriptor][] = [
    [
        Attribute.POSITION,
        {
            type: Type.BYTE,
            size: 2,
            normalized: false
        }
    ],
    [
        Attribute.NORMAL,
        {
            type: Type.UNSIGNED_BYTE,
            size: 1,
            normalized: true
        }
    ],
    [
        Attribute.UV,
        {
            type: Type.SHORT,
            size: 1,
            normalized: true
        }
    ]
];
const MAPPING_WITH_PADDINGS_ATTRIBUTE_ORDER = MAPPING_WITH_PADDINGS.map((a) => a[0]);

const MAPPING_WITH_BIG_ALIGNMENT: [Attribute, AttributeDescriptor][] = [
    [
        Attribute.POSITION,
        {
            type: Type.FLOAT,
            size: 2,
            normalized: true
        }
    ],
    [
        Attribute.NORMAL,
        {
            type: Type.SHORT,
            size: 2,
            normalized: true
        }
    ],
    [
        Attribute.UV,
        {
            type: Type.BYTE,
            size: 3,
            normalized: true
        }
    ]
];
const MAPPING_WITH_BIG_ALIGNMENT_ATTRIBUTE_ORDER = MAPPING_WITH_BIG_ALIGNMENT.map((a) => a[0]);

function getAttributesOrder(mapping: AttributeMapping): Attribute[] {
    return [...mapping.attributes]
        .sort((a1, a2) => a1[1].offset - a2[1].offset)
        .map((a) => a[0]);
}

function getAttributePointer(mapping: AttributeMapping, attribute: Attribute): AttributePointer | undefined {
    const index = mapping.attributes.findIndex((attr) => attr[0] === attribute);
    return index ===  -1 ? undefined : mapping.attributes[index][1];
}

describe('render/attrib_mapping', () => {
    let tightlyPackedMapping: AttributeMapping;
    let mappingWithPaddings: AttributeMapping;
    let mappingWithBigAlignment: AttributeMapping;

    beforeEach(() => {
        tightlyPackedMapping = allocateAttributes(TIGHTLY_PACKED_MAPPING);
        mappingWithPaddings = allocateAttributes(MAPPING_WITH_PADDINGS);
        mappingWithBigAlignment = allocateAttributes(
            MAPPING_WITH_BIG_ALIGNMENT,
            AttributeAlignment.ALIGN_8_BYTES
        );
    });

    it('should correctly compute size of a vertex structure', () => {
        expect(tightlyPackedMapping.vertexByteSize).to.be.eq(16);
        expect(mappingWithPaddings.vertexByteSize).to.be.eq(12);
        expect(mappingWithBigAlignment.vertexByteSize).to.be.eq(24);
    });

    it('should preserve order of vertex attributes in the vertex structure', () => {
        expect(getAttributesOrder(tightlyPackedMapping))
            .to.be.deep.eq(TIGHTLY_PACKED_ATTRIBUTE_ORDER);
        expect(getAttributesOrder(mappingWithPaddings))
            .to.be.deep.eq(MAPPING_WITH_PADDINGS_ATTRIBUTE_ORDER);
        expect(getAttributesOrder(mappingWithBigAlignment))
            .to.be.deep.eq(MAPPING_WITH_BIG_ALIGNMENT_ATTRIBUTE_ORDER);
    });

    it('should preserve types of vertex attributes', () => {
        for (const [idx, descriptor] of TIGHTLY_PACKED_MAPPING) {
            const {type, size, normalized} = getAttributePointer(tightlyPackedMapping, idx)!;
            expect(type).to.be.eq(descriptor.type);
            expect(size).to.be.eq(descriptor.size);
            expect(normalized).to.be.eq(descriptor.normalized);
        }

        for (const [idx, descriptor] of MAPPING_WITH_PADDINGS) {
            const {type, size, normalized} = getAttributePointer(mappingWithPaddings, idx)!;
            expect(type).to.be.eq(descriptor.type);
            expect(size).to.be.eq(descriptor.size);
            expect(normalized).to.be.eq(descriptor.normalized);
        }

        for (const [idx, descriptor] of MAPPING_WITH_PADDINGS) {
            const {type, size, normalized} = getAttributePointer(mappingWithPaddings, idx)!;
            expect(type).to.be.eq(descriptor.type);
            expect(size).to.be.eq(descriptor.size);
            expect(normalized).to.be.eq(descriptor.normalized);
        }
    });

    it('should correctly compute offsets of vertex attributes', () => {
        expect(getAttributePointer(tightlyPackedMapping, Attribute.POSITION)!.offset).to.be.eq(0);
        expect(getAttributePointer(tightlyPackedMapping, Attribute.NORMAL)!.offset).to.be.eq(8);
        expect(getAttributePointer(tightlyPackedMapping, Attribute.UV)!.offset).to.be.eq(12);

        expect(getAttributePointer(mappingWithPaddings, Attribute.POSITION)!.offset).to.be.eq(0);
        expect(getAttributePointer(mappingWithPaddings, Attribute.NORMAL)!.offset).to.be.eq(4);
        expect(getAttributePointer(mappingWithPaddings, Attribute.UV)!.offset).to.be.eq(8);

        expect(getAttributePointer(mappingWithBigAlignment, Attribute.POSITION)!.offset).to.be.eq(0);
        expect(getAttributePointer(mappingWithBigAlignment, Attribute.NORMAL)!.offset).to.be.eq(8);
        expect(getAttributePointer(mappingWithBigAlignment, Attribute.UV)!.offset).to.be.eq(16);
    });
});
