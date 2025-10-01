package edu.sjsu.cmpe172;

import org.springframework.graphql.client.HttpGraphQlClient;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import javax.xml.namespace.QName;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
                "      --token=<token>   API authorization token from canvas\n");
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
    @Option(names = "--no-active", negatable = true, description = "Check if the course is active or not", defaultValue = "true")
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
                        term {
                          name
                        }
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

            for (Object obj : courses) {
                Map course = (Map) obj;
                String courseName = (String) course.get("name");

                Map termObj = (Map) course.get("term");
                String termName = (String) termObj.get("name");

                if (active && termName.equals(("Fall 2025"))) {
                    System.out.println(courseName);
                } else if (!active) {
                    System.out.println(courseName);
                }
            }

        } catch (
                Exception e) {
            System.out.println(e.getMessage());
        }
    }
}

@Command(name = "list-assignments", description = "List of the assignments in Canvas")
class ListAssignments implements Runnable {
    @Parameters
    private String course;

    @Option(names = "--no-active", negatable = true, description = "Check if it is active or not", defaultValue = "true")
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
                      allCourses {
                        _id
                        name
                        term {
                          name
                        }
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
            List<Map> match = new ArrayList<>();

            for (Object obj : courses) {
                Map courseMap = (Map) obj;
                String courseName = (String) courseMap.get("name");

                Map termObj = (Map) courseMap.get("term");
                String termName = (String) termObj.get("name");

                if (courseName.toLowerCase().contains(course.toLowerCase())) {
                    if (active && "Fall 2025".equals(termName)) {
                        match.add(courseMap);
                    } else if (!active) {
                        match.add(courseMap);
                    }
                }
            }
            if (match.isEmpty()) {
                System.out.println("No matching courses found for: " + course);
                return;
            }
            if (match.size() > 1) {
                System.out.println("Multiple matches found, please be more specific:");
                for (Map m : match) {
                    System.out.println(m.get("name"));
                }
                return;
            }

            Map selectedCourse = (Map) match.get(0);
            String courseId = (String) selectedCourse.get("_id");

            String secondQuery = """
                    query courseInfo ($courseId: ID!) {
                      course(id: $courseId) {
                        assignmentsConnection {
                          nodes {
                            name
                            dueAt
                            lockInfo {
                              isLocked
                            }
                          }
                        }
                      }
                    }
                    """;

            Map<String, Object> variables = Map.of("courseId", courseId);

            var courseData = client.document(secondQuery)
                    .variables(variables)
                    .retrieve("course")
                    .toEntity(Map.class)
                    .block();

            Map assignmentsConn = (Map) courseData.get("assignmentsConnection");
            List<Map> assignments = (List<Map>) assignmentsConn.get("nodes");

            for (Map assignment : assignments) {
                String assignmentName = (String) assignment.get("name");
                String dueAt = (String) assignment.get("dueAt");

                Map lockInfo = (Map) assignment.get("lockInfo");
                boolean isLocked = lockInfo != null && Boolean.TRUE.equals(lockInfo.get("isLocked"));

                if (active) {
                    if (!isLocked) {
                        System.out.println(assignmentName + " due at " + dueAt);
                    }
                } else {
                    System.out.println(assignmentName + " due at " + dueAt);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
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