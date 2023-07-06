package studio.goodlabs;

import java.io.Closeable;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import static studio.goodlabs.CIFGenerator.CIF_LENGTH;
import static studio.goodlabs.CreditCardNoGenerator.CC_NO_LENGTH;

public class CCCIFFile implements Closeable {

    private static final int RECORD_LENGTH = CC_NO_LENGTH + 1 + CIF_LENGTH + 1;

    private final FileChannel fileChannel;
    private final MappedByteBuffer mappedByteBuffer;

    public CCCIFFile(Path file) throws IOException {
        fileChannel = FileChannel.open(file);
        mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, 36_000_000 * RECORD_LENGTH);
    }

    public Map.Entry<String, String> read(int index) {
        int position = index * RECORD_LENGTH;
        mappedByteBuffer.position(position);
        byte[] buffer = new byte[RECORD_LENGTH];
        mappedByteBuffer.get(buffer);
        String line = new String(buffer, StandardCharsets.US_ASCII);
        String ccNo = line.substring(0, CC_NO_LENGTH);
        String cif = line.substring(CC_NO_LENGTH + 1, CC_NO_LENGTH + 1 + CIF_LENGTH);
        return Map.entry(ccNo, cif);
    }

    @Override
    public void close() throws IOException {
        fileChannel.close();
    }

}
