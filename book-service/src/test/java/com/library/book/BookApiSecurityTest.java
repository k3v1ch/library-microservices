package com.library.book;

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

/** Проверка авторизации: кто и что имеет право делать с книгами. */
@SpringBootTest
@AutoConfigureMockMvc
class BookApiSecurityTest {

    private static final String BOOK_JSON = """
            {"title":"Предметно-ориентированное проектирование","author":"Эрик Эванс"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void searchIsPublic() throws Exception {
        mockMvc.perform(get("/books")).andExpect(status().isOk());
    }

    @Test
    void creatingBookRequiresToken() throws Exception {
        mockMvc.perform(post("/books").contentType(MediaType.APPLICATION_JSON).content(BOOK_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void readerCannotCreateBook() throws Exception {
        mockMvc.perform(post("/books")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_READER")))
                        .contentType(MediaType.APPLICATION_JSON).content(BOOK_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void librarianCanCreateBook() throws Exception {
        mockMvc.perform(post("/books")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON).content(BOOK_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void internalApiIsClosedForUsers() throws Exception {
        mockMvc.perform(post("/internal/reservations")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"borrowId\":\"11111111-1111-1111-1111-111111111111\"," +
                                "\"bookId\":\"22222222-2222-2222-2222-222222222222\"}"))
                .andExpect(status().isForbidden());
    }
}
