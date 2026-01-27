package com.saurabh.quickbill.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saurabh.quickbill.io.CategoryRequest;
import com.saurabh.quickbill.io.CategoryResponse;
import com.saurabh.quickbill.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
//import software.amazon.awssdk.thirdparty.jackson.core.JsonProcessingException;
//import tools.jackson.databind.ObjectMapper;

import java.util.List;

@RestController
//@RequestMapping("/categories")
@RequiredArgsConstructor

public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse addCategory(@RequestPart("category") String categoryString, @RequestPart("file") MultipartFile file){
        ObjectMapper objectMapper = new ObjectMapper();
        CategoryRequest request = null;
        try{
            request = objectMapper.readValue(categoryString, CategoryRequest.class);
            return categoryService.add(request,file);
        }
        // JsonProcessingException not working, instead we went for Exception.
        catch (Exception ex){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Exception occurred  while parsing the json"+ex.getMessage());
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
