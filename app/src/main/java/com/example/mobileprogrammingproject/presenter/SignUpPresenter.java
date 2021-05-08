package com.example.mobileprogrammingproject.presenter;

import android.util.Log;
import android.widget.Toast;

import com.example.mobileprogrammingproject.AppDatabase;
import com.example.mobileprogrammingproject.constants.Constants.ESignUp;
import com.example.mobileprogrammingproject.databinding.ActivitySignUpBinding;
import com.example.mobileprogrammingproject.model.User;

import java.util.List;
import java.util.regex.Pattern;

public class SignUpPresenter implements SignUpContract.Presenter{

    private SignUpContract.View signUpView;
    private ActivitySignUpBinding mBinding;
    private AppDatabase mAppDatabase;

    public SignUpPresenter(SignUpContract.View signUpView, ActivitySignUpBinding mBinding, AppDatabase mAppDatabase){
        this.signUpView = signUpView;
        this.mBinding = mBinding;
        this.mAppDatabase = mAppDatabase;
    }

    public boolean emailDuplicateCheck(Boolean buttonIsClicked){
        boolean emailDuplicateResult = false;
        if(validEmailCheck()==false){
            return false;
        } else if(validEmailCheck()==true){
            String inputEmail = mBinding.etEmailSignUp.getText().toString();
            List<String> allEmail = mAppDatabase.userDao().findAllEmail();
            for (String email : allEmail) {
                if (email.equals(inputEmail)) {
                    if (mAppDatabase.userDao().findTypeByEmail(email).equals("app")) {
                        emailDuplicateResult = true;
                        signUpView.showToast(ESignUp.emailDuplicateMessage.getText());
                    }
                }
            }
            if (emailDuplicateResult == false) {
                if(buttonIsClicked == true) {
                    signUpView.showToast(ESignUp.emailNonDuplicateMessage.getText());
                } else{
                    return emailDuplicateResult;
                }
            }
        }
        return emailDuplicateResult;
    }

    public boolean signUpCheck() {

        if(emailDuplicateCheck(false)==true){
            return false;
        } else if(validPasswordCheck()==false){
            return false;
        } else if(validNameCheck()==false){
            return false;
        } else if(phoneNumberCheck()==false){
            return false;
        } else if(genderCheck()==false){
            return false;
        } else if(agreeCheck()==false){
            return false;
        } else{
            User user = createUser();
            Log.e("유저", user.toString());
            mAppDatabase.userDao().insert(user);
            signUpView.showToast(ESignUp.completeSignUpMessage.getText());
        }
      return true;
    }

    private User createUser() {
        String password = mBinding.etPwSignUp.getText().toString();
        String email = mBinding.etEmailSignUp.getText().toString();
        String name = mBinding.etNameSignUp.getText().toString();
        String profileImg = null;
        String dateOfBirth = Integer.toString(mBinding.dpDateOfBirth.getYear())+"년"+Integer.toString(mBinding.dpDateOfBirth.getMonth()+1)+"월"
                +Integer.toString(mBinding.dpDateOfBirth.getDayOfMonth())+"일";
        String phoneNumber = mBinding.etPhoneNumber.getText().toString();
        String genderChoice = null;
        if(mBinding.btnMale.isChecked()){
            genderChoice = mBinding.btnMale.getText().toString();
        } else if(mBinding.btnFemale.isChecked()){
            genderChoice = mBinding.btnFemale.getText().toString();
        }
        String emailType = "app";
        User user = new User(password, email, name, profileImg, dateOfBirth, phoneNumber, genderChoice, emailType);
        return user;
    }

    public boolean validEmailCheck(){
        String inputEmail = mBinding.etEmailSignUp.getText().toString();
        Boolean emailFormatResult = Pattern.matches(ESignUp.emailFormat.getText(), inputEmail);
        if(emailFormatResult==false){
            signUpView.showToast(ESignUp.emailFormatErrorMessage.getText());
            return false;
        }
        if(inputEmail.length()<10 || inputEmail.length()>25){
            signUpView.showToast(ESignUp.emailLetterCountError.getText());
            return false;
        }

        return true;
    }

    public boolean validPasswordCheck(){
        String inputPW = mBinding.etPwSignUp.getText().toString();
        String inputCheckPW = mBinding.etPwSignUp2.getText().toString();
        Boolean inputPWEnglishNumberResult = Pattern.matches(ESignUp.pwEnglishNumberFormat.getText(), inputPW);
        Boolean inputCheckPWEnglishNumberResult = Pattern.matches(ESignUp.pwEnglishNumberFormat.getText(), inputCheckPW);
        if(inputPW.length()==0 || inputCheckPW.length()==0){
            signUpView.showToast(ESignUp.pwInputMessage.getText());
            return false;
        }
        if(inputPWEnglishNumberResult==false || inputCheckPWEnglishNumberResult==false){
            signUpView.showToast(ESignUp.pwWarningMessage.getText());
            return false;
        }
        if(!inputPW.equals(inputCheckPW)){
            signUpView.showToast(ESignUp.pwNotMatchMessage.getText());
            return false;
        }
        return true;
    }

    public boolean validNameCheck() {
        String inputName = mBinding.etNameSignUp.getText().toString();
        if(inputName.length()==0){
            signUpView.showToast(ESignUp.nameInputMessage.getText());
            return false;
        }
        return true;
    }

    public boolean phoneNumberCheck() {
        String inputPhoneNumber = mBinding.etPhoneNumber.getText().toString();
        Boolean inputPhoneNumberResult = Pattern.matches(ESignUp.phoneNumberFormat.getText(), inputPhoneNumber);
        if(inputPhoneNumber.length()==0){
            signUpView.showToast(ESignUp.phoneNumberInputMessage.getText());
            return false;
        }
        if(inputPhoneNumberResult==false){
            signUpView.showToast(ESignUp.phoneNumberErrorMessage.getText());
            return false;
        }
        return true;
    }


    public boolean genderCheck() {
        if(((!mBinding.btnMale.isChecked())&&(!mBinding.btnFemale.isChecked()))){
            signUpView.showToast(ESignUp.genderCheckMessage.getText());
            return false;
        }
        return true;
    }

    public boolean agreeCheck(){
        if((!mBinding.cbPrivacyAgreement.isChecked())||(!mBinding.cbTermsOfServiceAgreement.isChecked())){
            signUpView.showToast(ESignUp.agreeCheckMessage.getText());
            return false;
        }
        return true;
    }


}
