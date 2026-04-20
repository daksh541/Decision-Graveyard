package com.sai.decisiongraveyard.repository;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.sai.decisiongraveyard.model.DailyCheckIn;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DailyCheckInRepository {

    private static final String TAG = "DailyCheckInRepository";
    private static final String COLLECTION_NAME = "dailyCheckIns";

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private UserProfileRepository userProfileRepository;

    public DailyCheckInRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void setUserProfileRepository(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public interface CheckInCallback {
        void onSuccess(DailyCheckIn checkIn);
        void onError(String error);
    }

    public interface CheckInListCallback {
        void onSuccess(List<DailyCheckIn> checkIns);
        void onError(String error);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String error);
    }

    public String requireUserId() {
        if (auth.getCurrentUser() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        return auth.getCurrentUser().getUid();
    }

    private DocumentReference checkInDocument(String checkInId) {
        return db.collection(COLLECTION_NAME).document(checkInId);
    }

    private String getTodayKey() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return String.valueOf(calendar.getTimeInMillis());
    }

    private long getTodayStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getTodayEnd() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    public void getTodayCheckIn(CheckInCallback callback) {
        String userId = requireUserId();
        long todayStart = getTodayStart();
        long todayEnd = getTodayEnd();

        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    DailyCheckIn todayCheckIn = null;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        DailyCheckIn checkIn = doc.toObject(DailyCheckIn.class);
                        if (checkIn != null && checkIn.getDate() >= todayStart && checkIn.getDate() <= todayEnd) {
                            checkIn.setCheckInId(doc.getId());
                            todayCheckIn = checkIn;
                            break;
                        }
                    }
                    callback.onSuccess(todayCheckIn);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting today's check-in", e);
                    callback.onError(e.getMessage());
                });
    }

    public void createTodayCheckIn(CheckInCallback callback) {
        String userId = requireUserId();
        long today = getTodayStart();

        DailyCheckIn checkIn = new DailyCheckIn(userId, today);

        db.collection(COLLECTION_NAME)
                .add(checkIn)
                .addOnSuccessListener(documentReference -> {
                    checkIn.setCheckInId(documentReference.getId());
                    callback.onSuccess(checkIn);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating check-in", e);
                    callback.onError(e.getMessage());
                });
    }

    public void updateCheckIn(DailyCheckIn checkIn, ActionCallback callback) {
        if (checkIn.getCheckInId() == null) {
            callback.onError("CheckIn ID is null");
            return;
        }

        db.collection(COLLECTION_NAME)
                .document(checkIn.getCheckInId())
                .set(checkIn)
                .addOnSuccessListener(aVoid -> {
                    // Award XP and update streaks if check-in is complete
                    if (checkIn.isComplete() && userProfileRepository != null) {
                        userProfileRepository.incrementDailyDisciplineStreak(new UserProfileRepository.ActionCallback() {
                            @Override
                            public void onSuccess() {
                                callback.onSuccess();
                            }

                            @Override
                            public void onError(String error) {
                                callback.onSuccess(); // Still succeed even if XP fails
                            }
                        });
                    } else {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating check-in", e);
                    callback.onError(e.getMessage());
                });
    }

    public void markDecisionsMade(String checkInId, ActionCallback callback) {
        getCheckInById(checkInId, new CheckInCallback() {
            @Override
            public void onSuccess(DailyCheckIn checkIn) {
                if (checkIn == null) {
                    callback.onError("Check-in not found");
                    return;
                }
                checkIn.setDecisionsMade(true);
                updateCheckIn(checkIn, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void markActivitiesCompleted(String checkInId, ActionCallback callback) {
        getCheckInById(checkInId, new CheckInCallback() {
            @Override
            public void onSuccess(DailyCheckIn checkIn) {
                if (checkIn == null) {
                    callback.onError("Check-in not found");
                    return;
                }
                checkIn.setActivitiesCompleted(true);
                updateCheckIn(checkIn, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getCheckInById(String checkInId, CheckInCallback callback) {
        db.collection(COLLECTION_NAME)
                .document(checkInId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    DailyCheckIn checkIn = documentSnapshot.toObject(DailyCheckIn.class);
                    if (checkIn != null) {
                        checkIn.setCheckInId(documentSnapshot.getId());
                    }
                    callback.onSuccess(checkIn);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting check-in by ID", e);
                    callback.onError(e.getMessage());
                });
    }

    public void getRecentCheckIns(int days, CheckInListCallback callback) {
        String userId = requireUserId();
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);

        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", cutoffTime)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DailyCheckIn> checkIns = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        DailyCheckIn checkIn = doc.toObject(DailyCheckIn.class);
                        if (checkIn != null) {
                            checkIn.setCheckInId(doc.getId());
                            checkIns.add(checkIn);
                        }
                    }
                    // Sort descending by date
                    checkIns.sort((c1, c2) -> Long.compare(c2.getDate(), c1.getDate()));
                    callback.onSuccess(checkIns);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting recent check-ins", e);
                    callback.onError(e.getMessage());
                });
    }

    public int calculateDailyDisciplineStreak(List<DailyCheckIn> checkIns) {
        if (checkIns == null || checkIns.isEmpty()) {
            return 0;
        }

        int streak = 0;
        long currentTime = System.currentTimeMillis();
        long oneDay = 24L * 60 * 60 * 1000;

        for (DailyCheckIn checkIn : checkIns) {
            if (checkIn.isComplete()) {
                streak++;
            } else {
                break;
            }
        }

        return streak;
    }

    public int calculateMissedDayStreak(List<DailyCheckIn> checkIns) {
        if (checkIns == null || checkIns.isEmpty()) {
            return 0;
        }

        int streak = 0;
        for (DailyCheckIn checkIn : checkIns) {
            if (!checkIn.isComplete()) {
                streak++;
            } else {
                break;
            }
        }

        return streak;
    }

    private <T> T await(Task<T> task) {
        try {
            return Tasks.await(task);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
