package studio.goodlabs;

import java.util.Random;
import java.util.function.Supplier;

public class CreditCardNoGenerator implements Supplier<CharSequence> {

    public static final int CC_NO_LENGTH = 16;

    private final Random random = new Random();

    @Override
    public CharSequence get() {
        StringBuilder ccNo = new StringBuilder(CC_NO_LENGTH);
        ccNo.append('4');

        for (int i = 0; i < (CC_NO_LENGTH - 1 - 1); ++i) {
            int digit = random.nextInt(10);
            ccNo.append(digit);
        }

        int checkDigit = computeCheckDigit(ccNo.toString());
        ccNo.append(checkDigit);

        return ccNo;
    }

    private static int computeCheckDigit(String cardNumber) {
        int sum = 0;
        boolean timesTwo = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Integer.parseInt(cardNumber.substring(i, i + 1));
            int addend;
            if (timesTwo) {
                addend = digit * 2;
                if (addend > 9) {
                    addend -= 9; // Subtract 9 is same as adding the digits of the product
                }
            } else {
                addend = digit;
            }
            sum += addend;
            timesTwo = !timesTwo;
        }

        int mod = sum % 10;
        return ((mod == 0) ? 0 : (10 - mod));
    }

}
