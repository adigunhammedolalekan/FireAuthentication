package goride.com.goride.ui.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import goride.com.goride.R;
import goride.com.goride.models.User;
import goride.com.goride.ui.base.BaseActivity;
import goride.com.goride.util.L;
import goride.com.goride.util.Util;

/**
 * Created by root on 11/12/17.
 */

public class CreateAccountActivity extends BaseActivity {

    @BindView(R.id.edt_phone_create_account)
    EditText phoneEditText;
    @BindView(R.id.edt_fname_create_account)
    EditText firstNameEditText;
    @BindView(R.id.edt_lname_create_account)
    EditText lastNameEditText;
    @BindView(R.id.edt_code_create_account)
    EditText codeEditText;

    /*
    * Fields/EditText containers.
    * */
    @BindView(R.id.layout_phone_create_account)
    LinearLayout phoneContainerLayout;
    @BindView(R.id.otp_layout_create_account)
    LinearLayout otpContainerLayout;
    @BindView(R.id.layout_complete_profile_create_account)
    LinearLayout completeProfileLayout;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mChangedCallbacks;
    private PhoneAuthProvider.ForceResendingToken resendingToken;
    private ProgressDialog progressDialog;
    private String mVerificationID = "";
    private FirebaseAuth firebaseAuth;
    private FirebaseUser newUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_sign_up);
        progressDialog = new ProgressDialog(this);
        firebaseAuth = FirebaseAuth.getInstance();


        mChangedCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                codeEditText.setText(phoneAuthCredential.getSmsCode());
                signIn(phoneAuthCredential);

            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                L.WTF(e);
                /*
                * Verification Failed, Might be network error, or Firebase quota has been exhausted
                * */
                progressDialog.cancel();
                toast("Failed to send code. Please retry");
            }

            @Override
            public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);

                /*
                * Code has been sent to user device.
                * */
                mVerificationID = s;
                resendingToken = forceResendingToken;

                codeSent();
            }
        };
    }
    @OnClick(R.id.btn_finish_create_account) public void onCreateAccountClick() {

        /*
        * Phone number has been verified, Store new user details.
        * */
        User user = new User(Util.textOf(firstNameEditText), Util.textOf(lastNameEditText), Util.textOf(phoneEditText));
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.child("users")
                .child(newUser.getUid()).setValue(user);

        toast("Welcome to GoRide!");
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    /*
    * Verify entered phone number and allow firebase to send verification code.
    * */
    @OnClick(R.id.btn_next_create_account) public void onNextClick() {

        String phone = Util.textOf(phoneEditText);
        if(phone.length() < 11 || phone.length() > 11) {
            toast("Invalid phone number.");
            return;
        }
        progressDialog.setMessage("sending code...");
        progressDialog.show();
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phone, 60, TimeUnit.SECONDS, this, mChangedCallbacks);
    }

    /*
    * Verify OTP
    * */
    @OnClick(R.id.btn_verify_code_create_account) public void onVerifyCodeClick() {

        String code = Util.textOf(codeEditText);
        if(code.isEmpty()) {
            toast("Enter OTP!");
            return;
        }
        PhoneAuthCredential phoneAuthCredential = PhoneAuthProvider.getCredential(mVerificationID, code);
        signIn(phoneAuthCredential);
    }
    /*
    * Update UI using the newly created FirebaseUser.
    * */
    void updateUI(FirebaseUser firebaseUser) {
        if(firebaseUser != null) {
            hideViews(phoneContainerLayout, otpContainerLayout);
            showViews(completeProfileLayout);

            FirebaseDatabase.getInstance()
                    .getReference().child("users")
                    .child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    User user = dataSnapshot.getValue(User.class);
                    if(user != null) {
                        firstNameEditText.setText(user.getFirstName());
                        lastNameEditText.setText(user.getLastName());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }
    void hideViews(ViewGroup ...group) {
        for (ViewGroup viewGroup : group) {
            if(viewGroup != null)
                viewGroup.setVisibility(View.GONE);
        }
    }
    void showViews(ViewGroup ... toShow) {
        for (ViewGroup viewGroup : toShow) {
            if(viewGroup != null)
                viewGroup.setVisibility(View.VISIBLE);
        }
    }
    void signIn(PhoneAuthCredential credential) {

        progressDialog.setMessage("please wait...");
        progressDialog.show();
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        progressDialog.cancel();
                        if (task.isSuccessful()) {
                            newUser =
                                    task.getResult().getUser();
                            updateUI(newUser);
                        }else {
                            /*
                            * Code entered by user might be incorrect
                            * */
                            toast("Failed to create user. Invalid OTP. Please retry");
                            L.WTF(task.getException());
                        }
                    }
                });
    }
    @OnClick(R.id.btn_resend_code) public void onResendClick() {

        PhoneAuthProvider.getInstance().verifyPhoneNumber(Util.textOf(phoneEditText), 60, TimeUnit.SECONDS,
                this, mChangedCallbacks, resendingToken);
    }
    void codeSent() {
        progressDialog.cancel();
        hideViews(phoneContainerLayout, completeProfileLayout);
        showViews(otpContainerLayout);
    }
}
