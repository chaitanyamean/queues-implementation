package com.url_shortner.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.url_shortner.project.dto.UrlRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType; // ✅ ADD THIS
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

// ✅ CORRECT STATIC IMPORTS (Only these should remain)
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UrlShortnerProjectApplicationTests {

	@Test
	void contextLoads() {
	}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testUrlShorteningAndRedirectionFlow() throws Exception {
        String originalUrl = "https://example.com";
        UrlRequestDto requestDto = new UrlRequestDto();
        requestDto.setUrl(originalUrl);

        // 1. Call /shorten API
        MvcResult result = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").exists())
                .andExpect(jsonPath("$.originalUrl").value(originalUrl))
                .andReturn();

        // Extract the short code from the response
        String responseBody = result.getResponse().getContentAsString();
        String shortCode = objectMapper.readTree(responseBody).get("shortCode").asText();

        // 2. Call /redirect API with the extracted code
        mockMvc.perform(get("/redirect")
                        .param("code", shortCode))
                .andExpect(status().isFound()) // HTTP 302
                .andExpect(header().string("Location", originalUrl)); // Verify it redirects to the right place
    }

    @Test
    public void testDuplicateUrlReturnsSameShortCode() throws Exception {
        // Generate a random URL to ensure a clean state
        String randomUrl = "https://example.com/product/" + UUID.randomUUID().toString();

        UrlRequestDto requestDto = new UrlRequestDto();
        requestDto.setUrl(randomUrl);

        // 1. First Call: Should create a NEW short code
        MvcResult firstResult = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andReturn();

        String response1 = firstResult.getResponse().getContentAsString();
        String code1 = objectMapper.readTree(response1).get("shortCode").asText();

        // 2. Second Call: Should return the EXISTING short code
        MvcResult secondResult = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated()) // Or 200 OK depending on your preference
                .andReturn();

        String response2 = secondResult.getResponse().getContentAsString();
        String code2 = objectMapper.readTree(response2).get("shortCode").asText();

        // 3. Assertion: The codes MUST be identical
        assertEquals(code1, code2, "The short codes should be the same for duplicate URLs");
    }

    @Test
    public void testEmptyUrlReturnsValidationError() throws Exception {
        // 1. Prepare request with EMPTY URL
        UrlRequestDto requestDto = new UrlRequestDto();
        requestDto.setUrl(""); // Empty string

        // 2. Perform POST request
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                // 3. Verify Status: 400 Bad Request
                // (Change to .isNotFound() if you really implemented 404)
                .andExpect(status().isBadRequest())

                // 4. Verify the specific error message in JSON
                // This checks that the key "url" has the value "URL cannot be empty"
                .andExpect(jsonPath("$.url").value("URL cannot be empty"));
    }

}
