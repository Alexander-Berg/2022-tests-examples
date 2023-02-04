const now = Date.now();
const expireDate = now + 1000 * 60 * 60 * 24;
const createDate = now - 1000 * 60 * 60 * 24;

module.exports = [
    {
        service: 'all_sale_toplist',
        expire_date: expireDate,
        create_date: createDate,
        is_active: true,
    },
    {
        service: 'all_sale_special',
        expire_date: expireDate,
        create_date: createDate,
        is_active: true,
    },
    {
        service: 'all_sale_color',
        expire_date: expireDate,
        create_date: createDate,
        is_active: true,
    },
    {
        service: 'package_vip',
        expire_date: expireDate,
        create_date: createDate,
        is_active: true,
    },
    {
        service: 'all_sale_activate',
        expire_date: expireDate,
        create_date: createDate,
        is_active: true,
    },
];
