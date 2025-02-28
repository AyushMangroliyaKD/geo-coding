package com.caching.service;

import com.caching.exception.InvalidParameterException;
import com.caching.exception.InvalidReponseFormatException;
import com.caching.exception.ResourceNotFoundException;
import com.caching.exception.UnauthorizedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Service
public class APIService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(APIService.class);

    @Value("${access_key}")
    private String apiKey;

    public APIService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get Forward Geocoding result for the provided address.
     * The response contains latitude and longitude.
     * This method uses caching based on the provided address.
     */
    @Cacheable(value = "geocoding", key = "#address", unless = "#address.equalsIgnoreCase('goa')")
    public Map<String, Double> getForwardGeoCoding(String address) {
        logger.info("Attempting to get forward geocoding for address: {}", address);

        String url = "http://api.positionstack.com/v1/forward"
                + "?access_key=" + apiKey
                + "&query=" + address;

        logger.debug("Forward geocoding URL: {}", url);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, null, String.class);
        } catch (Exception e) {
            logger.error("Failed to fetch forward geocoding data for address: {}. Error: {}", address, e.getMessage());
            throw new ResourceNotFoundException("Error fetching data for address: " + address);
        }

        logger.debug("Forward geocoding response status: {}", response.getStatusCode());

        if (response.getStatusCode().is2xxSuccessful()) {
            String responseString = response.getBody();
            logger.debug("Forward geocoding response body: {}", responseString);

            try {
                JsonNode root = objectMapper.readTree(responseString);
                JsonNode data = root.path("data").get(0);

                if (data == null) {
                    logger.warn("No geocoding data found for address: {}", address);
                    throw new InvalidParameterException("Invalid address provided");
                } else {
                    logger.info("Successfully fetched geocoding data for address: {}", address);
                    return Map.of("latitude", data.get("latitude").asDouble(), "longitude", data.get("longitude").asDouble());
                }
            } catch (JsonProcessingException e) {
                logger.error("Error parsing geocoding response for address: {}. Error: {}", address, e.getMessage());
                throw new InvalidReponseFormatException("Invalid response format received");
            }
        } else {
            throwAppropriateException(response.getStatusCode());
        }
        return new HashMap<>();
    }

    /**
     * Get Reverse Geocoding result for the provided latitude and longitude.
     * The response contains the address.
     * This method uses caching based on the provided coordinates.
     */
    @Cacheable(value = "reverse-geocoding", key = "#latitude + ',' + #longitude")
    public String getReverseGeoCoding(String latitude, String longitude) {
        logger.info("Attempting to get reverse geocoding for coordinates: {}, {}", latitude, longitude);

        if (!isValidLatitude(latitude) || !isValidLongitude(longitude)) {
            logger.warn("Invalid latitude or longitude provided: {}, {}", latitude, longitude);
            throw new InvalidParameterException("Invalid latitude or longitude provided");
        }

        String url = "http://api.positionstack.com/v1/reverse"
                + "?access_key=" + apiKey
                + "&query=" + latitude + "," + longitude;

        logger.debug("Reverse geocoding URL: {}", url);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, null, String.class);
        } catch (Exception e) {
            logger.error("Failed to fetch reverse geocoding data for coordinates: {}, {}. Error: {}", latitude, longitude, e.getMessage());
            throw new ResourceNotFoundException("Error fetching data for coordinates: " + latitude + ", " + longitude);
        }

        logger.debug("Reverse geocoding response status: {}", response.getStatusCode());

        if (response.getStatusCode().is2xxSuccessful()) {
            String responseString = response.getBody();
            logger.debug("Reverse geocoding response body: {}", responseString);

            try {
                JsonNode root = objectMapper.readTree(responseString);
                String label = root.get("data").get(0).get("label").asText();
                logger.info("Successfully fetched reverse geocoding data for coordinates: {}, {}", latitude, longitude);
                return label;
            } catch (JsonProcessingException e) {
                logger.error("Error parsing reverse geocoding response for coordinates: {}, {}. Error: {}", latitude, longitude, e.getMessage());
                throw new InvalidReponseFormatException("Invalid response format received");
            }
        } else {
            logger.error("Received error response for query: {} , {}. Status: {}, Body: {}", latitude, longitude, response.getStatusCode(), response.getBody());
            throwAppropriateException(response.getStatusCode());
        }
        return null;
    }

    /**
     * Throw appropriate exception based on the provided status.
     */
    private void throwAppropriateException(HttpStatus status) {
        if (status == HttpStatus.UNAUTHORIZED) {
            throw new UnauthorizedException("Unauthorized access");
        } else if (status == HttpStatus.UNPROCESSABLE_ENTITY) {
            throw new InvalidParameterException("Invalid parameters provided");
        } else {
            throw new ResourceNotFoundException("Resource not found");
        }
    }

    /**
     * Validate the provided latitude.
     */
    private boolean isValidLatitude(String latitude) {
        try {
            double lat = Double.parseDouble(latitude);
            return lat >= -90 && lat <= 90;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate the provided longitude.
     */
    private boolean isValidLongitude(String longitude) {
        try {
            double lon = Double.parseDouble(longitude);
            return lon >= -180 && lon <= 180;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Clear the geocoding cache every 60 seconds.
     */
    @Scheduled(fixedRate = 60000)
    @CacheEvict(value = "geocoding", allEntries = true)
    public void evictGeocodingCache() {
        logger.info("Geocoding cache cleared.");
    }

    /**
     * Clear the reverse geocoding cache every 60 seconds.
     */
    @Scheduled(fixedRate = 60000)
    @CacheEvict(value = "reverse-geocoding", allEntries = true)
    public void evictReverseGeocodingCache() {
        logger.info("Reverse geocoding cache cleared.");
    }
}
