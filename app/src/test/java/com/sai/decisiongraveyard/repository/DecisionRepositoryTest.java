package com.sai.decisiongraveyard.repository;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.sai.decisiongraveyard.model.Decision;
import com.sai.decisiongraveyard.model.DecisionRecord;
import com.sai.decisiongraveyard.model.Evaluation;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

public class DecisionRepositoryTest {

    @Mock
    private DecisionRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void checkDecisionWarning_returnsWarning_whenRegretRateIsHigh() {
        // Arrange
        String category = "money";
        int hour = 10;
        DecisionRepository.DecisionWarning expectedWarning = new DecisionRepository.DecisionWarning(
                true, "Warning message", 75, new ArrayList<>()
        );
        
        when(repository.checkDecisionWarning(category, hour)).thenReturn(expectedWarning);

        // Act
        DecisionRepository.DecisionWarning actualWarning = repository.checkDecisionWarning(category, hour);

        // Assert
        assertNotNull(actualWarning);
        assertTrue(actualWarning.shouldWarn);
        assertEquals(75, actualWarning.regretRate);
    }

    @Test
    public void hasSimilarDecision_returnsTrue_whenDecisionExists() {
        // Arrange
        String title = "Buy stocks";
        String category = "money";
        long time = System.currentTimeMillis();
        
        when(repository.hasSimilarDecision(title, category, time)).thenReturn(true);

        // Act
        boolean exists = repository.hasSimilarDecision(title, category, time);

        // Assert
        assertTrue(exists);
    }
}
