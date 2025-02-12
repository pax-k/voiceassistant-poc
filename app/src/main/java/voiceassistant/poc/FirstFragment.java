package voiceassistant.poc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;

import ai.onnxruntime.OrtException;
import voiceassistant.poc.databinding.FragmentFirstBinding;
import voiceassistant.poc.mylibrary.OnnxReader;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private OnnxReader onnxReader;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the OnnxReader instance from MainActivity
        MainActivity activity = (MainActivity) requireActivity();
        onnxReader = activity.getOnnxReader();

        binding.submitButton.setOnClickListener(this::handleSubmitClick);

        binding.buttonFirst.setOnClickListener(v ->
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment)
        );
    }

    private void handleSubmitClick(View view) {
        String inputText = binding.editText.getText().toString();
        if (inputText.isEmpty()) {
            showError("Please enter some text");
            return;
        }

        if (onnxReader == null) {
            showError("Model not initialized");
            return;
        }

        performInference(inputText);
    }

    private void performInference(String inputText) {
        try {
            String result = onnxReader.performInference(inputText);
            showSuccess("Model output: " + result);
        } catch (OrtException e) {
            showError("Inference failed: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
    }

    private void showSuccess(String message) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}