package edu.sjsu.cmpe172;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;


@Command(name = "graphqlcli", version = "ASCIIArt 1.0", mixinStandardHelpOptions = true)
public class graphqlcli implements Runnable {

    @Option(names = "--token", description = "API token from Canvas")

    private File tokenFile;
    private String token;

    @Parameters(paramLabel = "<word>", defaultValue = "Hello, picocli",
            description = "Words to be translated into ASCII art.")

    @Override
    public void run() {
        // The business logic of the command goes here...
        // In this case, code for generation of ASCII art graphics
        // (omitted for the sake of brevity).
    }
    public static void main(String[] args) {
        int exitCode = new CommandLine(new graphqlcli()).execute(args);
        System.exit(exitCode);
    }

}

class ListCoursers implements Runnable {
    @Override
    public void run() {
    }
}

class ListAssignments implements Runnable {
    @Override
    public void run() {}
}

