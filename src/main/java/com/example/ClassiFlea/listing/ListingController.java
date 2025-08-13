package com.example.ClassiFlea.listing;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/listings")
public class ListingController {
    private final ListingRepository listingRepo;
    public ListingController(ListingRepository listingRepo){ this.listingRepo = listingRepo; }

    @GetMapping
    public List<Listing> listActive() {
        return listingRepo.findByStatusIgnoreCase("active");
    }

    @GetMapping("/all")
    public List<Listing> listAll() {
        return listingRepo.findAll();
    }

    @GetMapping("/{id}") 
    public ResponseEntity<?> one(@PathVariable Long id) {
        return listingRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
}
