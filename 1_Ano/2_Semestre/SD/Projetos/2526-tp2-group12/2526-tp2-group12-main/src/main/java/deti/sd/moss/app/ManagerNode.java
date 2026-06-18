package deti.sd.moss.app;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Option;

import io.grpc.ServerBuilder;

import deti.sd.moss.infra.rpc.discovery.GrpcServiceDiscovery;
import deti.sd.moss.infra.rpc.manager.ManagerGrpcService;

import deti.sd.moss.core.manager.ManagerServiceProviderImpl;

import deti.sd.moss.util.NetworkUtils;

public class ManagerNode implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ManagerNode.class);

    @Option(names = {"-p", "--port"}, description = "Port number", defaultValue = "4081")
    private int port;

    @Option(names = {"-m", "--mdir"}, description = "metadata dir", defaultValue = "data")
    private String mdir;

    @Override
    public void run() {

        var discovery = new GrpcServiceDiscovery();
        var provider = new ManagerServiceProviderImpl(mdir, discovery);
        var entrypoint = new ManagerGrpcService(provider);
        var server = ServerBuilder
            .forPort(port)
            .addService(entrypoint)
            .build();

        try{
            server.start();
            logger.info("Node started on {}:{}", NetworkUtils.getRealIPv4(), port);

            Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
            server.awaitTermination();

        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Server interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException  {
        int exitCode = new CommandLine(new ManagerNode()).execute(args);
        System.exit(exitCode);
    }
}
