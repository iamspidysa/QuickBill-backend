package com.saurabh.quickbill.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saurabh.quickbill.io.CategoryRequest;
import com.saurabh.quickbill.io.CategoryResponse;
import com.saurabh.quickbill.service.CategoryService;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
//import software.amazon.awssdk.thirdparty.jackson.core.JsonProcessingException;
//import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.stream.Collectors;

@RestController
//@RequestMapping("/categories")
@RequiredArgsConstructor

public class CategoryController {

    private final CategoryService categoryService;
    private final Validator validator;

    @PostMapping("/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse addCategory(@RequestPart("category") String categoryString, @RequestPart("file") MultipartFile file){
        ObjectMapper objectMapper = new ObjectMapper();
//        CategoryRequest request = null;
        try{
            CategoryRequest request = objectMapper.readValue(categoryString, CategoryRequest.class);

            // Manual validation for multipart
            validateRequest(request);
            
            return categoryService.add(request,file);

        } catch (ValidationException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception ex){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request: " + ex.getMessage());
        }
    }

    private <T> void validateRequest(T request) {
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new ValidationException(errors);
        }
    }

    @GetMapping("/categories")
    public List<CategoryResponse> fetchCategory(){
        return categoryService.read();
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("admin/categories/{categoryId}")
    public void remove(@PathVariable String categoryId){
        try {
            categoryService.delete(categoryId);
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,e.getMessage());
        }
    }

}
