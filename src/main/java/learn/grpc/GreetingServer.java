package learn.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class GreetingServer {

    public static void main(String[] args) throws IOException , InterruptedException{
        System.out.println("Hello GRPC");
        Server server = ServerBuilder.forPort(50051).build();
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            System.out.println("Rcieved Shutdown Request");
            server.shutdown();
            System.out.println("Stopped Server");
        }));
        server.awaitTermination();
    }
}
