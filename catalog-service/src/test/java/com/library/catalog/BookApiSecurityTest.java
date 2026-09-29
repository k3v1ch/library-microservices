package com.library.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Проверка авторизации: кто и что имеет право делать с каталогом. */
@SpringBootTest
@AutoConfigureMockMvc
class BookApiSecurityTest {

    private static final String BOOK_JSON = """
            {"isbn":"9785001234567","title":"Домен","author":"Эванс","genre":"IT",
             "publishedYear":2004,"totalCopies":3}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void searchIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/books")).andExpect(status().isOk());
    }

    @Test
    void creatingBookRequiresToken() throws Exception {
        mockMvc.perform(post("/api/v1/books").contentType(MediaType.APPLICATION_JSON).content(BOOK_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void readerCannotCreateBook() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_READER")))
                        .contentType(MediaType.APPLICATION_JSON).content(BOOK_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void librarianCanCreateBook() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON).content(BOOK_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void internalApiIsClosedForUsers() throws Exception {
        mockMvc.perform(post("/internal/v1/reservations")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loanId\":\"11111111-1111-1111-1111-111111111111\"," +
                                "\"bookId\":\"22222222-2222-2222-2222-222222222222\"}"))
                .andExpect(status().isForbidden());
    }
}
