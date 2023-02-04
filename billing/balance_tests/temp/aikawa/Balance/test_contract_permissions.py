# -*- coding: utf-8 -*-

#
# def test_is_signed_button():
#     user = User(436363430, 'yb-atst-user-15')
#
#     perm_ids = set(steps.PermissionsSteps.get_all_permission_ids()) - {Permissions.REDIRECT_TO_YANDEX_TEAM_36
#                                                                        }
#     steps.UserSteps.set_role_with_permissions_strict(user, perm_ids)

# with reporter.step(u'Проверяем активность галок "Подписан" '
#                    u'(под правом BillingSupport)'):
#     with balance_web.Driver(user=user) as driver:
#         contract_page = balance_web.AdminInterface.ContractPageNew.open(driver)
#         utils.check_that(contract_page.is_signed_button_active(), equal_to(True),
#                          step=u'Проверяем наличие кнопки Внести оплату на странице счета',
#                          error=u'Не найдена кнопка Внести оплату на странице счета')
