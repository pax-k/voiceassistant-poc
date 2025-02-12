package voiceassistant.poc;

import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import ai.onnxruntime.OrtException;
import voiceassistant.poc.databinding.ActivityMainBinding;
import voiceassistant.poc.mylibrary.OnnxReader;


import voiceassistant.poc.mylibrary.Tokenizer;

import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private OnnxReader onnxReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        initializeOnnxModel();
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);



        // Handle submit button click
//        binding.submit_button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                String inputText = binding.editText.getText().toString();
//                if (!inputText.isEmpty() && onnxReader != null) {
//                    try {
//                        String result = onnxReader.performInference(inputText);
//                        Snackbar.make(view, "Model output: " + result, Snackbar.LENGTH_LONG)
//                                .show();
//                    } catch (OrtException e) {
//                        Snackbar.make(view, "Inference failed: " + e.getMessage(),
//                                    Snackbar.LENGTH_LONG)
//                                .show();
//                    }
//                }
//            }
//        });
    }

    private void initializeOnnxModel() {
        try {
            Tokenizer tokenizer = new Tokenizer(this);
            onnxReader = new OnnxReader(this, "model_q4.onnx", tokenizer);
        } catch (Exception e) {
            Snackbar.make(binding.getRoot(), "Failed to initialize model: " + e.getMessage(),
                    Snackbar.LENGTH_LONG).show();
        }
    }

    public OnnxReader getOnnxReader() {
        if (onnxReader == null) {
            initializeOnnxModel();
        }
        return onnxReader;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (onnxReader != null) {
            try {
                onnxReader.close();
            } catch (OrtException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}