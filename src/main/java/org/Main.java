package org;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("🤖 Инициализация инфраструктуры NeyRonAgent 2.0...");

        ChatModelListener loggingAspect = createLoggingAspect();

        OllamaChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("qwen2.5-coder:14b")
                .temperature(0.2)
                .timeout(Duration.ofMinutes(60))
                .listeners(List.of(loggingAspect))
                .build();

        // 🛠️ НАСТРОЙКИ АГЕНТА
        Path projectRoot = Paths.get("/home/sleg/IdeaProjects/TimeSlot"); // Путь к Spring проекту
        Path appRoot = Paths.get(""); // Корень нашего текущего проекта для хранения отчетов

        // Поставьте `true`, если хотите проигнорировать прошлые файлы и начать СОВЕРШЕННО НОВЫЙ отчёт с таймстампом.
        // Поставьте `false`, чтобы он автоматически нашёл последний отчёт и продолжил его заполнять.
        boolean forceNewAudit = true;

        // Запуск
        AuditOrchestrator orchestrator = new AuditOrchestrator(projectRoot, appRoot, model, forceNewAudit);
        orchestrator.runAudit();
    }

    private static ChatModelListener createLoggingAspect() {
        return new ChatModelListener() {
            @Override
            public void onRequest(ChatModelRequestContext context) {
                log.info("🧠 [ИИ Аспект] Отправка тяжелого Java-контекста в локальную Ollama...");
            }

            @Override
            public void onResponse(ChatModelResponseContext context) {
                log.info("✍️ [ИИ Аспект] Анализ файла успешно завершен.");
            }

            @Override
            public void onError(ChatModelErrorContext context) {
                log.error("❌ [ИИ Аспект] Ошибка модели во время генерации", context.error());
            }
        };
    }
}