// import selectors from 'constants/selectors';
// import { accounts } from 'constants/accounts';

// В форме логина то ли стили нафигачены, то ли еще что, но некоторые элементы вебдрайвер не может найти.
// Поэтому делаем фиксированное маленькое окошко и кликаем в кнопку по экранным координатам.

// describe('Отключение запроса телефона на месяц.', function () {
//   Object.keys(accounts).forEach(user => {
//     it(`Для учетки ${user}`, function () {
//       return this.browser
//         .setViewportSize({
//           width: 500,
//           height: 720,
//         })
//         .url('https://passport.yandex.ru/auth?mode=password')
//         .waitForVisible(selectors.passport.form)
//         .waitForVisible(selectors.passport.loginInput)
//         .setValue(selectors.passport.loginInput, accounts[user].login)
//         .waitForVisible(selectors.passport.passwordInput)
//         .setValue(selectors.passport.passwordInput, accounts[user].password)
//         .waitAndClick(selectors.passport.shortSessionCheckbox)
//         .waitAndClick(selectors.passport.submitButton)
//         .waitForVisible(selectors.passport.form, null, true)
//         .simulateClick('body', 250, 600)
//         .pause(3000);
//     });
//   });
// });
