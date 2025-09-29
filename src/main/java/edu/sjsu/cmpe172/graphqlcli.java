package edu.sjsu.cmpe172;

import org.springframework.graphql.client.HttpGraphQlClient;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.File;
import java.util.Map;


@Command(name = "graphqlcli", version = "graphqlcli 1.0", mixinStandardHelpOptions = true, subcommands = {ListCoursers.class, ListAssignments.class})
public class graphqlcli implements Runnable {

    @Option(names = "--token", description = "API token from Canvas")

    private File tokenFile;
    private String token;


    @Override
    public void run() {
        System.out.println("Commands:");
        System.out.println(" list-assignments");
        System.out.println(" list-users");
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new graphqlcli()).execute(args);
        System.exit(exitCode);
    }

    public String getToken() {
        if (token == null && tokenFile != null) {
            try {
                token = java.nio.file.Files.readString(tokenFile.toPath()).trim();
            } catch (Exception e) {
                throw new RuntimeException("Could not read token file: " + tokenFile, e);
            }
        }
        return token;
    }

}

@Command(name = "list-courses", description = "List of the Courses in Canvas")
class ListCoursers implements Runnable {
    @Option(names = "--active/--no-active", description = "Check if the course is active or not")
    private boolean active;

    @ParentCommand
    private graphqlcli parent;

    @Override
    public void run() {
        try {
            String token = parent.getToken();
            HttpGraphQlClient client = CanvasHttp.create(token);
            String query = """
                    query MyQuery {
                      course(id: "Q291cnNlLTE0NjkwODY")  {
                        id
                        name
                      }
                    }
                    """;
            var course = client.document(query)
                    .retrieve("course")
                    .toEntity(Map.class)
                    .block();

            if (course != null) {
                System.out.println("Course: " + course.get("id") + " - " + course.get("name"));
            } else {
                System.out.println("No course found.");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}

@Command(name = "list-assignments", description = "List of the assignments in Canvas")
class ListAssignments implements Runnable {

    @Option(names = "--active/--no-active", description = "Check if it is active or not")
    private boolean active;

    @ParentCommand
    private graphqlcli parent;

    @Override
    public void run() {
        try {
            String token = parent.getToken();
            HttpGraphQlClient client = CanvasHttp.create(token);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}

class CanvasHttp {
    public static HttpGraphQlClient create(String token) {
        return HttpGraphQlClient.builder()
                .url("https://sjsu.instructure.com/api/graphql")
                .header("Authorization", "Bearer " + token)
                .build();
    }
}