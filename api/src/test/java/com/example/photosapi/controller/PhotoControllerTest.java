package com.example.photosapi.controller;

import com.example.photosapi.config.RateLimiter;
import com.example.photosapi.model.Photo;
import com.example.photosapi.repository.PhotoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
class PhotoControllerTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private RateLimiter rateLimiter;
    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;
    @Mock
    private PhotoRepository photoRepository;
    @InjectMocks
    private PhotoController photoController;

    private MockMvc mockMvc;
    private ObjectMapper mapper;

    private Photo photo1;
    private Photo photo2;


    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        this.mockMvc = MockMvcBuilders.standaloneSetup(photoController).build();
        this.mapper = new ObjectMapper();

        photo1 = new Photo(1, 1, "My first photo", "https://example.com/photo1.jpg", "https://example.com/photo1_thumbnail.jpg", "user");
        photo2 = new Photo(2, 2, "My second photo", "https://example.com/photo2.jpg", "https://example.com/photo2_thumbnail.jpg", "admin");

        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
    }

    @Test
    public void testGetPhotos() throws Exception {
        List<Photo> photos = Arrays.asList(photo1, photo2);
        when(photoRepository.findAll()).thenReturn(photos);

        mockMvc.perform(get("/photos")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andReturn();

        verify(photoRepository, atLeastOnce()).findAll();
    }

    @Test
    @WithMockUser
    public void testAddPhoto() throws Exception {
        when(photoRepository.save(any(Photo.class))).thenReturn(photo1);
        when(rateLimiter.tryConsume()).thenReturn(true);

        mockMvc.perform(post("/photos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(photo1)))
                .andExpect(status().isOk())
                .andReturn();

        verify(photoRepository, atLeastOnce()).save(any(Photo.class));
        verify(simpMessagingTemplate, atLeastOnce()).convertAndSend(eq("/photos/websocket"), any(Photo.class));
    }

    @Test
    @WithMockUser
    public void testEditPhoto() throws Exception {
        when(photoRepository.findById(1)).thenReturn(Optional.of(photo1));
        when(photoRepository.save(any(Photo.class))).thenReturn(photo1);

        photo1.setTitle("My updated photo");
        mockMvc.perform(put("/photos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(photo1)))
                .andExpect(status().isOk())
                .andReturn();

        verify(photoRepository, atLeastOnce()).findById(1);
        verify(photoRepository, atLeastOnce()).save(any(Photo.class));
        verify(simpMessagingTemplate, atLeastOnce()).convertAndSend(eq("/photos/websocket"), any(Photo.class));
    }

    @Test
    @WithMockUser(username = "admin")
    public void testEditPhotoWrongUser() throws Exception {
        when(photoRepository.findById(1)).thenReturn(Optional.of(photo1));
        when(photoRepository.save(any(Photo.class))).thenReturn(photo1);

        photo1.setTitle("My updated photo");
        mockMvc.perform(put("/photos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(photo1)))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    @WithMockUser
    public void testDeletePhoto() throws Exception {
        when(photoRepository.findById(1)).thenReturn(Optional.of(photo1));

        mockMvc.perform(delete("/photos/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        verify(photoRepository, atLeastOnce()).deleteById(1);
        verify(simpMessagingTemplate, atLeastOnce()).convertAndSend(eq("/photos/websocket"), any(Photo.class));
    }

}
