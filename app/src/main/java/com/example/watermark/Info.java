package com.example.watermark;
import android.app.DatePickerDialog;
import android.app.DownloadManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.DatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.navigation.NavigationView;
public class Info extends AppCompatActivity  {

    private EditText nameEditText,phoneEditText, addressEditText,emailEditText,pincodeEditText;
    private Button submitButton;
    private RadioButton male;
    private int date=0,month=0,year=0;
    private RadioButton female;
    private RadioButton others;
    private RadioButton skip;
    private RadioButton selectedButton;
    private int SelectedgenderId;
    private String gender="Skip";
    private Button choose;
    private int age=0;
    public DrawerLayout drawerLayout;
    public ActionBarDrawerToggle actionBarDrawerToggle;
    private String timestamp;
    private Button Skip;
    public void savedata(String name,String phone,String email,String address,String pincode,String gender,int age)
    {

        String url="https://script.google.com/macros/s/AKfycbzcBgL82EiQfeYPrFT0ahNM-bgIUWsXT1XUP7fYlkVZIDAmV8TWKfLP1gl65G5T7fAerg/exec?";
        url=url+"action=create&name="+name+"&phone="+phone+"&email="+email+"&address="+address+"&pincode="+pincode+"&gender="+gender+"&age="+age;

        StringRequest stringRequest=new StringRequest(Request.Method.GET,url,new Response.Listener<String>(){
            @Override
            public void onResponse(String response)
            {
                Toast.makeText(Info.this,response,Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError error){
                Toast.makeText(Info.this,error.getMessage(),Toast.LENGTH_SHORT).show();
            }

        });
        RequestQueue queue= Volley.newRequestQueue(this);
        queue.add(stringRequest);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info);
        // drawer layout instance to toggle the menu icon to open
        // drawer and back button to close drawer
        drawerLayout = findViewById(R.id.my_drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);

        // pass the Open and Close toggle for the drawer layout listener
        // to toggle the button
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        // to make the Navigation drawer icon always appear on the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Set the navigation view listener
       // NavigationView navigationView = findViewById(R.id.nav_view);
        //navigationView.setNavigationItemSelectedListener(this);
        nameEditText = findViewById(R.id.name);

        phoneEditText = findViewById(R.id.phone);
        addressEditText = findViewById(R.id.address);
        emailEditText = findViewById(R.id.email);
        pincodeEditText=findViewById(R.id.pincode);
        submitButton = findViewById(R.id.submit);
        Skip=findViewById(R.id.skip);
        choose= findViewById(R.id.choose);
        RadioGroup radioGroup=findViewById(R.id.radio_group);
        RadioButton male=findViewById(R.id.radio1);
        RadioButton female=findViewById(R.id.radio2);
        RadioButton others=findViewById(R.id.radio4);
        RadioButton skip=findViewById(R.id.radio5);

        choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar c = Calendar.getInstance();
                year = c.get(Calendar.YEAR);
                month = c.get(Calendar.MONTH);
                date = c.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(Info.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int yer,
                                          int monthOfYear, int dayOfMonth) {
                        date=dayOfMonth;
                        year=yer;
                        month=monthOfYear;


                    }
                },

                        year, month, date);
                datePickerDialog.show();
            }
        });
        Skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Intent i=new Intent(getApplicationContext(),MainActivity.class);
               i.putExtra("Result","");
               startActivity(i);
            }
        });
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                if(selectedId!=-1) {
                    selectedButton = (RadioButton) findViewById(selectedId);
                    gender = selectedButton.getText().toString();
                }
                final Calendar d = Calendar.getInstance();
                age=d.get(Calendar.YEAR)-year;
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy' - 'HH:mm:ss");
                String ts = sdf.format(new Date());
                savedata(nameEditText.getText().toString(),phoneEditText.getText().toString(),emailEditText.getText().toString(),addressEditText.getText().toString(),pincodeEditText.getText().toString(),gender,age);
              if(gender.equals("Skip")||date==0||nameEditText.getText().toString().equals("")||phoneEditText.getText().toString().equals("")||addressEditText.getText().toString().equals("")||pincodeEditText.getText().equals("")) {
                 Toast.makeText(Info.this,"Please enter all Required Fields !!!",Toast.LENGTH_LONG).show();
              }
              else
              {
                  String Result = nameEditText.getText().toString() + "\n" + "" + age + "\n" + ts + "\n";
                  Intent j = new Intent(getApplicationContext(), MainActivity.class);
                  j.putExtra("Result", Result);
                  startActivity(j);
              }
            }
        });


    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    /*
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
       String BUCKET_NAME = "AnteriCAM Images";
        int id = item.getItemId();

        if (id == R.id.nav_Gallery) {

            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendQueryParameter("bucketName", BUCKET_NAME)
                    .build();

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "image/*");
            intent.putExtra("bucketName", BUCKET_NAME);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(Info.this,"No Images Captured",Toast.LENGTH_LONG).show();
            }

        }
        drawerLayout.closeDrawers();
        return true;
    }

     */

}