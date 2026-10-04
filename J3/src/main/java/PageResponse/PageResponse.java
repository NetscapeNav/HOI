package PageResponse;

import java.util.List;

public record PageResponse(
        String message,
        List<String> successors
) {
}