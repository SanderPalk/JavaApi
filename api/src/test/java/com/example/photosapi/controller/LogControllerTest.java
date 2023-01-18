package com.example.photosapi.controller;

import com.example.photosapi.config.LogConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
public class LogControllerTest {
    @InjectMocks
    private LogController logController;
    @Mock
    private LogConfig logConfig;
    @MockBean
    private Environment env;
    private MockMvc mockMvc;
    private List<String> logLines;
    private File logfile;

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(logController).build();

        logLines = Arrays.asList(
                "12:00:00.000 [main] INFO PhotoController - Session=123 Method=POST Body={id:1,userId:1,title:'My first photo',url:'https://example.com/photo1.jpg',thumbnailUrl:'https://example.com/photo1_thumbnail.jpg'}",
                "12:01:00.000 [main] INFO PhotoController - Session=123 Method=PUT Body={id:1,userId:1,title:'My updated first photo',url:'https://example.com/photo1.jpg',thumbnailUrl:'https://example.com/photo1_thumbnail.jpg'}" +
                        " OldBody={id:1,userId:1,title:My first photo,url:https://example.com/photo1.jpg,thumbnailUrl:https://example.com/photo1_thumbnail.jpg}"
        );

        logfile = File.createTempFile("logs-test", "log");
        when(env.getProperty("logfile")).thenReturn(logfile.getPath());

        FileWriter writer = new FileWriter(logfile);
        writer.write(logLines.get(0) + System.lineSeparator() + logLines.get(1));
        writer.close();
    }

    @Test
    public void testGetLogs() throws Exception {
        when(logConfig.getLogsFilePath()).thenReturn(logfile.toPath());

        String expected = "[{\"time\":\"12:00:00.000\",\"sessionId\":\"123\",\"method\":\"POST\",\"body\":\"id:1,userId:1,title:My first photo,url:https://example.com/photo1.jpg,thumbnailUrl:https://example.com/photo1_thumbnail.jpg\",\"oldBody\":null}," +
                "{\"time\":\"12:01:00.000\",\"sessionId\":\"123\",\"method\":\"PUT\",\"body\":\"id:1,userId:1,title:My updated first photo,url:https://example.com/photo1.jpg,thumbnailUrl:https://example.com/photo1_thumbnail.jpg\",\"oldBody\":\"id:1,userId:1,title:My first photo,url:https://example.com/photo1.jpg,thumbnailUrl:https://example.com/photo1_thumbnail.jpg\"}]";

        MvcResult result = mockMvc.perform(get("/api/v1/logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Assertions.assertEquals(expected, response);
    }
}
