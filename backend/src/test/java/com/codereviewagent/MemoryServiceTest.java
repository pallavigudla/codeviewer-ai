package com.codereviewagent;

import com.codereviewagent.dto.MemoryItemDto;
import com.codereviewagent.entity.User;
import com.codereviewagent.repository.UserRepository;
import com.codereviewagent.service.MemoryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@Transactional
public class MemoryServiceTest {

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "memtestuser")
    public void testSaveAndRetrieveHindsightMemory() {
        User testUser = userRepository.findByUsername("memtestuser").orElseGet(() -> 
            userRepository.save(User.builder()
                .username("memtestuser")
                .email("memtest@example.com")
                .passwordHash("$2a$10$e8w5...dummy")
                .fullName("Test Developer")
                .isEmailVerified(true)
                .build())
        );

        MemoryItemDto dto = MemoryItemDto.builder()
                .developerName("Test Developer")
                .language("Java")
                .mistake("Unit test mistake entry")
                .suggestion("Unit test suggestion recommendation")
                .improvement("Unit test improvement standard")
                .build();

        MemoryItemDto saved = memoryService.saveMemory(dto);
        Assertions.assertNotNull(saved.getId());

        List<MemoryItemDto> retrieved = memoryService.retrieveMemory(testUser.getId().toString(), "Java");
        Assertions.assertFalse(retrieved.isEmpty());
        boolean foundMatch = retrieved.stream().anyMatch(m -> m.getMistake() != null && m.getMistake().toLowerCase().contains("unit test"));
        Assertions.assertTrue(foundMatch, "Saved unit test mistake entry should be recalled from Hindsight");
    }
}

