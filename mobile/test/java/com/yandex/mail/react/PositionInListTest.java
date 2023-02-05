package com.yandex.mail.react;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

public class PositionInListTest {

    @Test
    public void createShouldReturnCorrectInstances() {
        boolean[] allVariants = new boolean[] {false, true};
        for (boolean canMoveUp : allVariants) {
            for (boolean canMoveDown : allVariants) {
                PositionInList positionInList = PositionInList.create(canMoveUp, canMoveDown);
                assertThat(positionInList.getCanMoveUp()).isEqualTo(canMoveUp);
                assertThat(positionInList.getCanMoveDown()).isEqualTo(canMoveDown);
            }
        }
    }

    @Test
    public void fromPositionShouldReturnCorrectInstances() {
        assertThat(PositionInList.fromPosition(0, 20)).isEqualTo(PositionInList.FIRST);
        assertThat(PositionInList.fromPosition(19, 20)).isEqualTo(PositionInList.LAST);
        assertThat(PositionInList.fromPosition(7, 20)).isEqualTo(PositionInList.MIDDLE);

        try {
            PositionInList.fromPosition(-1, 20);
            fail("Should throw Illegal argument exception if position is out of bound");
        } catch (IllegalArgumentException e) {
            // it's ok
        }

        try {
            PositionInList.fromPosition(23, 20);
            fail("Should throw Illegal argument exception if position is out of bound");
        } catch (IllegalArgumentException e) {
            // it's ok
        }
    }
}
