from ads.bsyeti.tests.eagle.ft.lib.test_environment import check_answer


def test_return_coins(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "p{id}".format(id=id1): {
                "MarketLoyaltyCoins": [
                    {
                        "id": 111111,
                        "update_time": 1500000000,
                        "update_msec": 50,
                        "end_date": 1600000000,
                        "nominal": 100,
                        "reason": 11,
                        "type": 0,
                        "promo_id": 333,
                        "title": "Скидка 100 ₽",
                        "subtitle": "на заказ от 1 000 ₽",
                        "disabled": False,
                    },
                ]
            },
            "p{id}".format(id=id2): {
                "MarketLoyaltyCoins": [
                    {
                        "id": 222222,
                        "update_time": 1500000000,
                        "update_msec": 50,
                        "end_date": 1600000000,
                        "nominal": 100,
                        "reason": 11,
                        "type": 0,
                        "promo_id": 444,
                        "title": "Скидка 100 ₽",
                        "subtitle": "на заказ от 1 000 ₽",
                        "disabled": True,
                    },
                ]
            },
        }
    )

    result = test_environment.request(
        client="debug",
        ids={"puid": id1},
        test_time=1500001000,
        keywords=[1130],
    )
    check_answer(
        {
            "items": [],
            "market_loyalty_coins": [
                {
                    "id": 111111,
                    "update_time": 1500000000,
                    "update_msec": 50,
                    "end_date": 1600000000,
                    "nominal": 100,
                    "reason": 11,
                    "type": 0,
                    "promo_id": 333,
                    "title": "Скидка 100 ₽",
                    "subtitle": "на заказ от 1 000 ₽",
                    "disabled": False,
                    "source_uniq_index": 0,
                },
            ],
        },
        result.answer,
    )

    result = test_environment.request(
        client="debug",
        ids={"puid": id2},
        test_time=1500001000,
        keywords=[1130],
    )
    check_answer({"items": []}, result.answer)
