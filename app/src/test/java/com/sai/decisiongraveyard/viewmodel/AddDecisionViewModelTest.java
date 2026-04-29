package com.sai.decisiongraveyard.viewmodel;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.repository.RepositoryProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AddDecisionViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private Application application;
    @Mock
    private DecisionRepository repository;
    @Mock
    private RepositoryProvider repositoryProvider;

    private AddDecisionViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock the RepositoryProvider to return our mock repository
        // Note: This assumes RepositoryProvider can be mocked or injected. 
        // In the real code, it uses a singleton. For unit tests, we usually use a testing version.
        viewModel = new AddDecisionViewModel(application);
        // Since we can't easily swap the repository in the current constructor, 
        // a real test would use a Factory or Dependency Injection.
    }

    @Test
    public void saveDecision_withEmptyTitle_updatesSaveStateWithError() {
        // Arrange
        Observer<AddDecisionViewModel.SaveState> observer = mock(Observer.class);
        viewModel.getSaveState().observeForever(observer);

        // Act
        viewModel.saveDecision("", "Description", "money", System.currentTimeMillis() + 100000);

        // Assert
        ArgumentCaptor<AddDecisionViewModel.SaveState> captor = ArgumentCaptor.forClass(AddDecisionViewModel.SaveState.class);
        verify(observer).onChanged(captor.capture());
        assertFalse(captor.getValue().success);
        assertEquals("Title is required.", captor.getValue().message);
    }

    @Test
    public void saveDecision_withPastDate_updatesSaveStateWithError() {
        // Arrange
        Observer<AddDecisionViewModel.SaveState> observer = mock(Observer.class);
        viewModel.getSaveState().observeForever(observer);

        // Act
        viewModel.saveDecision("Title", "Description", "money", System.currentTimeMillis() - 1000);

        // Assert
        ArgumentCaptor<AddDecisionViewModel.SaveState> captor = ArgumentCaptor.forClass(AddDecisionViewModel.SaveState.class);
        verify(observer).onChanged(captor.capture());
        assertFalse(captor.getValue().success);
        assertEquals("Choose a future review date.", captor.getValue().message);
    }
}
