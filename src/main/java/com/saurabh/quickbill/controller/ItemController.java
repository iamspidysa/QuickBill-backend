package com.saurabh.quickbill.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saurabh.quickbill.io.ItemRequest;
import com.saurabh.quickbill.io.ItemResponse;
import com.saurabh.quickbill.service.ItemService;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final Validator validator;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/admin/items")
    public ItemResponse addItem(@RequestPart("item") String itemString,
                                @RequestPart("image") MultipartFile file){
        ObjectMapper objectMapper = new ObjectMapper();
//        ItemRequest itemRequest = null;

        try{
            ItemRequest itemRequest = objectMapper.readValue(itemString, ItemRequest.class);
            // Manual validation for multipart
            validateRequest(itemRequest);

            return itemService.add(itemRequest,file);
        } catch (ValidationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception ex) {
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

    //Read Items
    @GetMapping("/items")
    public List<ItemResponse> readItems(){
        return itemService.fetchItems();
    }

    //Delete Item
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/admin/items/{itemId}")
    public void removeItem(@PathVariable String itemId){
        try{
            itemService.deleteItem(itemId);
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found");
        }
    }
}
