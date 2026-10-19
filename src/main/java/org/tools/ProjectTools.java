package org.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectTools {
    private static final Logger log = LoggerFactory.getLogger(ProjectTools.class);
    private final Path projectRoot;

    public ProjectTools(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    @Tool("Читает конфигурацию сборки (build.gradle или pom.xml), чтобы узнать зависимости и стек проекта.")
    public String readBuildConfiguration() {
        try {
            Path gradleFile = projectRoot.resolve("build.gradle");
            if (Files.exists(gradleFile)) return Files.readString(gradleFile);
            Path mavenFile = projectRoot.resolve("pom.xml");
            if (Files.exists(mavenFile)) return Files.readString(mavenFile);
            return "Файлы сборки (Gradle/Maven) не найдены.";
        } catch (IOException e) {
            return "Ошибка чтения файлов сборки: " + e.getMessage();
        }
    }

    @Tool("Возвращает список всех Java-файлов в проекте (относительные пути).")
    public String listAllJavaFiles() {
        Path javaSrc = projectRoot.resolve("src/main/java");
        if (!Files.exists(javaSrc)) return "Папка src/main/java не найдена.";

        try (Stream<Path> paths = Files.walk(javaSrc)) {
            List<String> files = paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(projectRoot::relativize)
                    .map(Path::toString)
                    .collect(Collectors.toList());
            return files.isEmpty() ? "Java файлы не найдены." : String.join("\n", files);
        } catch (IOException e) {
            return "Ошибка сканирования директории: " + e.getMessage();
        }
    }

    @Tool("Читает содержимое конкретного Java-файла по его относительному пути (например, 'src/main/java/org/example/Controller.java').")
    public String readJavaFile(@P("Относительный путь к файлу от корня проекта") String relativePath) {
        try {
            Path filePath = projectRoot.resolve(relativePath);
            log.info("📂 [Tool] Чтение файла: {}", filePath.getFileName());
            return Files.readString(filePath);
        } catch (IOException e) {
            return "Не удалось прочитать файл " + relativePath + ": " + e.getMessage();
        }
    }
}
