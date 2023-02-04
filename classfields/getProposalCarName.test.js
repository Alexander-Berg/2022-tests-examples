const getProposalCarName = require('./getProposalCarName');

it('должен сформировать название авто, которое подбирает пользователь', () => {
    const result = getProposalCarName({
        user_proposal: {
            mark: 'Land Rover',
            model: 'Range Rover',
            tech_params: '150 л.c.',
            body_type: 'седан',
        },
    });

    expect(result).toEqual('Land Rover Range Rover 150 л.c. седан');
});
