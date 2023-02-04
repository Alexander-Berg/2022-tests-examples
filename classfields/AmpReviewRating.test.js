const React = require('react');
const { shallow } = require('enzyme');

const AmpReviewRating = require('./AmpReviewRating');

describe('rating', () => {
    it('должен поставить рейтинг good, если рейтинг больше 3.5', () => {
        const tree = shallow(
            <AmpReviewRating rating={ 3.6 }/>,
        );

        expect(tree).toHaveClassName('AmpReviewRating_rating_good');
    });

    it('должен поставить рейтинг neutral, если рейтинг больше 2.5', () => {
        const tree = shallow(
            <AmpReviewRating rating={ 2.6 }/>,
        );

        expect(tree).toHaveClassName('AmpReviewRating_rating_neutral');
    });

    it('должен поставить рейтинг bad, если рейтинг меньше 2.5', () => {
        const tree = shallow(
            <AmpReviewRating rating={ 2.4 }/>,
        );

        expect(tree).toHaveClassName('AmpReviewRating_rating_bad');
    });
});

describe('форматирование', () => {
    it('должен написать целое число как дробное', () => {
        const tree = shallow(
            <AmpReviewRating rating={ 5 }/>,
        );

        expect(tree.find('.AmpReviewRating__number')).toHaveText('5,0');
    });

    it('должен написать дробное число как дробное', () => {
        const tree = shallow(
            <AmpReviewRating rating={ 4.5 }/>,
        );

        expect(tree.find('.AmpReviewRating__number')).toHaveText('4,5');
    });
});
