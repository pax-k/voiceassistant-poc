package voiceassistant.poc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
        // Disable the submit button while processing
        binding.submitButton.setEnabled(false);
//        binding.progressBar.setVisibility(View.VISIBLE);

        // Create a new thread for inference
        new Thread(() -> {
            try {
                String result = onnxReader.performInference(inputText);
                // Update UI on main thread
                requireActivity().runOnUiThread(() -> {
                    displayModelOutput(result);
                    binding.submitButton.setEnabled(true);
//                    binding.progressBar.setVisibility(View.GONE);
                });
            } catch (OrtException e) {
                // Handle error on main thread
                requireActivity().runOnUiThread(() -> {
                    showError("Inference failed: " + e.getMessage());
                    binding.submitButton.setEnabled(true);
//                    binding.progressBar.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void displayModelOutput(String result) {
        if (!isAdded()) return;  // Check if fragment is still attached

        // Update the output TextView if it exists
        if (binding.outputText != null) {
            binding.outputText.setText(result);
            binding.outputText.setVisibility(View.VISIBLE);
        }

        // Show a material card with the result
        Snackbar snackbar = Snackbar.make(requireView(), "Model Response:", Snackbar.LENGTH_INDEFINITE)
                .setAction("DISMISS", v -> {})
                .setActionTextColor(getResources().getColor(android.R.color.white, null));

        // Customize the snackbar view
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(getResources().getColor(com.google.android.material.R.color.design_default_color_primary, null));
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setMaxLines(5);  // Allow multiple lines
        textView.setText(result);
        
        snackbar.show();
    }

    private void showError(String message) {
        if (isAdded()) {  // Check if fragment is still attached
            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}