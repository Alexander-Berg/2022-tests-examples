const React = require('react');
const { shallow } = require('enzyme');

const CarBodyImage = require('./CarBodyImage');

describe('должен отрисовать изображения для кузова, у которого есть соответствующий набор изображений', () => {
    it('вид сбоку', () => {
        const bodyImageHatch5DoorsSide = require('./images/hatch5d-side.png');

        const tree = shallow(
            <CarBodyImage bodyType="HATCHBACK_5_DOORS" direction="side"/>,
        );

        expect(
            tree.find('.CarBodyImage').prop('src'),
        ).toEqual(bodyImageHatch5DoorsSide);
    });

    it('вид спереди', () => {
        const bodyImageHatch5DoorsFront = require('./images/hatch5d-front.png');

        const tree = shallow(
            <CarBodyImage bodyType="HATCHBACK_5_DOORS" direction="side"/>,
        );

        expect(
            tree.find('.CarBodyImage').prop('src'),
        ).toEqual(bodyImageHatch5DoorsFront);
    });
});

describe('должен привести тип кузова, у которого нет набора изображений, с к одному из тех, у которых есть изображения и отрисовать', () => {
    it('вид сбоку', () => {
        const bodyImageMiniVanSide = require('./images/vclasse-side.png');

        const tree = shallow(
            <CarBodyImage bodyType="COMPACTVAN" direction="side"/>,
        );

        expect(
            tree.find('.CarBodyImage').prop('src'),
        ).toEqual(bodyImageMiniVanSide);
    });

    it('вид спереди', () => {
        const bodyImageMiniVanFront = require('./images/vclasse-front.png');

        const tree = shallow(
            <CarBodyImage bodyType="COMPACTVAN" direction="side"/>,
        );

        expect(
            tree.find('.CarBodyImage').prop('src'),
        ).toEqual(bodyImageMiniVanFront);
    });
});

describe('если не удалось привести тип кузова, то отрисовать седан', () => {
    it('вид сбоку', () => {
        const bodyImageSedanFront = require('./images/sedan-front.png');

        const tree = shallow(
            <CarBodyImage bodyType="LANDO" direction="side"/>,
        );

        expect(
            tree.find('.CarBodyImage').prop('src'),
        ).toEqual(bodyImageSedanFront);
    });

    it('вид спереди', () => {
        const bodyImageSedanSide = require('./images/sedan-side.png');

        const tree = shallow(
            <CarBodyImage bodyType="LANDO" direction="side"/>,
        );

        expect(
            tree.find('.CarBodyImage').prop('src'),
        ).toEqual(bodyImageSedanSide);
    });
});
