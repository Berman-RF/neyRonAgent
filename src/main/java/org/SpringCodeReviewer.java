package org;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SpringCodeReviewer {
    @SystemMessage("""
        Ты — элитный Java-архитектор и эксперт по Clean Code. Твоя задача — провести жесткий аудит кода Spring Boot.
        Проверяй код по критериям:
        1. ООП и Паттерны (SOLID, инкапсуляция, отсутствие God-классов).
        2. АОП (Сквозная логика, которую нужно вынести в аспекты).
        3. Чистый код (читаемость, именование, дублирование).
        4. Фичи Java 21 (Record, Pattern Matching, Virtual Threads) и проблемы N+1 в JPA.
        Отвечай строго по делу на русском языке в формате Markdown.
        """)
    String analyzeFileContent(@UserMessage String code, @V("fileName") String fileName);
}
