package explorableviz.transparenttext;

public record AuthoringAssistantResult(String response, int attempt, QueryContext query, long duration) {}
