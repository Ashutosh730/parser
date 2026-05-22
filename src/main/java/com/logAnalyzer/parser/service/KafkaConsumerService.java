//package com.logAnalyzer.parser.service;
//
//import com.logAnalyzer.parser.core.LogParserFactory;
//import com.logAnalyzer.parser.model.parsed.ParsedLog;
//import com.logAnalyzer.parser.model.raw.LogMessageModel;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class KafkaConsumerService {
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//    private final LogParserFactory logParserFactory;
//
//    @KafkaListener(
//            topics ="log-ingestion",
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void consume(List<String> messages) {
//        if (messages == null || messages.isEmpty()) {
//            log.warn("Received null or empty message batch");
//            return;
//        }
//
//        log.info("Received batch of {} messages", messages.size());
//        for (String msg : messages) {
//            try {
//                if (msg == null || msg.trim().isEmpty()) {
//                    log.warn("Skipping null or empty message");
//                    continue;
//                }
//
//                LogMessageModel logMessage = objectMapper.readValue(msg, LogMessageModel.class);
//                ParsedLog parsedLog = logParserFactory.parse(logMessage.getLogLine());
//                log.info("Parsed log: {}", parsedLog);
//            } catch (Exception e) {
//                log.error("Failed to parse message. Message content: {}", msg, e);
//            }
//        }
//    }
//}
