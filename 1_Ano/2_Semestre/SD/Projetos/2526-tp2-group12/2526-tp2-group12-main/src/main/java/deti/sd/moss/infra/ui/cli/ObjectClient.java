package deti.sd.moss.infra.ui.cli;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.Callable;


import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import deti.sd.moss.infra.rpc.discovery.GrpcServiceDiscovery;
import deti.sd.moss.core.object.model.GetRequest;
import deti.sd.moss.core.object.model.PutRequest;
import deti.sd.moss.core.object.model.ListRequest;
import deti.sd.moss.core.object.model.ListReply;
import deti.sd.moss.core.object.model.RemoveRequest;
import java.time.Instant;

@Command(name = "put")
class PutCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private ObjectClient parent;

    @Option(names = "-b", defaultValue="sd") String bucket;
    @CommandLine.Parameters(index = "0") File file;
    @CommandLine.Parameters(index = "1") String path;

    @Override
    public Integer call() throws IOException {

        // Validate user input
        if (!file.exists()){
            System.err.printf("Error: File '%s' not found.%n", file.toPath());
            return 1; // Return non-zero exit code for errors
        }

        if (file.isDirectory()){
            System.err.printf("Error: '%s' is a directory, not a file.%n", file.toPath());
            return 1; // Return non-zero exit code for errors
        }

        if (file.length() > 4 * 1024 * 1024){
            System.err.printf("Error: File '%s is too large.%n", file.toPath());
            return 1; // Return non-zero exit code for errors
        }
        //--------------------

        var discovery = new GrpcServiceDiscovery();
        var objects = discovery.getObject(parent.remote);

        var response = objects.put(new PutRequest(
            bucket,
            path,
            Files.readAllBytes(file.toPath())
        ));

        if (response.status() != 0){
            System.err.println("Failed to put object");
            return 1;
        }

        return 0;
    }

}

@Command(name = "get")
class GetCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private ObjectClient parent;

    @Option(names = "-b", defaultValue="sd") String bucket;
    @CommandLine.Parameters(index = "0") String path;
    @CommandLine.Parameters(index = "1") File file;

    @Override
    public Integer call() throws IOException {
        var discovery = new GrpcServiceDiscovery();
        var objects = discovery.getObject(parent.remote);

        var response = objects.get(new GetRequest(bucket, path));

        if (response.status() != 0) {
            System.err.println("Failed to get object");
            return 1;
        }

        Files.write(file.toPath(), response.data());
        System.out.printf("Downloaded object to '%s' (%d bytes).%n", file.toPath(), response.data().length);
        return 0;
    }
}


@Command(name = "moss",
         mixinStandardHelpOptions = true,
         version = "SD.26",
         description = "MOSS client",
         subcommands = { PutCommand.class, GetCommand.class, ListCommand.class, RemoveCommand.class })
public class ObjectClient implements Callable<Integer> {

    //==========================================================================
    @Option(
        names = {"-r", "--remote"},
        description = "Remote object server",
        defaultValue = "localhost:4281")
    public String remote;

    @Override
    public Integer call() throws Exception {
        // This runs if no subcommand is provided
        CommandLine.usage(this, System.out);
        return 0;
    }

}

@Command(name = "list")
class ListCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private ObjectClient parent;

    @Option(names = "-b", defaultValue = "sd") String bucket;

    @Override
    public Integer call() throws Exception {
        var discovery = new GrpcServiceDiscovery();
        var objects = discovery.getObject(parent.remote);

        ListReply reply = objects.list(new ListRequest(bucket));

        if (reply.status() != 0) {
            System.err.println("Failed to list objects (non-zero status)");
            return 1;
        }

        var list = reply.objects();
        if (list == null || list.isEmpty()) {
            System.out.println("No objects found in bucket '" + bucket + "'.");
            return 0;
        }

        System.out.printf("%-40s %10s %25s%n", "PATH", "SIZE", "TIMESTAMP");
        for (var obj : list) {
            String path = obj.path();
            int size = obj.size();
            long ts = obj.timestamp();
            String time = Instant.ofEpochMilli(ts).toString();
            System.out.printf("%-40s %10d %25s%n", path, size, time);
        }

        return 0;
    }

}

@Command(name = "rm")
class RemoveCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private ObjectClient parent;

    @Option(names = "-b", defaultValue = "sd") String bucket;
    @CommandLine.Parameters(index = "0") String path;

    @Override
    public Integer call() throws Exception {
        var discovery = new GrpcServiceDiscovery();
        var objects = discovery.getObject(parent.remote);

        var reply = objects.remove(new RemoveRequest(bucket, path));

        if (reply.status() != 0) {
            System.err.println("Failed to remove object '" + path + "'.");
            return 1;
        }

        System.out.println("Removed '" + path + "' from bucket '" + bucket + "'.");
        return 0;
    }

}
