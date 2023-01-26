package com.example.photosapi.controller;

import com.example.photosapi.config.RateLimiter;
import com.example.photosapi.dto.PhotoDto;
import com.example.photosapi.model.Photo;
import com.example.photosapi.repository.PhotoRepository;
import com.example.photosapi.util.LogUtil;
import com.example.photosapi.util.PhotoMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Type;
import java.util.List;

@RestController
@RequestMapping("photos")
@OpenAPIDefinition(info =
@Info(
        title = "Photos API",
        version = "1.0",
        description = "API for managing photos"
)
)
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PhotoController {

    @Autowired
    private RateLimiter rateLimiter;
    @Autowired
    private HttpServletRequest servletRequest;
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    private PhotoRepository photoRepository;
    private final PhotoMapper photoMapper = new PhotoMapper();
    private final LogUtil logUtil = new LogUtil();

    private static final Logger log = LogManager.getLogger(PhotoController.class);

    public PhotoController() {
    }


    @GetMapping
    @Operation(summary = "Retrieve all photos",
            description = "Retrieves a list of all photos in the system",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema =
                            @Schema(implementation = Photo.class), mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    examples = {@ExampleObject(name = "", value = "{\n" +
                                            "        \"id\": 1,\n" +
                                            "        \"title\": \"photoTitle\",\n" +
                                            "        \"url\": \"www.url.ee\",\n" +
                                            "        \"thumbnail\": \"thumbnail_placeholder\"\n" +
                                            "    }")}),
                            description = "Success Response."),
                    @ApiResponse(responseCode = "404",
                            content = @Content(schema =
                            @Schema(implementation = Photo.class), mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    examples = {@ExampleObject(name = "", value = "[\n" +
                                            "    ]")}),
                            description = "Photos not found")
            })
    public ResponseEntity<List<Photo>> getPhotos() {
        logUtil.handleLog(log, servletRequest);
        return ResponseEntity.ok(photoRepository.findAll());
    }

    @PostMapping
    @Operation(summary = "Add a new photo",
            description = "Adds a new photo to the system",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema =
                            @Schema(implementation = Photo.class), mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    examples = {@ExampleObject(name = "", value = "{\n" +
                                            "        \"id\": 1,\n" +
                                            "        \"title\": \"photoTitle\",\n" +
                                            "        \"url\": \"www.url.ee\",\n" +
                                            "        \"thumbnail\": \"thumbnail_placeholder\"\n" +
                                            "    }")}),
                            description = "Successful addition of photo"),
                    @ApiResponse(responseCode = "400",
                            content = @Content(schema =
                            @Schema(implementation = Photo.class), mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    examples = {@ExampleObject(name = "", value = "{\n" +
                                            "        \"(id\": 1),\n" +
                                            "        \"(title\": \"photoTitle\"),\n" +
                                            "        \"(url\": \"www.url.ee)\",\n" +
                                            "        \"(thumbnail\": \"thumbnail_placeholder)\"\n" +
                                            "    }")}),
                            description = "Invalid request body")
            })

    public ResponseEntity<Type> addPhoto(@RequestBody PhotoDto request) {
        if (!rateLimiter.tryConsume()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        System.out.println(request.getTitle());
        Photo photo = photoRepository.save(photoMapper.toPhoto(request));
        simpMessagingTemplate.convertAndSend("/photos", List.of("POST", photo));
        System.out.println(request.getTitle());
        logUtil.handleLog(log, servletRequest, photo);
        return ResponseEntity.ok().build();
    }

    @PutMapping("{photo_id}")
    @Operation(summary = "Edit a photo",
            description = "Updates a photo in the system with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema =
                            @Schema(implementation = Photo.class), mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    examples = {@ExampleObject(name = "", value = "{\n" +
                                            "        \"(id\": 1),\n" +
                                            "        \"title\": \"photoTitle\",\n" +
                                            "        \"url\": \"www.url.ee\",\n" +
                                            "        \"thumbnail\": \"thumbnail_placeholder\"\n" +
                                            "    }")}),
                            description = "Successful update of photo"),
                    @ApiResponse(responseCode = "404",
                            content = @Content(schema =
                            @Schema(implementation = Photo.class), mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    examples = {@ExampleObject(name = "", value = "{\n" +
                                            "        \"title\": \"photoTitle\",\n" +
                                            "        \"url\": \"www.url.ee\",\n" +
                                            "        \"thumbnail\": \"thumbnail_placeholder\"\n" +
                                            "    }")}),
                            description = "Photo not found"),
                    @ApiResponse(responseCode = "400",
                            content = @Content(schema =
                            @Schema(implementation = Photo.class), mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    examples = {@ExampleObject(name = "", value = "{\n" +
                                            "        \"(id\": 1),\n" +
                                            "        \"(title\": \"photoTitle\"),\n" +
                                            "        \"(url\": \"www.url.ee)\",\n" +
                                            "        \"(thumbnail\": \"thumbnail_placeholder)\"\n" +
                                            "    }")}),
                            description = "Invalid request body")
            }
    )

    public ResponseEntity<Type> editPhoto(@RequestBody PhotoDto request, @PathVariable int photo_id) {
        Photo oldPhoto = photoRepository.findById(photo_id).orElse(null);
        if (oldPhoto == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        if (!oldPhoto.getAuth().equals(SecurityContextHolder.getContext().getAuthentication().getName()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        oldPhoto = oldPhoto.clone();

        Photo newPhoto = photoMapper.toPhoto(request);
        newPhoto.setId(photo_id);
        simpMessagingTemplate.convertAndSend("/photos", List.of("PUT", photoRepository.save(newPhoto)));

        logUtil.handleLog(log, servletRequest, oldPhoto, newPhoto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("{photo_id}")
    @Operation(summary = "Delete a photo",
            description = "Deletes a photo from the system with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema =
                            @Schema(implementation = Photo.class), mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    examples = {@ExampleObject(name = "", value = "[\n" +
                                            "    ]")}),
                            description = "Successful deletion of photo"),
                    @ApiResponse(responseCode = "404",
                            content = @Content(schema =
                            @Schema(implementation = Photo.class), mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    examples = {@ExampleObject(name = "", value = "[\n" +
                                            "    ]")}),
                            description = "Photo not found"),
            })
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Type> deletePhoto(@PathVariable int photo_id) {
        Photo photo = photoRepository.findById(photo_id).orElse(null);
        if (photo == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        photo = photo.clone();

        photoRepository.deleteById(photo_id);
        simpMessagingTemplate.convertAndSend("/photos", List.of("DELETE", photo));

        logUtil.handleLog(log, servletRequest, photo_id, photo);
        return ResponseEntity.ok().build();
    }
}
