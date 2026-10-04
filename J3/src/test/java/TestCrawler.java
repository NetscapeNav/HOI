import Crawler.Crawler;
import PageResponse.PageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestCrawler extends Crawler {
    private Map<String, PageResponse> pages;
    private final Map<String, Integer> calls = new ConcurrentHashMap<>();
    private CountDownLatch started;

    TestCrawler() {
        super("http://localhost/");
    }

    @Override
    public PageResponse fetch(String path) throws IOException, InterruptedException {
        calls.merge(path, 1, Integer::sum);
        PageResponse page = pages.get(path);
        if (page == null) {
            throw new IOException("No page: " + path);
        }
        if (started != null && !path.equals("/")) {
            started.countDown();
            if (!started.await(2, TimeUnit.SECONDS)) {
                throw new IOException("Requests ran one after another");
            }
        }
        return page;
    }

    private PageResponse page(String message, String... successors) {
        return new PageResponse(message, List.of(successors));
    }

    @Test
    @Timeout(5)
    void cyclesAndDuplicates() throws Exception {
        pages = Map.of(
                "/", page("root", "a", "b", "a"),
                "a", page("zulu", "b", "/"),
                "b", page("alpha", "a", "c"),
                "c", page("alpha")
        );

        assertEquals(List.of("alpha", "alpha", "root", "zulu"), crawl("/"));
        assertEquals(Map.of("/", 1, "a", 1, "b", 1, "c", 1), calls);
    }

    @Test
    @Timeout(5)
    void waitsForAllLevels() throws Exception {
        pages = Map.of(
                "/", page("zero", "a"),
                "a", page("one", "b"),
                "b", page("two", "c"),
                "c", page("three")
        );

        assertEquals(List.of("one", "three", "two", "zero"), crawl("/"));
    }

    @Test
    @Timeout(5)
    void runsInParallel() throws Exception {
        pages = Map.of(
                "/", page("root", "a", "b"),
                "a", page("a"),
                "b", page("b")
        );
        started = new CountDownLatch(2);

        assertEquals(List.of("a", "b", "root"), crawl("/"));
    }
}
