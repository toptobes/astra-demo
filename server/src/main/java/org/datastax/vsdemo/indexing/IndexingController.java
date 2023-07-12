package org.datastax.vsdemo.indexing;

import org.datastax.vsdemo.indexing.messages.IndexRequest;
import org.datastax.vsdemo.indexing.messages.SimilarityRequest;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class IndexingController {
    private final IndexingService service;
    private final SimpMessagingTemplate simp;

    public IndexingController(IndexingService service, SimpMessagingTemplate simp) {
        this.service = service;
        this.simp = simp;
    }

    @MessageMapping("/index")
    public void indexSentences(
        @Payload List<IndexRequest> request,
        @Header("simpSessionId") String userID
    ) {
        service.indexSentences(userID, request);
    }

    @MessageMapping("/query")
    public void querySentences(
        @Payload SimilarityRequest message,
        @Header("simpSessionId") String userID
    ) {
        service.getSimilarSentences(userID, message.query(), message.limit())
            .whenComplete((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                } else {
                    simp.convertAndSend("/topic/query-result", result);
                }
            });
    }
}
