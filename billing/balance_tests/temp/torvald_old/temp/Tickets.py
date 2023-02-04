import xmlrpclib
import pprint


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


price = 116
simple = xmlrpclib.ServerProxy('http://greed-ts1f.yandex.ru:8018/simple/xmlrpc')

tickets_token = 'tickets_f4ac4122ee48c213eec816f4d7944ea6'
tickets_service_product = '1408716650364'
taxifee_token = 'taxifee_8c7078d6b3334e03c1b4005b02da30f4'
taxifee_service_product = '999012_ride'

##service_token = taxifee_token
##service_product_id = taxifee_service_product
service_token = tickets_token
service_product_id = tickets_service_product


def check_basket(trust_payment_id):
    result = simple.BalanceSimple.CheckBasket(service_token,
                                              {'trust_payment_id': trust_payment_id, 'user_ip': '127.0.0.1'})
    return result


def create_refund(purchase_token, trust_payment_id, order_id, sum):
    Refund = simple.BalanceSimple.CreateRefund(service_token, {'purchase_token': purchase_token, 'user_ip': '127.0.0.1',
                                                               'reason_desc': 'test1',
                                                               'trust_payment_id': trust_payment_id, 'orders': [
            {'service_order_id': order_id, 'delta_amount': sum}]})
    return Refund


def do_refund(purchase_token, trust_refund_id):
    status = simple.BalanceSimple.DoRefund(service_token, {'purchase_token': purchase_token, 'user_ip': '127.0.0.1',
                                                           'trust_refund_id': trust_refund_id})
    return status


def test_method_tickets():
    Order = simple.BalanceSimple.CreateOrderOrSubscription(service_token, {'uid': '3000248777', 'user_ip': '127.0.0.1',
                                                                           'region_id': 225,
                                                                           'service_product_id': service_product_id,
                                                                           'commission_category': 30})
    print Order

    order_id = Order['service_order_id']
    print 'order_id: %s' % order_id

    Basket = simple.BalanceSimple.CreateBasket(service_token, {'user_ip': '127.0.0.1', 'currency': 'RUB', 'orders': [
        {'service_order_id': order_id, 'price': price}
    ]
                                                               })
    print Basket

    purchase_token = Basket['purchase_token']
    print 'purchase_token: %s' % purchase_token
    trust_payment_id = Basket['trust_payment_id']
    print 'trust_payment_id: %s' % trust_payment_id

    PaymentForm = simple.BalanceSimple.PayBasket(service_token, {'purchase_token': purchase_token,
                                                                 'trust_payment_id': trust_payment_id,
                                                                 'back_url': 'http://balance-dev.yandex.ru',
                                                                 'user_ip': '127.0.0.1',
                                                                 'paymethod_id': 'trust_web_page',
                                                                 'return_path': 'http://ya.ru', })

    print '%s?purchase_token=%s' % (
        PaymentForm['payment_form']['_TARGET'], PaymentForm['payment_form']['purchase_token'])
    print '5555 5555 5555 4444; Any date; CVN: 123; Any name; email'

    Print(check_basket(str(trust_payment_id)))

    Refund = create_refund(purchase_token, trust_payment_id, order_id, 60)
    Refund = create_refund(purchase_token, trust_payment_id, order_id, 56)


test_method_tickets();
