package studio.goodlabs;

import java.util.Random;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

public class CIFGenerator implements Supplier<String> {

    public static final int CIF_LENGTH = 9;

    private final RandomGenerator random = new Random();

    @Override
    public String get() {
        int randomNumber = random.nextInt(1_000_000_000);
        // Convert to string and pad with leading zeroes to 9 digits
        return String.format("%09d", randomNumber);
    }

}
