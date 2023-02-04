import pytest

from smarttv.droideka.proxy.models import Category2
from smarttv.droideka.proxy.blackbox import SubscriptionType
from smarttv.droideka.proxy.views.categories import CategoryPatcher


class TestCategoryPatcher:
    RANDOM_ID = 'random_id'

    @pytest.mark.parametrize('category, subscription, expected_id', [
        (Category2(category_id=CategoryPatcher.KP_CATEGORY_ID_TO_PATCH), SubscriptionType.YA_PLUS, 'ya_plus'),
        (Category2(category_id=CategoryPatcher.KP_CATEGORY_ID_TO_PATCH), SubscriptionType.YA_PLUS_KP, 'ya_plus'),
        (Category2(category_id=CategoryPatcher.KP_CATEGORY_ID_TO_PATCH), SubscriptionType.YA_PLUS_3M, 'ya_plus'),
        (Category2(category_id=CategoryPatcher.KP_CATEGORY_ID_TO_PATCH), SubscriptionType.KP_BASIC, 'kp_basic'),
        (Category2(category_id=CategoryPatcher.KP_CATEGORY_ID_TO_PATCH), SubscriptionType.YA_PLUS_SUPER, 'ya_plus_super'),
        (Category2(category_id=CategoryPatcher.KP_CATEGORY_ID_TO_PATCH), SubscriptionType.YA_PREMIUM, 'ya_premium'),
    ])
    def test_main_category_exists_and_patched(self,
                                              category: Category2, subscription: SubscriptionType, expected_id: str):
        assert category.category_id == CategoryPatcher.KP_CATEGORY_ID_TO_PATCH
        CategoryPatcher(subscription, [category]).patch()
        assert category.category_id == expected_id

    @pytest.mark.parametrize('category, subscription, expected_id', [
        (Category2(category_id=RANDOM_ID), SubscriptionType.YA_PLUS, RANDOM_ID),
        (Category2(category_id=RANDOM_ID), SubscriptionType.YA_PLUS_KP, RANDOM_ID),
        (Category2(category_id=RANDOM_ID), SubscriptionType.YA_PLUS_3M, RANDOM_ID),
        (Category2(category_id=RANDOM_ID), SubscriptionType.KP_BASIC, RANDOM_ID),
        (Category2(category_id=RANDOM_ID), SubscriptionType.YA_PLUS_SUPER, RANDOM_ID),
        (Category2(category_id=RANDOM_ID), SubscriptionType.YA_PREMIUM, RANDOM_ID),
    ])
    def test_main_category_absent_nothing_patched(
            self, category: Category2, subscription: SubscriptionType, expected_id: str):
        assert category.category_id == self.RANDOM_ID
        CategoryPatcher(subscription, [category]).patch()
        assert category.category_id == self.RANDOM_ID
