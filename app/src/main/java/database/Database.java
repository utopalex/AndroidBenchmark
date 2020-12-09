package database;

import android.content.Context;

import androidx.annotation.NonNull;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.nongmsauth.FirebaseRestAuth;
import com.google.firebase.nongmsauth.FirebaseRestAuthUser;
import com.google.firebase.nongmsauth.api.types.identitytoolkit.SignInAnonymouslyResponse;

import java.util.HashMap;

import vendetta.androidbenchmark.MainActivity;
import vendetta.androidbenchmark.ScoreActivity;

/**
 * Created by Vendetta on 06-May-17.
 */

public class Database {
    private static final String TAG = "Database";
    private static String uid = null;
    protected static FirebaseRestAuth mAuth = null;
    private static FirebaseDatabase database = null;
    private static DatabaseReference databaseUserScoreRef;
    private static UserScores dbUserScores = new UserScores(true);
    private static Context mainActivityContext;
    private static HashMap<String, String> results;


    public static void establishConnection(Context context) {
        if (mAuth == null) {
            initialize(context);
        } else {
            mainActivityContext = context;
            updateUserScores();
        }
    }

    private static void initialize(Context context) {
        mainActivityContext = context;
        FirebaseApp mApp = FirebaseApp.getInstance();
        mAuth = FirebaseRestAuth.Companion.getInstance(mApp);
        FirebaseRestAuthUser user = mAuth.getCurrentUser();

        if (user != null) {
            uid = user.getUserId();
            Log.d(TAG, uid + "connected");
        }
        // User is signed in
        if (uid != null) {
            Log.d(TAG, "User " + uid + " is logged in!");
            if (database == null) {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true);
                database = FirebaseDatabase.getInstance();
                database.getReference().child("benchmarks").keepSynced(true);
            }
            updateUserScores();
            Log.d(TAG, "onAuthStateChanged:signed_in:" + uid);
        }

        mAuth.signInAnonymously().addOnSuccessListener(new OnSuccessListener<SignInAnonymouslyResponse>() {
            @Override
            public void onSuccess(SignInAnonymouslyResponse signInAnonymouslyResponse) {
                if (database == null) {
                    FirebaseDatabase.getInstance().setPersistenceEnabled(true);
                    database = FirebaseDatabase.getInstance();
                    database.getReference().child("benchmarks").keepSynced(true);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    private static void updateUserScores() {
        databaseUserScoreRef = database.getReference().child("users").child(uid);
        databaseUserScoreRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                HashMap<String, String> tmp = (HashMap<String, String>) dataSnapshot.getValue();
                if (tmp != null)
                    dbUserScores.updateAll(tmp);
                MainActivity.updateScores(dbUserScores, mainActivityContext);
                Log.d(TAG, "UserScores read from DB");
                if (database != null) {
                    database.goOffline();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    public static void postBenchScore(Score score) {
        score.setUid(uid);
        database.goOffline();
        database.getReference().child("benchmarks").child(score.getBenchName()).child(uid).setValue(score);
        databaseUserScoreRef.updateChildren(score.toMap());
        Log.d("DB ", "posted " + score.toString());
    }

    public static void getBenchScore(String benchmarkName) {
        database.getReference().child("benchmarks").child(benchmarkName).child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    ScoreActivity.updateResult(dataSnapshot.getValue(Score.class));
                    Log.d(TAG, "BenchmarkScore read from DB");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.d(TAG, "Failed to read value at benchmarkScore.", error.toException());
            }
        });
    }

    public static void getRankings(String benchmarkName, final Context rankContext) {
        results = new HashMap<>();
        database.getReference().child("benchmarks").child(benchmarkName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int entries = 0;
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    entries++;
                    try {
                        Score tempScore = data.getValue(Score.class);
                        if (results.get(tempScore.getDevice()) == null || Long.parseLong(results.get(tempScore.getDevice())) < Long.parseLong(tempScore.getResult()))
                            results.put(tempScore.getDevice(), tempScore.getResult());
                    } catch (com.google.firebase.database.DatabaseException e) {
                        Log.d(TAG, "Could not convert: " +  e.getMessage());
                    }
                }

                Log.d(TAG, "total entries: " + entries);
                ScoreActivity.updateRanking(results, rankContext);
                Log.d(TAG, "Rankings read from DB");
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.d(TAG, "Failed to read value at rankings.", error.toException());
            }
        });

    }

    public static Context getContext() {
        return mainActivityContext;
    }


}
