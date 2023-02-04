package ru.auto.tests.desktop.mock.beans.auctionState;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class CompetitiveBid {

    String bid;
    String competitors;

    public static CompetitiveBid bid() {
        return new CompetitiveBid();
    }

}
