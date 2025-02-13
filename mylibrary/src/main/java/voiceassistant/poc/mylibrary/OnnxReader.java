package voiceassistant.poc.mylibrary;

import android.content.Context;
import android.content.res.AssetManager;

//import androidx.xr.scenecore.Model;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtException;
import java.io.BufferedInputStream;
import ai.onnxruntime.OnnxTensor;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ai.onnxruntime.genai.GenAIException;
//import ai.onnxruntime.genai.Sequences;
//import ai.onnxruntime.genai.TokenizerStream;
//import ai.onnxruntime.genai.Model;
//import ai.onnxruntime.genai.Tokenizer;

public class OnnxReader {

    private OrtEnvironment env;
    private OrtSession session;
    private final Tokenizer tokenizer;
//    private Model onnxModel;

    public OnnxReader(Context context, String assetFileName, Tokenizer tokenizer) throws IOException, OrtException {
        this.tokenizer = tokenizer;
//        this.tokenizer= new Tokenizer(this);
        byte[] modelBytes = loadModelFile(context, assetFileName);
        env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
        session = env.createSession(modelBytes, opts);
    }

    private byte[] loadModelFile(Context context, String assetFileName) throws IOException {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open(assetFileName);

        // Get the file size
        long fileSize = inputStream.available();
        if (fileSize > Runtime.getRuntime().maxMemory() / 2) {
            throw new IOException("Model file is too large to load into memory");
        }

        // Use BufferedInputStream for better performance
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        byte[] buffer = new byte[8192]; // 8KB buffer
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int bytesRead;
        while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        bufferedInputStream.close();
        inputStream.close();

        return outputStream.toByteArray();
    }


    private String generateResponse(String prompt) throws OrtException {
        // Tokenize the input with a max length of 10 (padding applied if needed)
        Tokenizer.TokenizedOutput output = tokenizer.tokenize(prompt);
        long[] inputIds = output.getInputIds();
        long[] attentionMask = output.getAttentionMask();
        long[] inputShape = new long[]{1, inputIds.length};

        try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), inputShape);
             OnnxTensor attentionTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), inputShape)) {
            
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", inputTensor);
            inputs.put("attention_mask", attentionTensor);
//            inputs.put("past_key_values", OnnxTensor.createTensor(env, new float[0]));  // Empty tensor
//            inputs.put("past_key_values",null);
            int batchSize = 1;
            int numLayers = 30;  // 23 layers for past_key_values
            int numHeads = 3;    // From the model shape info: 3 heads
            int headDim = 64;    // As per the model
            int pastLength = 64; // past_sequence_length

// Provide empty past_key_values tensors for each layer
            for (int i = 0; i < numLayers; i++) {
                long[] shape = {batchSize, numHeads, pastLength, headDim};

                // Create empty tensor (of zeros) for past_key_values
                OnnxTensor emptyTensor = OnnxTensor.createTensor(env, FloatBuffer.allocate(batchSize * numHeads * pastLength * headDim), shape);

                // Add empty past key and value tensors to the input map
                inputs.put("past_key_values." + i + ".key", emptyTensor);
                inputs.put("past_key_values." + i + ".value", emptyTensor);
            }
            OrtSession.Result result = session.run(inputs);

            // Handle float array output
            float[][][] outputArray = (float[][][]) result.get(0).getValue();
            // Convert highest probability tokens to token IDs
            long[] outputTokens = convertOutputToTokens(outputArray);
            return tokenizer.detokenize(outputTokens);
        }
    }

    private long[] convertOutputToTokens(float[][][] output) {
        // Assuming output shape is [batch_size, sequence_length, vocab_size]
        int seqLength = output[0].length;
        long[] tokens = new long[seqLength];
        
        // For each position in the sequence
        for (int i = 0; i < seqLength; i++) {
            float[] logits = output[0][i];
            // Find the token with highest probability
            int maxIndex = 0;
            float maxValue = logits[0];
            
            for (int j = 1; j < logits.length; j++) {
                if (logits[j] > maxValue) {
                    maxValue = logits[j];
                    maxIndex = j;
                }
            }
            tokens[i] = maxIndex;
        }
        return tokens;
    }

    public OrtSession getSession() {
        return session;
    }

    public void close() throws OrtException {
        if (session != null) {
            session.close();
        }
        if (env != null) {
            env.close();
        }
//        if (onnxModel != null) {
//            onnxModel.close();
//        }
    }

    public String performInference(String inputText) throws OrtException {
        // Create input tensor



       return generateResponse(inputText);
//        OnnxTensor inputTensor = createInputTensor(inputText);
//
//        // Run inference
//        Map<String, OnnxTensor> inputs = new HashMap<>();
//        inputs.put("input_ids", inputTensor);  // Replace "input" with your model's actual input name
//
//        try {
//            OrtSession.Result result = session.run(inputs);
//
//            // Process the output tensor
//            OnnxTensor outputTensor = (OnnxTensor) result.get(0);
//            // Convert output tensor to string - adjust based on your model's output format
//            float[] outputData = (float[]) outputTensor.getValue();
//
//            return processOutput(outputData);
//        } finally {
//            inputTensor.close();
//        }
    }
    private OnnxTensor createInputTensor(String inputText) throws OrtException {
        // Convert input text to the format your model expects
        // This is a placeholder - adjust based on your model's input requirements
        long[] inputData = new long[inputText.length()];
        for (int i = 0; i < inputText.length(); i++) {
            inputData[i] = (long) inputText.charAt(i);
        }

        long[] shape = new long[]{1, inputText.length()};  // Adjust shape based on your model
        return OnnxTensor.createTensor(env, LongBuffer.wrap(inputData), shape);
    }

    private String processOutput(float[] outputData) {
        // Process the output data based on your model's output format
        // This is a placeholder - implement based on your specific model
        StringBuilder result = new StringBuilder();
        for (float value : outputData) {
            result.append(value).append(" ");
        }
        return result.toString().trim();
    }

    /**
     * Alternative initializer using ONNX Runtime's Model and Tokenizer classes
     * @param modelPath Path to the ONNX model file
     * @throws IOException If model file cannot be read
     * @throws OrtException If there's an error initializing the ONNX Runtime
     */
//    public static OnnxReader createFromModelPath(String modelPath) throws IOException, OrtException {
//        Model onnxModel = new Model(modelPath);
//        Tokenizer onnxTokenizer = onnxModel.createTokenizer();
//
//        // Create a dummy context since we're not using assets
//        Context dummyContext = null;
//        OnnxReader reader = new OnnxReader(dummyContext, modelPath, onnxTokenizer);
//
//        // Store the model reference for proper cleanup
//        reader.onnxModel = onnxModel;
//
//        return reader;
//    }
}
