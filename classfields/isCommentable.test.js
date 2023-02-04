const isCommentable = require('./isCommentable');
const _ = require('lodash');

const PTS_OWNERS = {
    header: { title: 'Владельцы по ПТС', updated: '43252352352365235' },
    data: {},
};

const PTS_INFO = {
    header: { title: 'Данные по ПТС', updated: '3029429839238724' },
    commentable: {
        add_comment: true,
    },
};

const DTP = {
    items: [ {
        commantable: {
            add_comment: false,
        },
    } ],
};

const DTP_COMMENTABLE = {
    items: [ {
        commantable: {
            add_comment: false,
        },
    }, {
        commantable: {
            add_comment: true,
        },
    } ],
};

const VIN_REPORT = {
    report: {
        pts_owners: PTS_OWNERS,
        pts_info: PTS_INFO,
        dtp: DTP,
    },
};

it('isCommentable должен вернуть `true`', () => {
    const result = isCommentable('pts_info', VIN_REPORT);
    expect(result).toEqual(true);
});

it('isCommentable должен вернуть `undefined`', () => {
    const result = isCommentable('pts_owners', VIN_REPORT);
    expect(result).toBeUndefined();
});

it('isCommentable должен правильно обработать `type: "DTP"` когда нельзя', () => {
    const result = isCommentable('dtp', VIN_REPORT);
    expect(result).toEqual(false);
});

it('isCommentable должен правильно обработать `type: "DTP"` когда можно', () => {
    const report = _.cloneDeep(VIN_REPORT);
    report.report.dtp = DTP_COMMENTABLE;
    const result = isCommentable('dtp', report);
    expect(result).toEqual(false);
});
