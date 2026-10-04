package Crawler;

import JSONReader.JSONReader;
import PageResponse.PageResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class Crawler {
    private final URI uri;
    private final HttpClient client;
    private final JSONReader jsonReader;

    public Crawler(String url) {
        uri = URI.create(url);
        client = HttpClient.newHttpClient();
        jsonReader = new JSONReader();
    }

    public PageResponse fetch(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri.resolve(path))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return jsonReader.read(response.body());
        } else {
            throw new IOException("Bad status: error code " + response.statusCode());
        }
    }

    public List<String> crawl(String path) throws InterruptedException, ExecutionException {
        Set<String> visited = new HashSet<>();
        List<String> messages = new ArrayList<>();

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CompletionService<PageResponse> completionService = new ExecutorCompletionService<>(executor);

        visited.add(path);
        completionService.submit(() -> fetch(path));
        int pending = 1;
        try {
            while (pending > 0) {
                Future<PageResponse> future = completionService.take();
                PageResponse response = future.get();
                pending--;
                messages.add(response.message());
                for (String s : response.successors()) {
                    if (visited.add(s)) {
                        completionService.submit(() -> fetch(s));
                        pending++;
                    }
                }
            }
        } finally {
            executor.close();
        }

        messages.sort(String::compareTo);
        return messages;
    }
}
