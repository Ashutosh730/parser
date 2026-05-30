//package com.logAnalyzer.parser.service;
//
//import com.logAnalyzer.parser.core.LogParser;
//import com.logAnalyzer.parser.core.LogParserFactory;
//import com.logAnalyzer.parser.model.parsed.ParsedLog;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.io.TempDir;
//import org.mockito.ArgumentCaptor;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.ArgumentMatchers.anyList;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//class LogPipelineServiceTest {
//
//	@TempDir
//	Path tempDir;
//
//	@Test
//	void processFile_shouldKeepStackTraceLinesInSameChunk() throws IOException {
//		LogParserFactory logParserFactory = mock(LogParserFactory.class);
//		LogParser parser = mock(LogParser.class);
//		when(logParserFactory.getParser(anyList())).thenReturn(parser);
//
//		when(parser.isPrimaryLine(anyString())).thenAnswer(invocation -> {
//			String line = invocation.getArgument(0, String.class);
//			return line.startsWith("2026-");
//		});
//		when(parser.parse(anyString())).thenReturn(ParsedLog.builder().rawLog("ok").build());
//
//		Path logFile = tempDir.resolve("spring-stack.log");
//		List<String> lines = List.of(
//				"2026-05-28T09:48:35.877Z  WARN 12044 --- [parser] [           main] org.hibernate.orm.jdbc.error             : HHH100046: Could not obtain connection",
//				"\tat org.hibernate.engine.jdbc.spi.SqlExceptionHelper.convert(SqlExceptionHelper.java:115)",
//				"Caused by: java.net.ConnectException: Connection refused",
//				"\t... 37 common frames omitted",
//				"2026-05-28T09:48:38.421Z ERROR 12044 --- [parser] [           main] o.s.boot.SpringApplication               : Application run failed",
//				"org.springframework.beans.factory.BeanCreationException: Error creating bean"
//		);
//		Files.write(logFile, lines);
//
//		LogPipelineService service = new LogPipelineService(logParserFactory);
//		service.processFile("session-1", logFile);
//
//		ArgumentCaptor<String> parsedChunkCaptor = ArgumentCaptor.forClass(String.class);
//		verify(parser, times(2)).parse(parsedChunkCaptor.capture());
//
//		List<String> parsedChunks = parsedChunkCaptor.getAllValues();
//		assertEquals(
//				lines.get(0) + "\n" + lines.get(1) + "\n" + lines.get(2) + "\n" + lines.get(3),
//				parsedChunks.get(0)
//		);
//		assertEquals(
//				lines.get(4) + "\n" + lines.get(5),
//				parsedChunks.get(1)
//		);
//	}
//}
//
