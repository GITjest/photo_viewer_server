import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Server {
    private static final Logger LOG = Logger.getLogger(Server.class.getName());
    public static List<Client> client = new ArrayList<>();

    void runServer() throws IOException {
        ServerSocket server = new ServerSocket(8000);
        LOG.info("Server run...");
        while (true) {
            Socket socket = server.accept();
            LOG.info("Client accepted...");
            Client thread = new Client(socket);
            client.add(thread);
            thread.setName("Client - " + client.indexOf(thread));
        }
    }

    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");
        try {
            new Server().runServer();
        } catch (IOException e) {
            LOG.warning(e.getMessage());
        }
    }
}
