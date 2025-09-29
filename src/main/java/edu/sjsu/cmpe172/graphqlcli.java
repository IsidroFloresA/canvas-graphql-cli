package edu.sjsu.cmpe172;

import org.springframework.graphql.client.HttpGraphQlClient;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;


@Command(name = "graphqlcli", version = "graphqlcli 1.0", mixinStandardHelpOptions = true, subcommands = {ListCoursers.class, ListAssignments.class})
public class graphqlcli implements Runnable {

    @Option(names = "--token", description = "API token from Canvas")

    private File tokenFile;
    private String token;


    @Override
    public void run() {
        System.out.print("Missing required option: '--token=<token>' \n" +
                "Usage: <main class> --token=<token> [COMMAND] \n" +
                "      --token=<token>   API authorization token from \n");
        System.out.print("Commands: \n" +
                " list-assignments \n" +
                " list-courses");
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
    @Option(names = {"--active"}, negatable = true, description = "Check if the course is active or not")
    private Boolean active;

    @ParentCommand
    private graphqlcli parent;

    @Override
    public void run() {
        try {
            String token = parent.getToken();
            HttpGraphQlClient client = CanvasHttp.create(token);
            String query = """
                    query MyQuery {
                      allCourses {
                        name
                        updatedAt
                      }
                    }
                    """;
            var courses = client.document(query)
                    .retrieve("allCourses")
                    .toEntityList(Map.class)
                    .block();

            if (courses == null || courses.isEmpty()) {
                System.out.println("No courses found.");
            }
            LocalDateTime cutoff = LocalDateTime.parse("2025-08-01T00:00:00");

            DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

            for (Object obj : courses) {
                Map course = (Map) obj;
                String updatedAtStr = (String) course.get("updatedAt");

                LocalDateTime updatedAt = LocalDateTime.parse(updatedAtStr, formatter);

                if (active == null) {
                    System.out.println(course.get("name"));
                } else if (active && updatedAt.isAfter(cutoff)) {
                    System.out.println(course.get("name")
                    );
                } else if(!active && updatedAt.isBefore(cutoff)) {
                    System.out.println(course.get("name"));
                }
            }

        } catch (
                Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}

@Command(name = "list-assignments", description = "List of the assignments in Canvas")
class ListAssignments implements Runnable {

    @Option(names = {"--active"}, negatable = true, description = "Check if it is active or not")
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
            """;
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