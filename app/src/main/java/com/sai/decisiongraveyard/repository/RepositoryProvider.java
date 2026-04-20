package com.sai.decisiongraveyard.repository;

import android.content.Context;

public class RepositoryProvider {
    private static RepositoryProvider instance;
    private final DecisionRepository decisionRepository;
    private final CauseEffectRepository causeEffectRepository;
    private final ActivityRepository activityRepository;
    private final RecoveryMissionRepository recoveryMissionRepository;
    private final UserProfileRepository userProfileRepository;
    private final DailyCheckInRepository dailyCheckInRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final LLMService llmService;

    private RepositoryProvider(Context context) {
        Context appContext = context.getApplicationContext();
        
        // Initialize independent services
        LLMService tempLlmService;
        try {
            tempLlmService = new LLMService(appContext);
        } catch (Exception e) {
            tempLlmService = null;
        }
        this.llmService = tempLlmService;
        
        // Initialize repositories
        this.userProfileRepository = new UserProfileRepository(appContext);
        this.activityRepository = new ActivityRepository(appContext);
        this.recoveryMissionRepository = new RecoveryMissionRepository(appContext);
        this.decisionRepository = new DecisionRepository(appContext);
        this.causeEffectRepository = new CauseEffectRepository(appContext);
        this.dailyCheckInRepository = new DailyCheckInRepository(appContext);
        this.userPreferencesRepository = new UserPreferencesRepository();
        
        // Inject dependencies into DecisionRepository
        this.decisionRepository.setCauseEffectRepository(this.causeEffectRepository);
        this.decisionRepository.setActivityRepository(this.activityRepository);
        this.decisionRepository.setRecoveryMissionRepository(this.recoveryMissionRepository);
        this.decisionRepository.setUserProfileRepository(this.userProfileRepository);
        this.decisionRepository.setLLMService(this.llmService);

        // Inject dependencies into CauseEffectRepository
        this.causeEffectRepository.setDecisionRepository(this.decisionRepository);
        this.causeEffectRepository.setActivityRepository(this.activityRepository);
        
        // Inject dependencies into DailyCheckInRepository
        this.dailyCheckInRepository.setUserProfileRepository(this.userProfileRepository);
        
        // Inject dependencies into ActivityRepository
        this.activityRepository.setUserProfileRepository(this.userProfileRepository);
        
        // Inject dependencies into RecoveryMissionRepository
        this.recoveryMissionRepository.setUserProfileRepository(this.userProfileRepository);
    }

    public static synchronized RepositoryProvider getInstance(Context context) {
        if (instance == null) {
            instance = new RepositoryProvider(context);
        }
        return instance;
    }

    public DecisionRepository getDecisionRepository() {
        return decisionRepository;
    }

    public CauseEffectRepository getCauseEffectRepository() {
        return causeEffectRepository;
    }

    public ActivityRepository getActivityRepository() {
        return activityRepository;
    }

    public RecoveryMissionRepository getRecoveryMissionRepository() {
        return recoveryMissionRepository;
    }

    public UserProfileRepository getUserProfileRepository() {
        return userProfileRepository;
    }

    public DailyCheckInRepository getDailyCheckInRepository() {
        return dailyCheckInRepository;
    }

    public UserPreferencesRepository getUserPreferencesRepository() {
        return userPreferencesRepository;
    }

    public LLMService getLLMService() {
        return llmService;
    }
}
