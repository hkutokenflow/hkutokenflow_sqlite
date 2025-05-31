package com.example.workshop1.Login;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.workshop1.R;
import com.example.workshop1.SQLite.Mysqliteopenhelper;
import com.example.workshop1.SQLite.User;
import com.example.workshop1.Utils.PasswordEncryption;

import java.util.Random;

public class RegisterActivity extends AppCompatActivity {


    private EditText et_email,et_pwd,et_equal, et_verifyCode;
    private ImageView iv_eye2,iv_eye3;
    private CheckBox cb_accept;
    private Mysqliteopenhelper mysqliteopenhelper;
    private int Visiable1=0,Visiable2=0;
    private String sentCode = "nocode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        et_email=findViewById(R.id.et_register_name);
        et_pwd=findViewById(R.id.et_register_password);
        et_equal=findViewById(R.id.et_equal_password);
        et_verifyCode = findViewById(R.id.et_verify_code);  // Verification code input

        iv_eye2=findViewById(R.id.iv_eye2);
        iv_eye3=findViewById(R.id.iv_eye3);

        cb_accept=(CheckBox)findViewById(R.id.accept_policy);

        //----------------------------SQL----------------------
        mysqliteopenhelper=new Mysqliteopenhelper(this);
        Button btn_sendCode = findViewById(R.id.btn_sendCode);
        btn_sendCode.setOnClickListener(v -> sendEmailCode());


    }



    //------------------------------------sendEmailCode------------------------------
    private void sendEmailCode() {
        String email = et_email.getText().toString().trim();
        if (!isHKUEmail(email)) {
            Toast.makeText(this, "Must use HKU email", Toast.LENGTH_SHORT).show();
            return;
        }

        sentCode = generateCode();

        new Thread(() -> {
            try {
                EmailSender.sendEmail(email, "Your Verification Code", "Your code is: " + sentCode);
                runOnUiThread(() ->
                        Toast.makeText(this, "Code sent!", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Failed to send email", Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
    }

    private String generateCode() {
        Random rand = new Random();
        int code = 100000 + rand.nextInt(900000); // 6位数
        return String.valueOf(code);
    }


    private boolean verifyCode() {
        String inputCode = et_verifyCode.getText().toString().trim();
        if (inputCode.equals(sentCode)) {
            return true;
            // 可以跳转回注册页面，或者设置一个"认证通过"的标志
        }
        return false;
    }

    //--------------------------------------register--------------------------------------

    public void register_newuser(View view) {
        String name = et_email.getText().toString();
        String pwd = et_pwd.getText().toString();
        String equal = et_equal.getText().toString();

        // 检查邮箱格式是否为学校邮箱
        if (!isHKUEmail(name)) {
            Toast.makeText(this, "Email must be your HKU email", Toast.LENGTH_SHORT).show();
            return;
        }

        //查看验证码是否正确
        if(!verifyCode()){
            Toast.makeText(this, "Email code incorrect", Toast.LENGTH_SHORT).show();
            return;
        }

        // 先检查密码格式是否合规
        if (!isPasswordValid(pwd)) {
            return;
        }

        if (!pwd.equals(equal)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!cb_accept.isChecked()) {
            Toast.makeText(this, "Please agree to the HKU Privacy Policy", Toast.LENGTH_SHORT).show();
            return;
        }

        // 加密密码
        String encryptedPassword = PasswordEncryption.encrypt(pwd);
        if (encryptedPassword == null) {
            Toast.makeText(this, "Password encryption failed", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = new User(name, encryptedPassword, "", "student");
        long res = mysqliteopenhelper.addUser(user);
        if (res != -1) {
            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
        }
    }



    //---------------------------------isHKUEmail---------------------------------
    private boolean isHKUEmail(String email) {
        return email.endsWith("@connect.hku.hk") || email.endsWith("@hku.hk");
    }

    // 至少6位，必须同时包含大小写字母，数字和特殊符号
    private boolean isPasswordValid(String password) {
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!password.matches(".*[a-z].*")) {
            Toast.makeText(this, "Password must include at least one lowercase letter", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!password.matches(".*[A-Z].*")) {
            Toast.makeText(this, "Password must include at least one uppercase letter", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!password.matches(".*\\d.*")) {
            Toast.makeText(this, "Password must include at least one number", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            Toast.makeText(this, "Password must include at least one special character", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true; // 全部符合要求
    }


    //监听密码是否可见
    public void Isvisiable2(View view){
        if(Visiable1==0){
            et_pwd.setTransformationMethod(HideReturnsTransformationMethod.getInstance());//可见
            iv_eye2.setImageResource(R.drawable.baseline_visibility_24);
            Visiable1=1;
        }
        else{
            et_pwd.setTransformationMethod(PasswordTransformationMethod.getInstance());//不可见
            iv_eye2.setImageResource(R.drawable.baseline_visibility_off_24);
            Visiable1=0;
        }
    }

    //监听密码是否可见
    public void Isvisiable3(View view){
        if(Visiable2==0){
            et_equal.setTransformationMethod(HideReturnsTransformationMethod.getInstance());//可见
            iv_eye3.setImageResource(R.drawable.baseline_visibility_24);
            Visiable2=1;
        }
        else{
            et_equal.setTransformationMethod(PasswordTransformationMethod.getInstance());//不可见
            iv_eye3.setImageResource(R.drawable.baseline_visibility_off_24);
            Visiable2=0;
        }
    }

}