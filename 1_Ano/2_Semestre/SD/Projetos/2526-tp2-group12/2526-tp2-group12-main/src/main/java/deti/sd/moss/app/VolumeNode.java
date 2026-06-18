package deti.sd.moss.app;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Option;

import io.grpc.ServerBuilder;

import deti.sd.moss.infra.rpc.discovery.GrpcServiceDiscovery;
import deti.sd.moss.infra.rpc.volume.VolumeGrpcService;

import deti.sd.moss.core.volume.VolumeServiceProviderImpl;

import deti.sd.moss.util.NetworkUtils;

public class VolumeNode implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(VolumeNode.class);

    @Option(names = {"-p", "--port"}, description = "Port number", defaultValue = "4181")
    private int port;

    @Option(names = {"-d", "--dir"}, description = "Data directory", required = true)
    private String dir;

    @Option(names = {"-m", "--manager"}, description = "Manager url", defaultValue = "localhost:4081")
    private String managerUrl;

    @Override
    public void run() {
        var discovery = new GrpcServiceDiscovery();
        var provider = new VolumeServiceProviderImpl(port, managerUrl, dir, discovery);
        var entrypoint = new VolumeGrpcService(provider);
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
        int exitCode = new CommandLine(new VolumeNode()).execute(args);
        System.exit(exitCode);
    }
}
