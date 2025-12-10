package com.saurabh.quickbill.service.impl;

import com.saurabh.quickbill.io.CategoryRequest;
import com.saurabh.quickbill.io.CategoryResponse;
import com.saurabh.quickbill.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    @Override
    public CategoryResponse add(CategoryRequest request) {
        return null;
    }
}
