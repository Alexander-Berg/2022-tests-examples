package ru.yandex.realty.util.collection;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class LongestCommonSubsequenceTest {

    private byte[] randomByteArray() {
        Random rd = new Random();
        byte[] result = new byte[17 + rd.nextInt(42)];
        rd.nextBytes(result);
        return result;
    }

    private List<Integer> bytesToIntList(byte[] input) {
        return new String(Base64.getMimeEncoder().encode(input), StandardCharsets.UTF_8)
                .codePoints()
                .boxed()
                .collect(Collectors.toList());
    }

    @Test
    public void testLCS() {
        int iters = 512;
        for (int i = 0; i < iters; i++) {
            byte[] left = randomByteArray();
            List<Integer> leftList = bytesToIntList(left);

            byte[] right = randomByteArray();
            List<Integer> rightList = bytesToIntList(right);

            List<Integer> leftToSelf = LongestCommonSubsequence.apply(leftList, leftList);
            List<Integer> rightToSelf = LongestCommonSubsequence.apply(rightList, rightList);

            List<Integer> leftToRight = LongestCommonSubsequence.apply(leftList, rightList);
            List<Integer> rightToLeft = LongestCommonSubsequence.apply(rightList, leftList);

            Assert.assertEquals(leftList, leftToSelf);
            Assert.assertEquals(rightList, rightToSelf);
            Assert.assertEquals(leftToRight.size(), rightToLeft.size());

            Assert.assertEquals(leftToRight, LongestCommonSubsequence.apply(leftList, leftToRight));
            Assert.assertEquals(rightToLeft, LongestCommonSubsequence.apply(rightToLeft, rightList));
        }
    }

}
