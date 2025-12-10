package com.saurabh.quickbill.service;

import com.saurabh.quickbill.io.CategoryRequest;
import com.saurabh.quickbill.io.CategoryResponse;

public interface CategoryService {
    CategoryResponse add(CategoryRequest request);
}
