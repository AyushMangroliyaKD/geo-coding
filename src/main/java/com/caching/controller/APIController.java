package com.caching.controller;

import com.caching.service.APIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class APIController {

    private final APIService apiService;

    public APIController(APIService apiService) {
        this.apiService = apiService;
    }

    /**
     * Get Forward Geocoding result for the provided address.
     * The response contains latitude and longitude.
     */
    @GetMapping("/geocoding")
    public ResponseEntity<Map<String, Double>> getGeocoding(@RequestParam String address) {

        Map<String, Double> response = apiService.getForwardGeoCoding(address);
        return ResponseEntity.ok(response);

    }

    /**
     * Get Reverse Geocoding result for the provided latitude and longitude.
     * The response contains the address.
     */
    @GetMapping("/reverse-geocoding")
    public ResponseEntity<String> getReverseGeocoding(@RequestParam String latitude, @RequestParam String longitude) {

        String response = apiService.getReverseGeoCoding(latitude, longitude);
        return ResponseEntity.ok(response);

    }

}