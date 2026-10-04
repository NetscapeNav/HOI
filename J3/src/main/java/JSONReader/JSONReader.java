package JSONReader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import PageResponse.PageResponse;

public class JSONReader {
    private final ObjectMapper mapper = new ObjectMapper();

    public PageResponse read(String value) throws JsonProcessingException {
        return mapper.readValue(value, PageResponse.class);
    }
}
