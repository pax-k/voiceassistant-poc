package voiceassistant.poc.mylibrary;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.genai.GenAIException;
import ai.onnxruntime.genai.Generator;
import ai.onnxruntime.genai.GeneratorParams;
import ai.onnxruntime.genai.Model;
import ai.onnxruntime.genai.Sequences;
import ai.onnxruntime.genai.Tokenizer;
import ai.onnxruntime.genai.TokenizerStream;


import ai.onnxruntime.genai.GenAIException;
import ai.onnxruntime.genai.Generator;
import ai.onnxruntime.genai.GeneratorParams;
//import ai.onnxruntime.genai.SessionParams;

import ai.onnxruntime.genai.Sequences;
import ai.onnxruntime.genai.TokenizerStream;
import ai.onnxruntime.genai.Model;
import ai.onnxruntime.genai.Tokenizer;
public class LLMReader {
    private static final String TAG = "LLMReader";

    private Model model;
    private Tokenizer tokenizer;

    private int maxLength = 100;
    private float lengthPenalty = 1.0f;





    private Context context;  // Add context field


    private static boolean fileExists(Context context, String fileName) {
        File file = new File(context.getFilesDir(), fileName);
        return file.exists();
    }




    public LLMReader(Context context) throws ModelInitializationException {
        try {
//            model = new Model(onnxPath);
//            tokenizer = model.createTokenizer();
            context =context;
            downloadModels(context);

        } catch (GenAIException e) {
            Log.e(TAG, "Failed to initialize model", e);
            throw new ModelInitializationException("Failed to initialize LLM model", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during model initialization", e);
            throw new ModelInitializationException("Unexpected error during model initialization", e);
        }
    }

    private void downloadModels(Context context) throws GenAIException {

        final String baseUrl = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/";
//        final String baseUrl ="https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/tree/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4";
        List<String> files = Arrays.asList(
                "added_tokens.json",
                "config.json",
                "configuration_phi3.py",
                "genai_config.json",
                "phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx",
                "phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx.data",
                "special_tokens_map.json",
                "tokenizer.json",
                "tokenizer.model",
                "tokenizer_config.json");

        List<Pair<String, String>> urlFilePairs = new ArrayList<>();
        for (String file : files) {
            if (!fileExists(context, file)) {
                urlFilePairs.add(new Pair<>(
                        baseUrl + file,
                        file));
            }
        }
        if (urlFilePairs.isEmpty()) {
            // Display a message using Toast
//            Toast.makeText(this, "All files already exist. Skipping download.", Toast.LENGTH_SHORT).show();
//            Log.d(TAG, "All files already exist. Skipping download.");
            model = new Model(context.getFilesDir().getPath());
            tokenizer = model.createTokenizer();
            return;
        }

//        progressText.setText("Downloading...");
//        progressText.setVisibility(View.VISIBLE);
//
//        Toast.makeText(this,
//                "Downloading model for the app... Model Size greater than 2GB, please allow a few minutes to download.",
//                Toast.LENGTH_SHORT).show();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            ModelDownloader.downloadModel(context, urlFilePairs, new ModelDownloader.DownloadCallback() {
                @Override
                public void onProgress(long lastBytesRead, long bytesRead, long bytesTotal) {
                    long lastPctDone = 100 * lastBytesRead / bytesTotal;
                    long pctDone = 100 * bytesRead / bytesTotal;
                    if (pctDone > lastPctDone) {
                        Log.d(TAG, "Downloading files: " + pctDone + "%");

                    }
                }
                @Override
                public void onDownloadComplete() {
                    Log.d(TAG, "All downloads completed.");

                    // Last download completed, create SimpleGenAI
                    try {
                        model = new Model(context.getFilesDir().getPath());
                        tokenizer = model.createTokenizer();

                    } catch (GenAIException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }

                }
            });
        });
        executor.shutdown();
    }




    /**
     * Custom exception for model initialization errors
     */
    public static class ModelInitializationException extends Exception {
        public ModelInitializationException(String message, Throwable cause) {
            super(message, cause);
        }

        public ModelInitializationException(String message) {
            super(message);
        }
    }

    public String performInference(String inputText) throws OrtException {
        TokenizerStream stream = null;
        GeneratorParams generatorParams = null;
        Generator generator = null;
        Sequences encodedPrompt = null;
        StringBuilder outputBuilder = new StringBuilder();
        Consumer<String> tokenListener = outputBuilder::append;

        String promptQuestion =inputText;

        String promptQuestion_formatted = "<system>You are a helpful AI assistant. Answer in two paragraphs or less<|end|><|user|>"+promptQuestion+"<|end|>\n<assistant|>";
        Log.i("GenAI: prompt question", promptQuestion_formatted);

        try {
            stream = tokenizer.createStream();

            generatorParams = model.createGeneratorParams();
            //examples for optional parameters to format AI response
            // https://onnxruntime.ai/docs/genai/reference/config.html
            generatorParams.setSearchOption("length_penalty", lengthPenalty);
            generatorParams.setSearchOption("max_length", maxLength);
//                            generatorParams.setModelOption()

            encodedPrompt = tokenizer.encode(promptQuestion_formatted);
            generatorParams.setInput(encodedPrompt);

            generator = new Generator(model, generatorParams);

            // try to measure average time taken to generate each token.
            long startTime = System.currentTimeMillis();
            long firstTokenTime = startTime;
            long currentTime = startTime;
            int numTokens = 0;
            while (!generator.isDone()) {
                generator.computeLogits();
                generator.generateNextToken();

                int token = generator.getLastTokenInSequence(0);

                if (numTokens == 0) { //first token
                    firstTokenTime = System.currentTimeMillis();
                }

                tokenListener.accept(stream.decode(token));

                currentTime = System.currentTimeMillis();
                numTokens++;
            }


            String finalOutput = outputBuilder.toString();

            return finalOutput;

        } catch (GenAIException e) {
            throw new RuntimeException(e);
        } finally {

        }

    }
}
