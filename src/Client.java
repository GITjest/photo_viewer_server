import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.Logger;

public class Client extends Thread {
    private static final Logger LOG = Logger.getLogger(Client.class.getName());

    private final Socket socket;
    private final OutputStream out;
    private final InputStream in;
    private final BufferedReader br;
    private final PrintStream ps;

    public Client(Socket socket) throws IOException {
        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.br = new BufferedReader(new InputStreamReader(in));
        this.ps = new PrintStream(out);
        start();
    }

    public void run() {
        try {
            while (!isInterrupted()) {
                String requestType = br.readLine();
                LOG.info("#################################### - " + this.getName() + " - " + requestType + " - ####################################");
                switch (RequestType.valueOf(requestType)) {
                    case GET:
                        get(categoryFile(br.readLine()));
                        break;
                    case GET_IMAGE:
                        getImage(categoryFile(br.readLine()));
                        break;
                    case LOGIN:
                        login(br.readLine(), br.readLine());
                        break;
                    case CREATE:
                        create(categoryFile(br.readLine()));
                        break;
                    case DELETE:
                        delete(categoryFile(br.readLine()));
                        break;
                    case UPDATE:
                        update(categoryFile(br.readLine()), categoryFile(br.readLine()));
                        break;
                    case CREATE_ACCOUNT:
                        createAccount(categoryFile(br.readLine()), br.readLine());
                        break;
                    case DELETE_ACCOUNT:
                        deleteAccount(categoryFile(br.readLine()));
                        break;
                    case UPDATE_ACCOUNT:
                        updateAccount(categoryFile(br.readLine()), br.readLine(), br.readLine(), br.readLine());
                        break;
                }
            }
        } catch (Exception e) {
            LOG.warning(this.getName() + " | " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private File categoryFile(String category) {
        return new File("Categories/" + category);
    }

    private void error(String logMsg, Error error) {
        LOG.warning(logMsg);
        ps.println(false);
        ps.println(error);
    }

    private void get(File file) {
        if (file.exists() && file.isDirectory()) {
            ps.println(true);
            File[] listFiles = file.listFiles(File::isDirectory);
            if (listFiles != null) {
                LOG.info("Sending: " + file.getName() + " (" + listFiles.length + ")");
                LOG.info(Arrays.toString(listFiles));
                ps.println(listFiles.length);
                for (File directory : listFiles) {
                    ps.println(directory.getName());
                }
            } else {
                ps.println(0);
            }
        } else {
            error("File " + file.getName() + " not exist", Error.NOT_EXIST);
        }
    }

    private void getImage(File file) throws IOException, InterruptedException {
        if (file.exists()) {
            ps.println(true);
            if (file.isDirectory()) {
                File[] listFiles = file.listFiles(File::isFile);
                if (listFiles != null) {
                    LOG.info("Sending: " + file.getName() + " (" + listFiles.length + ")");
                    ps.println(listFiles.length);
                    ByteArrayOutputStream[] imagesBuffer = new ByteArrayOutputStream[listFiles.length];
                    for (int i = 0; i < listFiles.length; i++) {
                        ps.println(listFiles[i].getName());
                        imagesBuffer[i] = getByteArrayImage(listFiles[i]);
                        ps.println(imagesBuffer[i].size());
                        LOG.info(listFiles[i].getName() + " (" + imagesBuffer[i].size() + " bytes)");
                    }
                    for (int i = 0; i < listFiles.length; i++) {
                        out.write(imagesBuffer[i].toByteArray());
                    }
                } else {
                    ps.println(0);
                }
            } else {
                ByteArrayOutputStream baos = getByteArrayImage(file);
                ps.println(1);
                ps.println(file.getName());
                ps.println(baos.size());
                LOG.info(file.getName() + " (" + baos.size() + " bytes)");
                sleep(100);
                out.write(baos.toByteArray());
            }
        } else {
            error("File " + file.getName() + " not exist", Error.NOT_EXIST);
        }
    }

    private ByteArrayOutputStream getByteArrayImage(File file) throws IOException {
        String extension = file.getName().substring(file.getName().lastIndexOf('.') + 1);
        BufferedImage image = ImageIO.read(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, extension, baos);
        return baos;
    }

    private void login(String login, String password) {
        try {
            File userDirectory = categoryFile(login);
            if (checkPass(userDirectory, password)) {
                LOG.info(login + " login successful");
                get(userDirectory);
            } else {
                error("login filed", Error.NOT_EXIST);
            }
        } catch (IOException e) {
            error("login filed " + e.getMessage(), Error.NOT_EXIST);
        }
    }

    private Boolean checkPass(File userDirectory, String password) throws IOException {
        if (userDirectory.exists()) {
            FileReader fileReader = new FileReader(userDirectory.getPath() + "/Password.txt");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String pass = bufferedReader.readLine();
            bufferedReader.close();
            return password.equals(pass);
        }
        return false;
    }

    private void delete(File file) {
        if (file.exists()) {
            boolean isDirectory = file.isDirectory();
            if (isDirectory) {
                deleteFile(file);
            }
            if (file.delete()) {
                if (isDirectory) {
                    get(file.getParentFile());
                } else {
                    ps.println(true);
                    ps.println(1);
                    ps.println(file.getParentFile());
                }
                LOG.info("delete: " + file.getPath());
            } else error(file.getPath() + " delete failed", Error.NOT_REMOVED);
        } else error(file.getPath() + " not exist", Error.NOT_EXIST);
    }

    private void create(File file) {
        if (!file.exists()) {
            String extension = file.getName().substring(file.getName().lastIndexOf('.') + 1);
            if (!extension.equals("jpg")) {
                if (file.mkdir()) {
                    LOG.info(file.getPath() + " create");
                    get(file.getParentFile());
                } else error(file.getPath() + " not create", Error.NOT_CREATED);
            } else {
                File parent = file.getParentFile();
                if(!parent.exists()) {
                    parent.mkdir();
                }
                downloadImage(file, extension);
            }
        } else error(file.getPath() + " exist", Error.EXIST);
    }

    private void downloadImage(File file, String extension) {
        try {
            int size = Integer.parseInt(br.readLine());
            if (size > 0) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                for (int i = 0; i < size; i++) baos.write(in.read());
                BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
                ImageIO.write(bufferedImage, extension, file);
                baos.close();
            }
            LOG.info(file.getPath() + " (" + size + " bytes)");
            ps.println(true);
            ps.println(1);
            ps.println(file.getName());
        } catch (IOException e) {
            error("Something went wrong", Error.NOT_CREATED);
        }
    }

    private void update(File oldFile, File newFile) {
        try {
            if(oldFile.exists()) {
                Files.move(oldFile.toPath(), newFile.toPath());
                String extension = newFile.getName().substring(newFile.getName().lastIndexOf('.') + 1);
                if (!extension.equals("jpg")) {
                    LOG.info(oldFile.getPath() + " update");
                    get(newFile.getParentFile());
                } else {
                    downloadImage(newFile, extension);
                }
            } else {
                error(oldFile.getPath() + " not exist", Error.NOT_EXIST);
            }
        } catch (IOException e) {
            error("Something went wrong", Error.NOT_UPDATED);
        }
    }

    private void createAccount(File userDirectory, String pass) throws IOException {
        if (!userDirectory.exists()) {
            userDirectory.mkdir();
            File newCategory = new File(userDirectory.getPath() + "/nowa kategoria/");
            newCategory.mkdir();

            PrintStream password = new PrintStream(userDirectory.getPath() + "/Password.txt");
            password.println(pass);
            password.close();
            LOG.info(userDirectory.getName() + " create");
            ps.println(true);
            ps.println(1);
            ps.println(userDirectory.getName());
        } else {
            error(userDirectory.getName() + " exist", Error.EXIST);
        }
    }

    private void deleteAccount(File userDirectory) {
        deleteFile(userDirectory);
        if (userDirectory.delete()) {
            LOG.info(userDirectory.getName() + " remove");
            ps.println(true);
            ps.println(1);
            ps.println(userDirectory.getName());
        } else {
            error(userDirectory.getName() + " not removed", Error.NOT_REMOVED);
        }
    }

    private void updateAccount(File userDirectory, String newName, String oldPass, String newPass) throws IOException {
        if (checkPass(userDirectory, oldPass)) {
            Path newUserDirectory = Files.move(userDirectory.toPath(), userDirectory.toPath().resolveSibling(newName));
            if (!newPass.equals("")) {
                PrintStream password = new PrintStream(newUserDirectory + "/Password.txt");
                password.println(newPass);
                password.close();
            }
            LOG.info(userDirectory.getName() + " update");
            ps.println(true);
            ps.println(1);
            ps.println(newName);
        } else {
            error(userDirectory.getName() + " not updated", Error.NOT_UPDATED);
        }
    }

    private void deleteFile(File file) {
        for (File f : file.listFiles()) {
            if (f.isDirectory()) deleteFile(f);
            f.delete();
        }
    }

    private void closeConnection() {
        try {
            socket.close();
            in.close();
            out.close();
            br.close();
            ps.close();
            LOG.info(this.getName() + " | connection close");
            interrupt();
            Server.client.remove(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
