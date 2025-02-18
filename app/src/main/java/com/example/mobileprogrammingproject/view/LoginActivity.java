package com.example.mobileprogrammingproject.view;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mobileprogrammingproject.R;
import com.example.mobileprogrammingproject.database.AppDatabase;
import com.example.mobileprogrammingproject.databinding.ActivityLoginBinding;
import com.example.mobileprogrammingproject.contract.LoginContract;
import com.example.mobileprogrammingproject.presenter.LoginPresenter;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeV2ResponseCallback;
import com.kakao.usermgmt.response.MeV2Response;
import com.kakao.util.exception.KakaoException;
import com.example.mobileprogrammingproject.constants.Constants.ELogin;

public class LoginActivity extends AppCompatActivity implements LoginContract.View, GoogleApiClient.OnConnectionFailedListener{

    // Attributes
    private ActivityLoginBinding mBinding;
    private static final int signUpResultCode = 200;
    private LoginContract.Presenter loginPresenter;
    private AppDatabase mAppDatabase;

    // kakao Attributes
    private ISessionCallback mSessionCallback;

    // google Attributes
    private SignInButton btn_google; // 구글 로그인 버튼
    private FirebaseAuth auth; // 파이어 베이스 인증 객체
    private GoogleApiClient googleApiClient; // 구글 API 클라이언트 객체
    private static final int REQ_SIGN_GOOGLE = 100; // 구글 로그인 결과 코드
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set Binding
        mBinding = ActivityLoginBinding.inflate(getLayoutInflater());
        View view = mBinding.getRoot();
        setContentView(view);

        // set Database
        mAppDatabase = AppDatabase.getInstance(getApplicationContext());

        loginPresenter = new LoginPresenter(getApplicationContext(), mAppDatabase, this);

        // init
        apiInit();
        init();
    }

    private void apiInit() {

        // kakao Login
        mSessionCallback = new ISessionCallback() {
            @Override
            public void onSessionOpened() {
                // 로그인 요청
                UserManagement.getInstance().me(new MeV2ResponseCallback() {
                    @Override
                    public void onFailure(ErrorResult errorResult) {
                        // 로그인 실패
                        showToast(ELogin.kakaoLoginErrorMessage.getText());
                    }

                    @Override
                    public void onSessionClosed(ErrorResult errorResult) {
                        // 세션이 닫힘..
                        showToast(ELogin.kakaoSessionClosedMessage.getText());
                    }

                    @Override
                    public void onSuccess(MeV2Response result) {
                        // 로그인 성공
                        Intent intent = loginPresenter.kakakOnSuccess(result);
                        startActivity(intent);
                        showToast(ELogin.kakaoLoginSuccessMessage.getText());
                    }
                });
            }

            @Override
            public void onSessionOpenFailed(KakaoException exception)
            {
                showToast(ELogin.kakaoSessionFailedMessage.getText());
            }
        };
        Session.getCurrentSession().addCallback(mSessionCallback);
        Session.getCurrentSession().checkAndImplicitOpen();

//        getAppKeyHash();

        // google Login
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
        auth = FirebaseAuth.getInstance(); // 파이어베이스 인증 객체 초기화

        mBinding.btnGoogleLogin.setOnClickListener(new View.OnClickListener() { // 구글 로그인 버튼을 클릭했을 때 이곳을 수행
            @Override
            public void onClick(View view) {
                Intent intent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(intent, REQ_SIGN_GOOGLE); //
            }
        });
    }

    private void init() {

        // google LoginButton setText
        setGooglePlusButtonText(mBinding.btnGoogleLogin, ELogin.googleLoginButtonText.getText());

        mBinding.tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivityForResult(intent, signUpResultCode);
            }
        });
        mBinding.tvSearchEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SearchEmailActivity.class);
                startActivity(intent);
            }
        });
        mBinding.tvSearchPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SearchPasswordActivity.class);
                startActivity(intent);
            }
        });

        mBinding.btnAppLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Boolean loginSuccess = loginPresenter.validLoginCheck(mBinding.etLoginEmail.getText().toString(), mBinding.etLoginPassword.getText().toString());
                if(loginSuccess == true){
                    Intent intent = loginPresenter.appOnSuccess(mBinding.etLoginEmail.getText().toString(), mBinding.etLoginPassword.getText().toString());
                    mBinding.etLoginEmail.setText("");
                    mBinding.etLoginPassword.setText("");
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        updateUI(currentUser);
    }

    // 카카오 로그인 시 필요한 해시키를 얻는 메소드 이다.
//    private void getAppKeyHash() {
//        try {
//            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
//            for (Signature signature : info.signatures) {
//                MessageDigest md;
//                md = MessageDigest.getInstance("SHA");
//                md.update(signature.toByteArray());
//                String something = new String(Base64.encode(md.digest(), 0));
//                Log.e("Hash key", something);
//            }
//        } catch (Exception e){
//            Log.e("name not found", e.toString());
//        }
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // kakao Login
        if(Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)){
            super.onActivityResult(requestCode, resultCode, data);
        }

        // google Login
        if (requestCode == REQ_SIGN_GOOGLE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
            }
        }

        // app Login
        if(resultCode == signUpResultCode){
            String userEmail = data.getStringExtra(ELogin.intentResultEmail.getText());
            String userPassword = data.getStringExtra(ELogin.intentResultPassword.getText());
                mBinding.etLoginEmail.setText(userEmail);
                mBinding.etLoginPassword.setText(userPassword);
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            updateUI(user);
                        } else {
                            updateUI(null);
                        }
                    }
                });
    }

    // 액티비티가 죽었을 때
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Session.getCurrentSession().removeCallback(mSessionCallback);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    private void updateUI(FirebaseUser user) {
        if(user!=null) {
            Intent intent = loginPresenter.googleOnSuccess(user);
            startActivity(intent);
        }
    }

    // google button setText
    protected void setGooglePlusButtonText(SignInButton signInButton, String buttonText) {
        // Search all the views inside SignInButton for TextView
        for (int i = 0; i < signInButton.getChildCount(); i++) {
            View v = signInButton.getChildAt(i);
            // if the view is instance of TextView then change the text SignInButton
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                tv.setText(buttonText);
                return;
            }
        }
    }


    @Override
    public void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}