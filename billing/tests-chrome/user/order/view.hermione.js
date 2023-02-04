describe('user', () => {
    describe('order', () => {
        it('пустой заказ', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({});
            const [, , , order_id] = await browser.ybRun('test_client_empty_order', {
                login,
                fixed_dt: true
            });

            await browser.ybUrl('user', `order.xml?order_id=${order_id}`);
            await browser.waitForVisible('.content');
            await browser.ybAssertView('просмотр страницы заказа, пустой', '.content', {
                hideElements: ['h1']
            });
        });

        it('заказ со скидкой и возвратом, под админом', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });

            await browser.ybUrl('user', `order.xml?service_cc=PPC&service_order_id=47175932`);
            await browser.waitForVisible('.content');
            await browser.waitForVisible('#consumes_data tr:nth-child(2)');
            await browser.waitForVisible('#operations_data tr:nth-child(2)');
            await browser.ybAssertView('заказ со скидкой и возвратом', '.content');

            await browser.ybAssertLink('a=Б-1921528283-1', 'invoice.xml?invoice_id=103312094');
        });

        it('пагинация в зачислениях, под админом', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });

            await browser.ybUrl(
                'user',
                `order.xml?service_cc=taxi&service_order_id=20000000069991`
            );
            await browser.waitForVisible('.content');
            await browser.waitForVisible('#consumes_data tr:nth-child(2)');
            await browser.ybAssertView('первая страница зачислений', '.content div:nth-child(5)');

            await browser.selectByValue('select[id="consumes_data_sel"]', '20');
            await browser.waitForVisible('#consumes_data tr:nth-child(20)');
            await browser.ybAssertView('зачисления, 20 элементов', '.content div:nth-child(5)');

            await browser.click('#consumes_data_a2');
            await browser.waitForVisible('#consumes_data_a_prev');
            await browser.ybAssertView('зачисления, вторая страница', '.content div:nth-child(5)');
        });

        it('пагинация в операциях, под админом', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });

            await browser.ybUrl(
                'user',
                `order.xml?service_cc=taxi&service_order_id=20000000069991`
            );
            await browser.waitForVisible('.content');
            await browser.waitForVisible('#operations_data tr:nth-child(2)');
            await browser.ybAssertView('первая страница операций', '.content div:nth-child(6)');

            await browser.selectByValue('select[id="operations_data_sel"]', '20');
            await browser.waitForVisible('#operations_data tr:nth-child(20)');
            await browser.ybAssertView('операции, 20 элементов', '.content div:nth-child(6)');

            await browser.click('#operations_data_a2');
            await browser.waitForVisible('#operations_data_a_prev');
            await browser.ybAssertView('операции, вторая страница', '.content div:nth-child(6)');
        });

        it('частично открученный и заакченный заказ', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({});
            const [, , , order_id] = await browser.ybRun('test_client_part_compl', { login });

            await browser.ybUrl('user', `order.xml?order_id=${order_id}`);
            await browser.waitForVisible('.content');
            await browser.waitForVisible('#consumes_data tr:nth-child(2)');
            await browser.waitForVisible('#operations_data tr:nth-child(2)');
            let hideElements = ['h1', '.date', '.invoice-eid', '.operation-id'];
            await browser.ybAssertView(
                'просмотр страницы заказа, частично открученный и заакченный',
                '.content',
                {
                    hideElements: hideElements
                }
            );
        });

        it('просмотр пустого заказа под бухлогином', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({});
            const [client_id, , , id] = await browser.ybRun('test_client_empty_order', {
                fixed_dt: true
            });
            await browser.ybRun('test_delete_every_accountant_role', { login });
            await browser.ybRun('test_unlink_client', { login });
            await browser.ybRun('test_add_accountant_role', { login, client_id });
            await browser.ybUrl('user', 'order.xml?order_id=' + id);
            await browser.waitForVisible('.content');

            await browser.ybAssertView('страница, пустой заказ под бухлогином', '.content', {
                hideElements: ['h1']
            });

            await browser.ybRun('test_delete_every_accountant_role', { login });
        });
    });
});
