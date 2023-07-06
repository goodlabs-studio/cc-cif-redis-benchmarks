package studio.goodlabs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;

public class DataGenerator {

    private static final Logger logger = LoggerFactory.getLogger(DataGenerator.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        Supplier<CharSequence> creditCardNoGenerator = new CreditCardNoGenerator();
        Supplier<String> cifGenerator = new CIFGenerator();
        int accountsTotal = 36_000_000;
        Path mappingFile = Path.of("data", "cc_cif.txt");
        if (Files.exists(mappingFile))
            Files.delete(mappingFile);
        try (BufferedWriter out = Files.newBufferedWriter(mappingFile, StandardOpenOption.CREATE_NEW)) {
            for (int i = 0; i < accountsTotal; ++i) {
                String ccNo = creditCardNoGenerator.get().toString();
                String cif = cifGenerator.get();
                out.write(ccNo);
                out.write(' ');
                out.write(cif);
                out.write('\n');
            }
        }
    }

    // PoC for TD using Redis in Azure.
    // 36MM accounts mapping between card number (16 digits) to another number called CIF (9 digits) to measure the latency on lookup on the client side.
    // DSAP will be providing this mapping on their system and another system called FDIP outside of DSAP will perform the lookup.
    // We need to know what the 36MM mapping Redis's size will look like and how various approaches including client side caching performance will look like.
    // We will also need the understanding on how fast the mapping update (only insert) will reflect in the mapping.

}
