import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientNTP {
    private static final String SERVER_NAME = "pool.ntp.org";
    private static final int SERVER_PORT = 123;
    private final static double ADD_SECONDS = 2208988800.0;

    public static void main(String[] args) {
        try {
            System.out.println("Отправляем пакет на NTP сервер");
            DatagramSocket datagramSocket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(SERVER_NAME);

            long sendTime = System.currentTimeMillis();
            String sendTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss:SSS\n").format(new Date(sendTime));
            System.out.println("Время отправки на сервер: " + sendTimeFormat);
            PacketNTP sendPacket = new PacketNTP(3,4, sendTime/1000.0+ADD_SECONDS);

            byte[] buffer = sendPacket.packMessage();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, SERVER_PORT);
            datagramSocket.send(packet);

            System.out.println("Полученный ответ");
            packet = new DatagramPacket(buffer, buffer.length);
            datagramSocket.receive(packet);

            long destinationTimestamp = System.currentTimeMillis();
            String receiveTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss:SSS").format(new Date(destinationTimestamp));
            System.out.println("Время получения: " + receiveTime + "\n");

            PacketNTP receivePacket = new PacketNTP(packet.getData(),destinationTimestamp/1000.0+ADD_SECONDS);
            receivePacket.unpackMessage();
            receivePacket.printPacket();

            datagramSocket.close();
        } catch (Exception e) {
            System.out.println("Ошибка работы NTP клиента");
        }
    }
}