package goride.com.goride.ui.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import goride.com.goride.R;
import goride.com.goride.models.User;
import goride.com.goride.ui.base.BaseActivity;

/**
 * Created by root on 11/14/17.
 */

public class MainActivity extends BaseActivity {

    @BindView(R.id.user_tv)
    TextView userTextView;
    private User currentUser;

    private DatabaseReference userReference;
    private FirebaseUser firebaseUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        userReference = FirebaseDatabase.getInstance()
                .getReference().child("users");
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if(firebaseUser != null) {
            userReference.child(firebaseUser.getUid())
                    .addListenerForSingleValueEvent(valueEventListener);
        }

    }
    private ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {

            currentUser = dataSnapshot.getValue(User.class);
            if(currentUser != null) {
                userTextView.setText(String.valueOf("Welcome, " + currentUser.toString()));
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };
}
