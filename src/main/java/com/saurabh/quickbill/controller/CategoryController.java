package com.saurabh.quickbill.controller;

import com.saurabh.quickbill.io.CategoryRequest;
import com.saurabh.quickbill.io.CategoryResponse;
import com.saurabh.quickbill.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse addCategory(@RequestBody CategoryRequest request){
        return categoryService.add(request);
    }

}
