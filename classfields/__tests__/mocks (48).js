export const getLinks = ({ count = 1 } = {}) => {
    return [
        {
            section: 'infrastructure'
        },
        {
            section: 'proposals'
        },
        {
            section: 'decoration',
            count
        },
        {
            section: 'progress'
        },
        {
            section: 'reviews'
        }
    ];
};
