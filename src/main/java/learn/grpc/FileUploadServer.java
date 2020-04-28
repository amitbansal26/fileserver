package learn.grpc;

import com.amit.upload.example.*;
import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.*;
import java.util.UUID;
import java.util.logging.*;

public class FileUploadServer {
    private final int port;
    private final Server server;
    private static Logger logger;

    private static final int CHUNKSIZE = 1;

    public FileUploadServer(int port) throws IOException {
        logger = Logger.getLogger("logger.info");
        logger.setLevel(Level.INFO);

        File logFile = new File("src/main/log/server.log");
        FileHandler fileHandler = new FileHandler(logFile.getAbsolutePath(), 10240, 1, true);
        fileHandler.setLevel(Level.INFO);
        fileHandler.setFormatter(new MyLogFormatter());
        logger.addHandler(fileHandler);

        this.port = port;
        ServerBuilder sb = ServerBuilder.forPort(port);
        this.server = sb.addService(new FileUploadServie()).addService(new FileDownloadService()).build();
    }

    public void start() throws IOException {
        logger.info("************ START *************");

        server.start();
        System.out.println("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                FileUploadServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    /**
     * Stop serving requests and shutdown resources.
     */
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
        logger.info("************ FINISH ************");
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main method.  This comment makes the linter happy.
     */
    public static void main(String[] args) throws Exception {


        FileUploadServer server = new FileUploadServer(8980);
        server.start();
        server.blockUntilShutdown();

    }




public static class  MyLogFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        return record.getMillis() + ": " + record.getMessage() + "\n";
    }
}
        public static class FileUploadServie extends UploadServiceGrpc.UploadServiceImplBase {
            @Override
            public StreamObserver<Chunk> upload(StreamObserver<UploadStatus> responseObserver) {
                 return new StreamObserver<Chunk>() {
                    String filename = UUID.randomUUID().toString();
                    int count = 0;
                    @Override
                    public void onNext(Chunk chunk) {
                        count++;
                        logger.info("chunk-" + String.valueOf(count) + " start");
                         BufferedOutputStream bos = null;
                        FileOutputStream fos = null;
                        File file = null;
                        try {
                            file = new File("src/main/resources/" + filename);
                            fos = new FileOutputStream(file, true);
                            bos = new BufferedOutputStream(fos);
                            bos.write(chunk.getContent().toByteArray());
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if (bos != null) {
                                try {
                                    bos.close();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }
                            if (fos != null) {
                                try {
                                    fos.close();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }
                        logger.info("chunk-" + String.valueOf(count) + " finish");
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("error!!!!");
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("complete!!!!!");
                        logger.info("File Transfer Completed\n");
                        responseObserver.onNext(UploadStatus.newBuilder().setCodeValue(1).build());
                        responseObserver.onCompleted();
                    }
                };


            }
        }


    public static class FileDownloadService extends DownloadServiceGrpc.DownloadServiceImplBase {

        @Override
        public void downloadFile(DownloadFileRequest request, StreamObserver<Chunk> responseObserver) {
            try {
                String fileName = "src/main/resources/" + request.getFileName();
                File file = new File("src/main/resources/" + request.getFileName());
                FileInputStream inputStream = new FileInputStream(file);
                // read the file and convert to a byte array

                byte[] bytes = inputStream.readAllBytes();
                BufferedInputStream imageStream = new BufferedInputStream(new ByteArrayInputStream(bytes));

                int bufferSize = 1 * 1024;// 1K
                byte[] buffer = new byte[bufferSize];
                int length;
                while ((length = imageStream.read(buffer, 0, bufferSize)) != -1) {
                    responseObserver.onNext(Chunk.newBuilder()
                            .setContent(ByteString.copyFrom(buffer, 0, length))
                             .build());
                }
                imageStream.close();
                responseObserver.onCompleted();
            } catch (Throwable e) {
                responseObserver.onError(Status.ABORTED
                        .withDescription("Unable to acquire the image " + request.getFileName())
                        .withCause(e)
                        .asException());
            }
        }
    }

}
