package org;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tools.ProjectTools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;

public class AuditOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(AuditOrchestrator.class);
    private static final String BASE_REPORT_NAME = "autonomous_spring_audit";

    private Path reportPath;
    private final Path appRoot;
    private final ProjectTools projectTools;
    private final SpringCodeReviewer reviewer;

    public AuditOrchestrator(Path projectRoot, Path appRoot, OllamaChatModel chatModel, boolean forceNewAudit) {
        this.appRoot = appRoot;
        this.projectTools = new ProjectTools(projectRoot);
        this.reviewer = AiServices.builder(SpringCodeReviewer.class)
                .chatLanguageModel(chatModel)
                .build();

        resolveReportPath(forceNewAudit);
    }

    /**
     * Определяет путь к файлу отчета: находит последний измененный или создает новый с таймстампом.
     */
    private void resolveReportPath(boolean forceNewAudit) {
        if (!forceNewAudit) {
            // Ищем самый свежий существующий отчет в папке нашего NeyRonAgent
            try (Stream<Path> walk = Files.walk(appRoot, 1)) {
                this.reportPath = walk
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().startsWith(BASE_REPORT_NAME) && p.toString().endsWith(".md"))
                        .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                        .orElse(null);
            } catch (IOException e) {
                log.warn("Не удалось просканировать существующие отчёты: {}", e.getMessage());
            }
        }

        // Если принудительно новый запуск или старых отчетов вообще не найдено — генерируем новое имя с префиксом времени
        if (this.reportPath == null) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = String.format("%s_%s.md", BASE_REPORT_NAME, timestamp);
            this.reportPath = appRoot.resolve(fileName);
            log.info("🆕 Создается новый уникальный файл отчёта: {}", fileName);
        } else {
            log.info("📋 Найден последний сохранённый отчёт: {}. Возобновляем аудит в нём.", reportPath.getFileName());
        }
    }

    public void runAudit() {
        log.info("🚀 Запуск исследования проекта...");
        String existingReport = initializeOrReadReport();

        // Шаг 1. Конфигурация сборки
        processBuildConfiguration(existingReport);

        // Шаг 2 & 3. Сканирование и обход файлов
        String allFilesText = projectTools.listAllJavaFiles();
        String[] filePaths = allFilesText.split("\n");
        log.info("📁 Всего в проекте обнаружено файлов: {}", filePaths.length);

        processJavaFiles(filePaths, existingReport);

        log.info("✅ Автономный аудит успешно завершен! Итоговый отчёт: {}", reportPath.toAbsolutePath());
    }

    private String initializeOrReadReport() {
        if (Files.exists(reportPath) && reportPath.toFile().length() > 0) {
            try {
                return Files.readString(reportPath);
            } catch (IOException e) {
                log.error("Ошибка чтения существующего отчета", e);
                return "";
            }
        }

        try {
            Files.writeString(reportPath, "# 📋 Автономный ИИ-Аудит проекта (ООП/АОП/Clean Code)\n\n",
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("Не удалось создать файл отчета", e);
        }
        return "";
    }

    private void processBuildConfiguration(String existingReport) {
        if (!existingReport.contains("## ⚙️ Анализ конфигурации сборки")) {
            log.info("🛠️ [Шаг 1] Чтение конфигурации сборки...");
            String buildConfig = projectTools.readBuildConfiguration();
            String buildReview = reviewer.analyzeFileContent(buildConfig, "build.gradle / pom.xml");
            appendSection("## ⚙️ Анализ конфигурации сборки и зависимостей\n\n" + buildReview + "\n\n---\n\n");
        } else {
            log.info("⏭️ [Шаг 1] Анализ сборки уже есть в отчете, пропускаем.");
        }
    }

    private void processJavaFiles(String[] filePaths, String existingReport) {
        int processedCount = 0;
        int skippedCount = 0;

        for (String relativePath : filePaths) {
            if (relativePath.trim().isEmpty() || relativePath.contains("test")) {
                continue;
            }

            Path p = reportPath.getFileSystem().getPath(relativePath);
            String fileName = p.getFileName().toString();

            if (existingReport.contains("## 📄 Компонент: " + fileName)) {
                skippedCount++;
                continue;
            }

            log.info("⚡ Анализирую бизнес-логику: {}", relativePath);
            processedCount++;

            try {
                String fileContent = projectTools.readJavaFile(relativePath);
                String fileReview = reviewer.analyzeFileContent(fileContent, fileName);

                String section = "## 📄 Компонент: " + fileName + "\n" +
                        "> **Путь в проекте:** `" + relativePath + "`\n\n" +
                        fileReview + "\n\n---\n\n";

                appendSection(section);
                log.info("💾 Результат для {} сохранен на диск.", fileName);
            } catch (Exception e) {
                log.warn("⚠ Ошибка анализа файла {}: {}", relativePath, e.getMessage());
            }
        }
        log.info("ℹ️ Успешно пропущено: {} файлов. Обработано в текущей сессии: {} файлов.", skippedCount, processedCount);
    }

    private void appendSection(String content) {
        try {
            Files.writeString(reportPath, content, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("❌ Ошибка записи на диск: {}", e.getMessage());
        }
    }
}
