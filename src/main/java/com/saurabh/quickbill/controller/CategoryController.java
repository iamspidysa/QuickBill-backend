package com.saurabh.quickbill.controller;

import com.saurabh.quickbill.io.CategoryResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    public CategoryResponse addCategory(@ResponseBody CategoryResponse){

    }

}
