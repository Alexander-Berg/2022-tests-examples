import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class BankAccountTest {
  
  private static final double DELTA = 1e-15;
  private static final String CUSTOMER_NAME = "John Doe";
  private static final double INITIAL_BALANCE = 100.0;
  
  @Test
  public void testDebitWithValidAmount() {
    BankAccount account = new BankAccount(CUSTOMER_NAME, INITIAL_BALANCE);
    double debitAmount = 50.0;
    account.debit(debitAmount);
    double expected = INITIAL_BALANCE - debitAmount;
    double actual = account.getBalance();
    assertEquals(expected, actual, DELTA, "Debit amount not debited correctly");
  }

  @Test
  public void testDebitWithAmountGreaterThanBalance() {
    BankAccount account = new BankAccount(CUSTOMER_NAME, INITIAL_BALANCE);
    double debitAmount = INITIAL_BALANCE + 1.0;
    assertThrows(IllegalArgumentException.class, () -> account.debit(debitAmount));
  }

  @Test
  public void testDebitWithNegativeAmount() {
    BankAccount account = new BankAccount(CUSTOMER_NAME, INITIAL_BALANCE);
    double debitAmount = -50.0;
    assertThrows(IllegalArgumentException.class, () -> account.debit(debitAmount));
  }

  @Test
  public void testCreditWithValidAmount() {
    BankAccount account = new BankAccount(CUSTOMER_NAME, INITIAL_BALANCE);
    double creditAmount = 50.0;
    account.credit(creditAmount);
    double expected = INITIAL_BALANCE + creditAmount;
    double actual = account.getBalance();
    assertEquals(expected, actual, DELTA, "Credit amount not credited correctly");
  }

  @Test
  public void testCreditWithNegativeAmount() {
    BankAccount account = new BankAccount(CUSTOMER_NAME, INITIAL_BALANCE);
    double creditAmount = -50.0;
    assertThrows(IllegalArgumentException.class, () -> account.credit(creditAmount));
  }

}
