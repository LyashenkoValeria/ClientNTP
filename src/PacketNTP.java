import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class PacketNTP {
    private byte leapIndicator = 0;
    private byte versionNumber = 4;
    private byte mode = 3;
    private byte stratum = 0;
    private byte poll = 0;
    private byte precision = 0;
    private double rootDelay = 0;
    private double rootDispersion = 0;
    private byte[] referenceId = {0, 0, 0, 0};
    private double referenceTimestamp = 0;
    private double originTimestamp = 0;   //T1
    private double receiveTimestamp = 0;  //T2
    private double transmitTimestamp = 0; //T3

    byte[] receivedData;
    private double destinationTimestamp = 0; //T4
    private final static double ADD_SECONDS = 2208988800.0;

    public PacketNTP(int mode, int version, double transmitTimestamp) {
        this.mode = (byte) mode;
        this.versionNumber = (byte) version;
        this.transmitTimestamp = transmitTimestamp;
    }

    public PacketNTP(byte[] receivedData, double destinationTimestamp){
        this.receivedData = receivedData;
        this.destinationTimestamp = destinationTimestamp;
    }

    public byte[] packMessage(){
        byte[] bytes = new byte[48];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.put((byte) (leapIndicator << 6 | versionNumber << 3 | mode));
        byte[] transmitBytes = transmitTimestampToBytes(transmitTimestamp);
        for (int i = 40; i < 48; i++) {
            buffer.put(i, transmitBytes[i-40]);
        }
        return bytes;
    }

    public void unpackMessage(){
        leapIndicator = (byte) ((receivedData[0] >> 6) & 0x3);
        versionNumber = (byte) ((receivedData[0] >> 3) & 0x7);
        mode = (byte) (receivedData[0] & 0x7);
        stratum = receivedData[1];
        poll = receivedData[2];
        precision = receivedData[3];
        rootDelay = bytesToDouble(Arrays.copyOfRange(receivedData, 4, 8));
        rootDispersion = bytesToDouble(Arrays.copyOfRange(receivedData, 8, 12));
        referenceId = Arrays.copyOfRange(receivedData, 12, 16);
        referenceTimestamp = bytesToDouble(Arrays.copyOfRange(receivedData, 16, 24));
        originTimestamp = bytesToDouble(Arrays.copyOfRange(receivedData, 24, 32));
        receiveTimestamp = bytesToDouble(Arrays.copyOfRange(receivedData, 32, 40));
        transmitTimestamp = bytesToDouble(Arrays.copyOfRange(receivedData, 40, 48));
    }

    public static byte[] transmitTimestampToBytes(double timestamp) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            double base = Math.pow(2, (3 - i) * 8);
            bytes[i] = (byte) (timestamp / base);
            timestamp = timestamp - Byte.toUnsignedInt(bytes[i]) * base;
        }
        return bytes;
    }

    public double bytesToDouble(byte[] receivedBytes){
        double result = 0.0;
        int k = 3;
        if (receivedBytes.length == 4) k = 1;
        for (int i = 0; i < receivedBytes.length; i++) {
            result += Byte.toUnsignedInt(receivedBytes[i]) * Math.pow(2, (k - i) * 8);
        }
        return result;
    }

    public void printPacket(){
        System.out.printf("Вычисление смещения: %.6f\n", getOffset());
        System.out.printf("Вычисление задержки в обоих направлениях: %.6f\n\n",getRoundTripDelay());

        System.out.println("Индикатор коррекции: " + leapIndicator);
        System.out.println("Версия: " + versionNumber);
        System.out.println("Режим: " + mode);
        System.out.println("Слой: " + stratum);
        System.out.println("Интервал опроса: " + poll);
        System.out.println("Точность: " + precision);
        System.out.printf("Общая задержка: %.6f\n", rootDelay);
        System.out.printf("Дисперсия: %.6f\n", rootDispersion);
        System.out.println("Идентификатор источника: " + Byte.toUnsignedInt(referenceId[0]) + "."
                + Byte.toUnsignedInt(referenceId[1]) + "."
                + Byte.toUnsignedInt(referenceId[2]) + "."
                + Byte.toUnsignedInt(referenceId[3]));
        System.out.println("Время обновления: " + timestampToString(referenceTimestamp));
        System.out.println("Начальное время: " + timestampToString(originTimestamp));
        System.out.println("Время приёма: " + timestampToString(receiveTimestamp));
        System.out.println("Время отправки: " + timestampToString(transmitTimestamp));
    }

    public String timestampToString(double timestamp){
        double time = timestamp - ADD_SECONDS;
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss:SSS").format(new Date((long) (time * 1000)));
    }

    public double getOffset(){
        return 0.5*((receiveTimestamp-originTimestamp)+(transmitTimestamp-destinationTimestamp));
    }

    public double getRoundTripDelay(){
        return (destinationTimestamp-originTimestamp)-(transmitTimestamp-receiveTimestamp);
    }

}