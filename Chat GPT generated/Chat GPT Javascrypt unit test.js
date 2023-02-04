const BankAccount = require('./bank_account');

describe('BankAccount', () => {
  describe('#Debit', () => {
    it('should subtract the specified amount from the balance', () => {
      const account = new BankAccount(100.0);
      const debitAmount = 50.0;
      account.debit(debitAmount);
      const expected = 100.0 - debitAmount;
      const actual = account.balance;
      expect(actual).toEqual(expected);
    });

    it('should throw an error if the amount is greater than the balance', () => {
      const account = new BankAccount(100.0);
      const debitAmount = 100.0 + 1.0;
      expect(() => account.debit(debitAmount)).toThrow();
    });

    it('should throw an error if the amount is negative', () => {
      const account = new BankAccount(100.0);
      const debitAmount = -50.0;
      expect(() => account.debit(debitAmount)).toThrow();
    });
  });

  describe('#Credit', () => {
    it('should add the specified amount to the balance', () => {
      const account = new BankAccount(100.0);
      const creditAmount = 50.0;
      account.credit(creditAmount);
      const expected = 100.0 + creditAmount;
      const actual = account.balance;
      expect(actual).toEqual(expected);
    });

    it('should throw an error if the amount is negative', () => {
      const account = new BankAccount(100.0);
      const creditAmount = -50.0;
      expect(() => account.credit(creditAmount)).toThrow();
    });
  });
});
