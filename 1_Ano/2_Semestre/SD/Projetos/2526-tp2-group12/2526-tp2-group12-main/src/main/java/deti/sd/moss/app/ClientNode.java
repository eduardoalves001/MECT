package deti.sd.moss.app;

import java.io.IOException;

import picocli.CommandLine;

import deti.sd.moss.infra.ui.cli.ObjectClient;

public class ClientNode {
    public static void main(String[] args) throws IOException, InterruptedException  {
        int exitCode = new CommandLine(new ObjectClient()).execute(args);
        System.exit(exitCode);
    }
}
