package ru.auto.tests.realtyapi.adaptor.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.auto.tests.passport.account.Account;

@Data
@AllArgsConstructor
public class P2PChatRoomComponents {
    private Account sellerAccount;
    private String sellerToken;
    private Account buyerAccount;
    private String buyerToken;
    private String offerId;
    private String roomId;
}
