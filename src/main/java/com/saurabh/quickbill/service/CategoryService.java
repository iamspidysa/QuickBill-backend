package com.saurabh.quickbill.service;

import com.saurabh.quickbill.io.CategoryRequest;
import com.saurabh.quickbill.io.CategoryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CategoryService {
    CategoryResponse add(CategoryRequest request, MultipartFile file);

    List<CategoryResponse> read();

    void delete(String categoryId);
}


