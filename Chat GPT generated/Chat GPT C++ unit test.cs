#include <gtest/gtest.h>
#include "bank_account.h"

TEST(BankAccountTest, TestDebitWithValidAmount) {
  BankAccount account(100.0);
  double debit_amount = 50.0;
  account.Debit(debit_amount);
  double expected = 100.0 - debit_amount;
  double actual = account.Balance();
  EXPECT_EQ(expected, actual);
}

TEST(BankAccountTest, TestDebitWithAmountGreaterThanBalance) {
  BankAccount account(100.0);
  double debit_amount = 100.0 + 1.0;
  EXPECT_THROW(account.Debit(debit_amount), std::invalid_argument);
}

TEST(BankAccountTest, TestDebitWithNegativeAmount) {
  BankAccount account(100.0);
  double debit_amount = -50.0;
  EXPECT_THROW(account.Debit(debit_amount), std::invalid_argument);
}

TEST(BankAccountTest, TestCreditWithValidAmount) {
  BankAccount account(100.0);
  double credit_amount = 50.0;
  account.Credit(credit_amount);
  double expected = 100.0 + credit_amount;
  double actual = account.Balance();
  EXPECT_EQ(expected, actual);
}

TEST(BankAccountTest, TestCreditWithNegativeAmount) {
  BankAccount account(100.0);
  double credit_amount = -50.0;
  EXPECT_THROW(account.Credit(credit_amount), std::invalid_argument);
}

int main(int argc, char** argv) {
  ::testing::InitGoogleTest(&argc, argv);
  return RUN_ALL_TESTS();
}
